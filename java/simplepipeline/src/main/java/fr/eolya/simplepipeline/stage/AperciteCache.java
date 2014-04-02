package fr.eolya.simplepipeline.stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;

import fr.eolya.simplepipeline.connector.threads.QueueItem;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.AperciteCache">
 *		<param name="onaction">add</param>
 *		<param name="url">item_url</param>
 *		<param name="apercite_image_target">apercite_image</param>
 *		<param name="apercite_cache_path">/tmp</param>
 *		<param name="apercite_tempo">30000</param>
 *		<param name="apercite_url_target">apercite_url</param>
 *		<param name="apercite_resolution">120x90</param>
 *	</stage>
 */

public class AperciteCache extends Stage {
    
    /**
     * Perform initialization.
     */
    public void initialize() {
        super.initialize();
    }
    
    @Override
    public void processDoc(Doc doc) throws Exception {
        
        // Check onaction
        if (!doProcess(doc)) {
            if (nextStage != null)
                nextStage.processDoc(doc);	
            return;
        }
        
        java.util.Date startTime = new java.util.Date();
        
        if (logger!=null) logger.log("    field copy");
        
        // Input
        String urlElement = "";
        String urlStr = "";
        String resolution = "";
        String aperciteUrlTargetElement = "";
        String aperciteImageTargetElement = "";
        String aperciteCachePath = "";
        String aperciteTempo = "";
        
        urlElement = props.getProperty("url");
        if (urlElement != null && !"".equals(urlElement)) {
        	urlStr = doc.getElementText("//" + urlElement);
        }
        
        resolution = props.getProperty("apercite_resolution");
        if (resolution == null || "".equals(resolution)) {
        	resolution = "120x90";
        }
        
        aperciteTempo = props.getProperty("apercite_tempo");
        if (aperciteTempo == null || "".equals(aperciteTempo)) {
        	aperciteTempo = "30000";
        }
 

        aperciteUrlTargetElement = props.getProperty("apercite_url_target");
        aperciteImageTargetElement = props.getProperty("apercite_image_target");
        aperciteCachePath = props.getProperty("apercite_cache_path");
        if (aperciteImageTargetElement == null || "".equals(aperciteImageTargetElement) 
        		|| aperciteUrlTargetElement == null || "".equals(aperciteUrlTargetElement) 
        		|| aperciteCachePath == null || "".equals(aperciteCachePath)) {
            if (nextStage != null)
                nextStage.processDoc(doc);	
            return;
        }
        
        // Check if the target directory exists
        File cacheDirectory = new File (aperciteCachePath);
        if (cacheDirectory==null || !cacheDirectory.isDirectory()) {
            if (nextStage != null)
                nextStage.processDoc(doc);	
            return;
        }

        // Get the root page for this url
        URL url = new URL(urlStr);
        String homeHost = url.getHost();
        String homeUrl =  url.getProtocol() + "://" + url.getHost();
        if (url.getPort()!=80 && url.getPort()!=-1)
        	homeUrl += String.valueOf(url.getPort());
        
        // build the apercite url
        String aperciteUrl = "http://www.apercite.fr/api/apercite/" + resolution + "/no/";
        aperciteUrl += URLEncoder.encode(homeUrl, "UTF-8");

        // build the target file name
        String cacheFile = homeHost.replace(".","_") + "_" + resolution.replace("/x/g","_");
        String cacheFilePath = aperciteCachePath + "/" + cacheFile;
        String cacheWitnessFilePath = cacheFilePath + ".tmp";
        
        
        // Check if this site is currently processing or already processed
        File destination = null;
        File witness = null;
        synchronized (this) {
        	boolean stop = false;
        	witness = new File(cacheWitnessFilePath);
	        if (witness!=null && witness.exists()) stop = true;
 
	        destination = new File(cacheFilePath);
	        if (destination!=null && destination.exists()) stop = true;
	        //if (destination==null) stop = true;

	        if (stop) {
	            if (nextStage != null)
	                nextStage.processDoc(doc);	
	            return;
	        }
	        
	        // Create witness file
	        new FileOutputStream(witness).close();
        }
               
        URL aperciteAPIUrl = new URL(aperciteUrl);
        FileUtils.copyURLToFile(aperciteAPIUrl, destination);
        int tempo = Math.max(Integer.valueOf(aperciteTempo), 10000);
        Utils.sleep(tempo);
        FileUtils.copyURLToFile(aperciteAPIUrl, destination);
        
        // set the Apercite URL
		doc.addElement("/job", aperciteUrlTargetElement, aperciteUrl);

        // set the apercite file name
		doc.addElement("/job", aperciteImageTargetElement, cacheFile);
      
        java.util.Date endTime = new java.util.Date();
        processingTime += (endTime.getTime() - startTime.getTime());
        
        // remove witness file 
        witness.delete();
        
        if (nextStage != null) {
            nextStage.processDoc(doc);
        }		
    }
    
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }
    
    
	public static void main(String[] args) {
		
		AperciteCache ac = new AperciteCache();
		
		String docSrc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><job action=\"add\"></job>";
		
		Doc doc;
		try {
			doc = new Doc(docSrc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		doc.addElement("/job", "url", "http://www.eolya.fr");	
		doc.addElement("/job", "apercite_target", "apercite_image");	
		doc.addElement("/job", "apercite_target", "apercite_image");	
		doc.addElement("/job", "apercite_target", "apercite_image");	
		doc.addElement("/job", "apercite_target", "apercite_image");	

		/*
		 * <param name="url">item_url</param>
 *		<param name="apercite_target">apercite_image</param>
 *		<param name="apercite_cache_path">apercite_image</param>
 *		<param name="apercite_url">item_url</param>
 * *		<param name="apercite_resolution">120x90</param>

		 */
		
		
	}
    
}
