# JournalArticleImageOptimizer-ext
	Liferay Ext plugin for image compressor while uploading the Liferay web page image data

## Installation
	Put the jdeli.jar in to the liferay-portal-6.2-ce-ga5\tomcat-7.0.62\lib\ext folder
	Put com.liferay.image.jpg.compress.level = 0.9 in the portal-ext.properties file
	Build Ext plugin 
	Deploy
	Restart the server
	
## Uninstall
	First stop the server
	
	* Remove the JournalArticleImageOptimizer-ext folder in liferay-portal-6.2-ce-ga5\tomcat-7.0.62\webapps folder
	* Remove following files in liferay-portal-6.2-ce-ga5\tomcat-7.0.62\webapps\ROOT\WEB-INF\lib folder
		ext-JournalArticleImageOptimizer-ext-impl.jar
		ext-JournalArticleImageOptimizer-ext-util-bridges.jar
		ext-JournalArticleImageOptimizer-ext-util-java.jar
		ext-JournalArticleImageOptimizer-ext-util-taglib.jar
	* Remove following files in liferay-portal-6.2-ce-ga5\tomcat-7.0.62\webapps\JournalArticleImageOptimizer-ext\WEB-INF folder
		ext-JournalArticleImageOptimizer-ext.xml
		
	Start the server
	
## License
	Only use for non-commercial purpose.
	This software is licensed under the MIT License.