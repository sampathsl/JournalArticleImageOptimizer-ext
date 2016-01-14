package com.liferay.portal.journal.action.png;

import com.idrsolutions.image.BitWriter;
import com.idrsolutions.image.png.D3;
import com.idrsolutions.image.png.D4;
import com.idrsolutions.image.png.PngBitReader;
import com.idrsolutions.image.png.PngChunk;
import com.idrsolutions.image.png.Quant24;
import com.idrsolutions.image.png.Quant32;

import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.Deflater;

public class PngEncoderExtend {

	private boolean compress;

	public OutputStream write(BufferedImage bufferedimage, OutputStream outputstream) throws IOException {
		if (compress)
			return compress8Bit(bufferedimage, outputstream);
		else
			return compressNormal(bufferedimage, outputstream);
	}

	public boolean isCompressed() {
		return compress;
	}

	public void setCompressed(boolean flag) {
		compress = flag;
	}

	private OutputStream compressNormal(BufferedImage bufferedimage, OutputStream outputstream) throws IOException {
		int i = bufferedimage.getHeight();
		int j = bufferedimage.getWidth();
		ColorModel colormodel = bufferedimage.getColorModel();
		boolean flag = colormodel.hasAlpha();
		int k = colormodel.getPixelSize();
		int l = colormodel.getNumComponents();
		boolean flag1 = colormodel instanceof IndexColorModel;
		int i1 = calculateBitDepth(k, l);
		int j1;
		if (flag1) {
			j1 = 3;
			l = 1;
		} else if (l < 3)
			j1 = flag ? 4 : 0;
		else if (i1 < 8)
			j1 = flag ? 4 : 0;
		else
			j1 = flag ? 6 : 2;
		outputstream.write(PngChunk.SIGNATURE);
		PngChunk pngchunk = PngChunk.createHeaderChunk(j, i, (byte) i1, (byte) j1, (byte) 0, (byte) 0, (byte) 0);
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		byte abyte0[];
		if (flag1 && i1 != 8)
			abyte0 = getIndexedPaletteData(bufferedimage);
		else
			abyte0 = getPixelData(bufferedimage, i1, l, j, i);
		if (flag1) {
			IndexColorModel indexcolormodel = (IndexColorModel) colormodel;
			int k1 = indexcolormodel.getMapSize();
			int ai[] = new int[k1];
			indexcolormodel.getRGBs(ai);
			if (i1 == 8)
				k1 = reduceIndexMap(k1, ai, abyte0);
			ByteBuffer bytebuffer = ByteBuffer.allocate(k1 * 3);
			for (int l1 = 0; l1 < k1; l1++) {
				int i2 = ai[l1];
				bytebuffer.put(new byte[] { (byte) (i2 >> 16), (byte) (i2 >> 8), (byte) i2 });
			}

			pngchunk = PngChunk.createPaleteChunk(bytebuffer.array());
			outputstream.write(pngchunk.getLength());
			outputstream.write(pngchunk.getName());
			outputstream.write(pngchunk.getData());
			outputstream.write(pngchunk.getCRCValue());
			if (indexcolormodel.getNumComponents() == 4) {
				byte abyte1[] = new byte[k1];
				for (int j2 = 0; j2 < k1; j2++)
					abyte1[j2] = (byte) (ai[j2] >> 24);

				pngchunk = PngChunk.createTrnsChunk(abyte1);
				outputstream.write(pngchunk.getLength());
				outputstream.write(pngchunk.getName());
				outputstream.write(pngchunk.getData());
				outputstream.write(pngchunk.getCRCValue());
			}
		}
		abyte0 = getDeflatedData(abyte0);
		pngchunk = PngChunk.createDataChunk(abyte0);
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		pngchunk = PngChunk.createEndChunk();
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		return outputstream;
	}

	private static int reduceIndexMap(int i, int ai[], byte abyte0[]) {
		int j = 0;
		byte abyte1[] = new byte[i];
		LinkedHashMap linkedhashmap = new LinkedHashMap();
		for (int k = 0; k < i; k++) {
			int i1 = ai[k];
			if (!linkedhashmap.containsKey(Integer.valueOf(i1))) {
				abyte1[k] = (byte) j;
				linkedhashmap.put(Integer.valueOf(i1), Integer.valueOf(j));
				j++;
			} else {
				abyte1[k] = (byte) ((Integer) linkedhashmap.get(Integer.valueOf(i1))).intValue();
			}
		}

		if (j < i) {
			for (int l = 0; l < abyte0.length; l++)
				abyte0[l] = abyte1[abyte0[l] & 255];

			Set set = linkedhashmap.keySet();
			int j1 = 0;
			for (Iterator iterator = set.iterator(); iterator.hasNext();) {
				int k1 = ((Integer) iterator.next()).intValue();
				ai[j1++] = k1;
			}

		}
		return j;
	}

	private static boolean isAlphaUsed(byte abyte0[]) {
		byte abyte1[] = abyte0;
		int i = abyte1.length;
		for (int j = 0; j < i; j++) {
			byte byte0 = abyte1[j];
			if (byte0 != -1)
				return true;
		}

		return false;
	}

	private OutputStream compress8Bit(BufferedImage bufferedimage, OutputStream outputstream) throws IOException {
		int i = bufferedimage.getType();
		int j = bufferedimage.getHeight();
		int k = bufferedimage.getWidth();
		int l = j * k;
		byte abyte3[] = null;
		int ai3[][] = (int[][]) null;
		int ai4[][] = (int[][]) null;
		int l3 = 0;
		label0: switch (i) {
		case 5: // '\005'
			byte abyte0[] = ((DataBufferByte) bufferedimage.getRaster().getDataBuffer()).getData();
			ai4 = new int[j][k];
			for (int i4 = 0; i4 < j; i4++) {
				int ai5[] = ai4[i4];
				for (int j5 = 0; j5 < k; j5++) {
					int i3 = abyte0[l3++] & 255;
					int j2 = abyte0[l3++] & 255;
					int k1 = abyte0[l3++] & 255;
					ai5[j5] = k1 << 16 | j2 << 8 | i3;
				}

			}

			break;

		case 6: // '\006'
			byte abyte1[] = ((DataBufferByte) bufferedimage.getRaster().getDataBuffer()).getData();
			ai3 = new int[j][k];
			int j4 = 0;
			do {
				if (j4 >= j)
					break label0;
				int ai6[] = ai3[j4];
				for (int k5 = 0; k5 < k; k5++) {
					int j1 = abyte1[l3++] & 255;
					int j3 = abyte1[l3++] & 255;
					int k2 = abyte1[l3++] & 255;
					int l1 = abyte1[l3++] & 255;
					ai6[k5] = j1 << 24 | l1 << 16 | k2 << 8 | j3;
				}

				j4++;
			} while (true);

		case 4: // '\004'
			int ai[] = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
			ai4 = new int[j][k];
			int k4 = 0;
			do {
				if (k4 >= j)
					break label0;
				int ai7[] = ai4[k4];
				for (int l5 = 0; l5 < k; l5++) {
					int i1 = ai[l3++];
					int k3 = i1 >> 16 & 255;
					int l2 = i1 >> 8 & 255;
					int i2 = i1 & 255;
					ai7[l5] = i2 << 16 | l2 << 8 | k3;
				}

				k4++;
			} while (true);

		case 2: // '\002'
			int ai1[] = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
			ai3 = new int[j][k];
			int l4 = 0;
			do {
				if (l4 >= j)
					break label0;
				int ai8[] = ai3[l4];
				for (int i6 = 0; i6 < k; i6++)
					ai8[i6] = ai1[l3++];

				l4++;
			} while (true);

		case 1: // '\001'
			int ai2[] = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
			ai4 = new int[j][k];
			int i5 = 0;
			do {
				if (i5 >= j)
					break label0;
				int ai9[] = ai4[i5];
				for (int j6 = 0; j6 < k; j6++)
					ai9[j6] = ai2[l3++];

				i5++;
			} while (true);

		case 3: // '\003'
		default:
			return compressNormal(bufferedimage, outputstream);
		}
		byte abyte5[] = new byte[l + j];
		byte abyte4[];
		if (ai3 != null) {
			Object aobj[] = getIndexedMap(ai3);
			byte abyte6[];
			if (aobj != null) {
				abyte6 = (byte[]) (byte[]) aobj[0];
				abyte4 = (byte[]) (byte[]) aobj[1];
				abyte3 = (byte[]) (byte[]) aobj[2];
				if (!isAlphaUsed(abyte3))
					abyte3 = null;
			} else {
				Quant32 quant32 = new Quant32();
				Object aobj2[] = quant32.getPalette(ai3);
				abyte4 = (byte[]) (byte[]) aobj2[0];
				abyte3 = (byte[]) (byte[]) aobj2[1];
				abyte6 = D4.process(abyte4, abyte3, ai3, j, k);
				if (!isAlphaUsed(abyte3))
					abyte3 = null;
			}
			int i7 = 0;
			int k7 = 0;
			for (int i8 = 0; i8 < j; i8++) {
				abyte5[k7++] = 0;
				for (int k8 = 0; k8 < k; k8++)
					abyte5[k7++] = abyte6[i7++];

			}

		} else {
			Object aobj1[] = getIndexedMap(ai4);
			byte abyte7[];
			if (aobj1 != null) {
				abyte7 = (byte[]) (byte[]) aobj1[0];
				abyte4 = (byte[]) (byte[]) aobj1[1];
			} else {
				Quant24 quant24 = new Quant24();
				abyte4 = quant24.getPalette(ai4);
				abyte7 = D3.process(abyte4, ai4, j, k);
			}
			int j7 = 0;
			int l7 = 0;
			for (int j8 = 0; j8 < j; j8++) {
				abyte5[l7++] = 0;
				for (int l8 = 0; l8 < k; l8++)
					abyte5[l7++] = abyte7[j7++];

			}

		}
		int k6 = 8;
		int l6 = 3;
		outputstream.write(PngChunk.SIGNATURE);
		PngChunk pngchunk = PngChunk.createHeaderChunk(k, j, (byte) k6, (byte) l6, (byte) 0, (byte) 0, (byte) 0);
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		byte abyte2[] = getDeflatedData(abyte5);
		pngchunk = PngChunk.createPaleteChunk(abyte4);
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		if (abyte3 != null) {
			pngchunk = PngChunk.createTrnsChunk(abyte3);
			outputstream.write(pngchunk.getLength());
			outputstream.write(pngchunk.getName());
			outputstream.write(pngchunk.getData());
			outputstream.write(pngchunk.getCRCValue());
		}
		pngchunk = PngChunk.createDataChunk(abyte2);
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		pngchunk = PngChunk.createEndChunk();
		outputstream.write(pngchunk.getLength());
		outputstream.write(pngchunk.getName());
		outputstream.write(pngchunk.getData());
		outputstream.write(pngchunk.getCRCValue());
		
		return outputstream;
		
	}

	private static Object[] getIndexedMap(int ai[][]) {
		int i = ai.length;
		int j = ai[0].length;
		int ai2[] = new int[256];
		int k = 0;
		int l = 0;
		int i1 = 0;
		byte abyte0[] = new byte[i * j];
		HashMap hashmap = new HashMap();
		for (int j1 = 0; j1 < i; j1++) {
			int ai1[] = ai[j1];
			for (int k1 = 0; k1 < j; k1++) {
				int l1 = ai1[k1];
				Integer integer = (Integer) hashmap.get(Integer.valueOf(l1));
				if (integer == null) {
					if (k > 255)
						return null;
					hashmap.put(Integer.valueOf(l1), Integer.valueOf(k));
					ai2[k] = l1;
					abyte0[l++] = (byte) k;
					k++;
				} else {
					abyte0[l++] = (byte) integer.intValue();
				}
			}

		}

		byte abyte1[] = new byte[k * 3];
		byte abyte2[] = new byte[k];
		l = 0;
		for (int i2 = 0; i2 < k; i2++) {
			int j2 = ai2[i2];
			abyte2[i1++] = (byte) (j2 >> 24 & 255);
			abyte1[l++] = (byte) (j2 >> 16 & 255);
			abyte1[l++] = (byte) (j2 >> 8 & 255);
			abyte1[l++] = (byte) (j2 & 255);
		}

		return (new Object[] { abyte0, abyte1, abyte2 });
	}

	private static byte[] getIndexedPaletteData(BufferedImage bufferedimage) throws IOException {
		byte abyte0[] = ((DataBufferByte) bufferedimage.getRaster().getDataBuffer()).getData();
		int i = bufferedimage.getHeight();
		int j = abyte0.length / i;
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		int k = 0;
		for (int l = 0; l < i; l++) {
			bytearrayoutputstream.write(0);
			byte abyte1[] = new byte[j];
			System.arraycopy(abyte0, k, abyte1, 0, j);
			bytearrayoutputstream.write(abyte1);
			k += j;
		}

		bytearrayoutputstream.close();
		return bytearrayoutputstream.toByteArray();
	}

	private static byte[] getPixelData(BufferedImage bufferedimage, int i, int j, int k, int l) throws IOException {
		ColorModel colormodel = bufferedimage.getColorModel();
		switch (i) {
		case 1: // '\001'
		case 2: // '\002'
		case 4: // '\004'
			byte abyte0[] = ((DataBufferByte) bufferedimage.getRaster().getDataBuffer()).getData();
			byte byte0 = i != 1 ? ((byte) (i != 2 ? 2 : 4)) : 8;
			PngBitReader pngbitreader = new PngBitReader(abyte0, true);
			ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
			BitWriter bitwriter = new BitWriter(bytearrayoutputstream);
			int j1 = 0;
			int k1 = abyte0.length * byte0;
			for (int l1 = 0; l1 < k1; l1++) {
				if (j1 == 0)
					bitwriter.writeByte((byte) 0);
				bitwriter.writeBits(pngbitreader.getPositive(i), i);
				if (++j1 == k)
					j1 = 0;
			}

			bitwriter.end();
			bytearrayoutputstream.flush();
			bytearrayoutputstream.close();
			return bytearrayoutputstream.toByteArray();

		case 8: // '\b'
			DataBuffer databuffer = bufferedimage.getRaster().getDataBuffer();
			switch (databuffer.getDataType()) {
			case 0: // '\0'
				byte abyte1[] = ((DataBufferByte) bufferedimage.getRaster().getDataBuffer()).getData();
				int i1 = abyte1.length;
				int i2 = 0;
				ByteBuffer bytebuffer = ByteBuffer.allocate(k * l * j + l);
				int l2;
				switch (bufferedimage.getType()) {
				case 5: // '\005'
					for (int j2 = 0; j2 < i1; j2 += j) {
						if (i2 == 0)
							bytebuffer.put((byte) 0);
						byte abyte2[] = { abyte1[j2 + 2], abyte1[j2 + 1], abyte1[j2] };
						bytebuffer.put(abyte2);
						if (++i2 == k)
							i2 = 0;
					}

					return bytebuffer.array();

				case 6: // '\006'
				case 7: // '\007'
					for (int k2 = 0; k2 < i1; k2 += j) {
						if (i2 == 0)
							bytebuffer.put((byte) 0);
						byte abyte3[] = { abyte1[k2 + 3], abyte1[k2 + 2], abyte1[k2 + 1], abyte1[k2] };
						bytebuffer.put(abyte3);
						if (++i2 == k)
							i2 = 0;
					}

					return bytebuffer.array();

				default:
					l2 = 0;
					break;
				}
				for (; l2 < i1; l2 += j) {
					if (i2 == 0)
						bytebuffer.put((byte) 0);
					for (int j3 = 0; j3 < j; j3++)
						bytebuffer.put(abyte1[l2 + j3]);

					if (++i2 == k)
						i2 = 0;
				}

				return bytebuffer.array();

			case 3: // '\003'
				int ai[] = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
				int l3 = 0;
				int j4 = 0;
				byte abyte4[];
				if (bufferedimage.getType() == 2 || bufferedimage.getType() == 3) {
					abyte4 = new byte[k * l * 4 + l];
					for (int k5 = 0; k5 < l; k5++) {
						abyte4[l3++] = 0;
						for (int j6 = 0; j6 < k; j6++) {
							int k4 = ai[j4++];
							abyte4[l3++] = (byte) (k4 >> 16);
							abyte4[l3++] = (byte) (k4 >> 8);
							abyte4[l3++] = (byte) k4;
							abyte4[l3++] = (byte) (k4 >> 24);
						}

					}

				} else if (bufferedimage.getType() == 1) {
					abyte4 = new byte[k * l * 3 + l];
					for (int l5 = 0; l5 < l; l5++) {
						abyte4[l3++] = 0;
						for (int k6 = 0; k6 < k; k6++) {
							int l4 = ai[j4++];
							abyte4[l3++] = (byte) (l4 >> 16);
							abyte4[l3++] = (byte) (l4 >> 8);
							abyte4[l3++] = (byte) l4;
						}

					}

				} else if (bufferedimage.getType() == 4) {
					abyte4 = new byte[k * l * 3 + l];
					for (int i6 = 0; i6 < l; i6++) {
						abyte4[l3++] = 0;
						for (int l6 = 0; l6 < k; l6++) {
							int i5 = ai[j4++];
							abyte4[l3++] = (byte) i5;
							abyte4[l3++] = (byte) (i5 >> 8);
							abyte4[l3++] = (byte) (i5 >> 16);
						}

					}

				} else if (colormodel instanceof DirectColorModel) {
					DirectColorModel directcolormodel = (DirectColorModel) colormodel;
					long l7 = getMaskValue(directcolormodel.getRedMask());
					long l8 = getMaskValue(directcolormodel.getGreenMask());
					long l9 = getMaskValue(directcolormodel.getBlueMask());
					long l10 = getMaskValue(directcolormodel.getAlphaMask());
					abyte4 = new byte[k * l * 4 + l];
					for (int j8 = 0; j8 < l; j8++) {
						abyte4[l3++] = 0;
						for (int k8 = 0; k8 < k; k8++) {
							int j5 = ai[j4++];
							abyte4[l3++] = (byte) (j5 >> (int) l7);
							abyte4[l3++] = (byte) (j5 >> (int) l8);
							abyte4[l3++] = (byte) (j5 >> (int) l9);
							abyte4[l3++] = (byte) (j5 >> (int) l10);
						}

					}

				} else {
					ByteBuffer bytebuffer2 = ByteBuffer.allocate(k * l * j + l);
					int i7 = 0;
					int ai1[] = ai;
					int j7 = ai1.length;
					for (int k7 = 0; k7 < j7; k7++) {
						int i8 = ai1[k7];
						if (i7 == 0)
							bytebuffer2.put((byte) 0);
						byte abyte5[] = PngChunk.intToBytes(i8);
						switch (j) {
						case 4: // '\004'
							bytebuffer2.put(new byte[] { abyte5[1], abyte5[2], abyte5[3], abyte5[0] });
							break;

						case 3: // '\003'
							bytebuffer2.put(new byte[] { abyte5[1], abyte5[2], abyte5[3] });
							break;

						case 2: // '\002'
							bytebuffer2.put(new byte[] { abyte5[2], abyte5[3] });
							break;

						case 1: // '\001'
							bytebuffer2.put(abyte5[3]);
							break;
						}
						if (++i7 == k)
							i7 = 0;
					}

					return bytebuffer2.array();
				}
				return abyte4;
			}
			// fall through

		case 16: // '\020'
			short aword0[] = ((DataBufferUShort) bufferedimage.getRaster().getDataBuffer()).getData();
			ByteBuffer bytebuffer1 = ByteBuffer.allocate(aword0.length * 2 + l);
			int i3 = 0;
			for (int k3 = 0; k3 < aword0.length; k3 += j) {
				if (i3 == 0)
					bytebuffer1.put((byte) 0);
				for (int i4 = 0; i4 < j; i4++)
					bytebuffer1.putShort(aword0[k3 + i4]);

				if (++i3 == k)
					i3 = 0;
			}

			return bytebuffer1.array();

		default:
			return null;
		}
	}

	private static int getMaskValue(int i) {
		switch (i) {
		case 255:
			return 0;

		case 65280:
			return 8;

		case 16711680:
			return 16;
		}
		return 24;
	}

	private static int calculateBitDepth(int i, int j) {
		if (i < 8)
			return i;
		int k = i / j;
		if (k == 8 || k == 16)
			return k;
		else
			return 8;
	}

	private byte[] getDeflatedData(byte abyte0[]) throws IOException {
		Deflater deflater;
		if (compress)
			deflater = new Deflater(9);
		else
			deflater = new Deflater(1);
		deflater.setInput(abyte0);
		int i = Math.min(abyte0.length / 2, 4096);
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(i);
		deflater.finish();
		byte abyte1[] = new byte[i];
		int j;
		for (; !deflater.finished(); bytearrayoutputstream.write(abyte1, 0, j))
			j = deflater.deflate(abyte1);

		deflater.end();
		bytearrayoutputstream.close();
		return bytearrayoutputstream.toByteArray();
	}
}
