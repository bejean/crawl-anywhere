package fr.eolya.crawlerws;


import com.codahale.metrics.annotation.Timed;

import fr.eolya.extraction.tika.TikaWrapper;
import fr.eolya.utils.Base64;
import fr.eolya.utils.http.HttpLoader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


@Path("/testcleaning")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlerWsResourcesTestCleaning extends CrawlerWsResources {
	
    public CrawlerWsResourcesTestCleaning(CrawlerWsConfiguration conf) {
		super(conf);
	}

	@GET
    @Timed
    public CrawlerWsSayingTestCleaning doGet(@QueryParam("page") String page) {
		
        java.net.URL url = null;
        
        try {
            url = new java.net.URL(page);
        }
        catch (Exception e) {
        	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Cleaning error");
        }
        
        try {
			String page_0 = null;
			String title_0 = null;
			String page_1 = null;
			String title_1 = null;
			String page_2 = null;
			String title_2 = null;
			String page_3 = null;
			String title_3 = null;
			String page_4 = null;
			String title_4 = null;
        	
        	HttpLoader urlLoader = new HttpLoader();
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
				TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_HTML);
				tikaWrapper.process(urlLoader.getStream());
				page_0 = tikaWrapper.getText();
				title_0 = tikaWrapper.getMetaTitle();
            } else {
            	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Error loading page");
            }            
            urlLoader.close();

            urlLoader = new HttpLoader();
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
            	TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_ARTICLE, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream(),TikaWrapper.CONTENT_TYPE_HTML);
				page_1 = tikaWrapper.getText();
				title_1 = tikaWrapper.getMetaTitle();
            } else {
            	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Error loading page");
            }
            urlLoader.close();
            
            urlLoader = new HttpLoader();
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
	        	TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_DEFAULT, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream(),TikaWrapper.CONTENT_TYPE_HTML);
				page_2 = tikaWrapper.getText();
				title_2 = tikaWrapper.getMetaTitle();
	        }
            urlLoader.close();
 
            urlLoader = new HttpLoader();
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
	        	TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_CANOLA, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream(),TikaWrapper.CONTENT_TYPE_HTML);
				page_3 = tikaWrapper.getText();
				title_3 = tikaWrapper.getMetaTitle();
            } else {
            	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Error loading page");
            }
            urlLoader.close();
	        
            urlLoader = new HttpLoader();
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
	        	TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_SNACKTORY, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream(),TikaWrapper.CONTENT_TYPE_HTML);
				page_4 = tikaWrapper.getText();
				title_4 = tikaWrapper.getMetaTitle();
            } else {
            	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Error loading page");
            }
            urlLoader.close();
            
        	return new CrawlerWsSayingTestCleaning(1, 
        			Base64.stringToStringBase64(page_0), title_0, 
        			Base64.stringToStringBase64(page_1), title_1, 
        			Base64.stringToStringBase64(page_2), title_2, 
        			Base64.stringToStringBase64(page_3), title_3, 
        			Base64.stringToStringBase64(page_4), title_4, 
        			0, "");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    	return new CrawlerWsSayingTestCleaning(0, "", "", "", "", "", "", "", "", "", "", 10, "Cleaning error");
    }
}