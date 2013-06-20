package fr.eolya.crawler.connectors;

import java.util.Date;
import java.util.HashMap;

import fr.eolya.crawler.ICrawlerController;
import fr.eolya.crawler.cache.IDocumentCache;
import fr.eolya.crawler.database.ICrawlerDB;
import fr.eolya.crawler.documenthandler.IDocumentHandler;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.utils.CrawlerUtilsCommon;
import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;

public abstract class Connector {
	
	private ISource src;
	protected ISourceItemsQueue queue;
	protected IDocumentCache docCache;
	protected Logger logger;
	protected XMLConfig config;
	protected ICrawlerController crawlerController;
	
    protected IDocumentHandler dh;

    protected String outputType;
    protected String targetType;
    protected String targetParameters;
    protected String targetName;
    protected String targetQueueDir;
    
    protected boolean verbose = false;
    protected boolean debug = false;

    protected long updateProcessingInfoLastTime;

    public boolean initializeInternal(Logger logger, XMLConfig config, ISource src, ISourceItemsQueue queue, ICrawlerController controller) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		this.src = src;
		this.queue = queue;
		this.logger = logger;
		this.config = config;   
		this.crawlerController = controller;
        
        verbose = "1".equals(config.getProperty("/crawler/param[@name='log_verbose']", "0"));
        debug = "1".equals(config.getProperty("/crawler/param[@name='log_debug']", "0"));
        
        // Get output class name
        ICrawlerDB db = crawlerController.getCrawlerDB();
        HashMap<String, String> target = db.getTarget(String.valueOf(src.getTargetId()));
        if (target==null) return false;
        
        outputType = target.get("output_type");
        targetName = target.get("name");
        targetType = target.get("target_type");
        targetParameters = target.get("target_parameters");
        targetQueueDir = target.get("queue_dir");
  
        if (outputType==null || "".equals(outputType))
            outputType = "default";
        
        if (targetType==null || "".equals(targetType))
            targetType = "solr";
        
        String className = config.getProperty("/crawler/documenthandler/" + outputType + "/param[@name='classname']", "");
        
        if ("".equals(className)) {
            if (logger!=null) logger.log("Failed to initiate Web Connector (no class name)");
            src.memLogAppend("Failed to initiate Web Connector (no class name)");
            return false;
        }
        
        @SuppressWarnings("rawtypes")
		Class fc = Class.forName(className);
        dh = (IDocumentHandler) fc.newInstance();
        dh.init(outputType, targetName, targetType, targetParameters, targetQueueDir, config);
        if (verbose && logger!=null) dh.setLogger(logger);		
        
        updateProcessingInfoLastTime = 0;
        
        return true;
    }
	
	public void setQueue(ISourceItemsQueue queue) {
		this.queue = queue;
	}
	
	public void setDocumentCache(IDocumentCache docCache) {
		this.docCache = docCache;
	}
	

	public static boolean isAcceptedUrl(String url, String filteringRules) {

		String mode = CrawlerUtilsCommon.getUrlMode(url, filteringRules, "a");
		if ("s".equals(mode))
			return false;
		else
			return true;
	}
	
	public static boolean isAcceptedExtension(String extension, String extensionExclude) {
		if (extension == null || "".equals(extension))
			return true;

		extension = extension.toLowerCase().trim();

		if (extensionExclude != null && !"".equals(extensionExclude)) {
			// si contentType est dans contentTypeExclude, on retourne false
			String[] aExtensionExclude = extensionExclude.toLowerCase().split(",");
			for (int i = 0; i < aExtensionExclude.length; i++) {
				if (extension.startsWith(aExtensionExclude[i].trim()))
					return false;
			}
			return true;
		}
		return true;
	}
	
	public static boolean isAcceptedContentType(String contentType, String contentTypeInclude, String contentTypeExclude) {
		if (contentType == null || "".equals(contentType))
			return true;

		contentType = contentType.toLowerCase();

		if (contentTypeInclude != null && !"".equals(contentTypeInclude)) {
			// si contentType est dans contentTypeInclude, on retourne true
			String[] aContentTypeInclude = contentTypeInclude.split(",");
			for (int i = 0; i < aContentTypeInclude.length; i++) {
				if (contentType.startsWith(aContentTypeInclude[i].trim()))
					return true;
			}
			return false;
		}

		if (contentTypeExclude != null && !"".equals(contentTypeExclude)) {
			// si contentType est dans contentTypeExclude, on retourne false
			String[] aContentTypeExclude = contentTypeExclude.split(",");
			for (int i = 0; i < aContentTypeExclude.length; i++) {
				if (contentType.startsWith(aContentTypeExclude[i].trim()))
					return false;
			}
			return true;
		}
		return true;
	}

	protected synchronized void updateProcessingInfo(int srcId, long queueSize, long doneQueueSize, long processedItemCount) { //, boolean force) {
		boolean doIt = false;
		long nowTime = new Date().getTime();
		if (updateProcessingInfoLastTime==0) {
			doIt = true;
		}
		else {
			if ((nowTime-updateProcessingInfoLastTime)>30000) {
				doIt = true;   
			}
		}
		doIt = (processedItemCount>0 && doIt); //|| force;
		if (!doIt) return;

		updateProcessingInfoLastTime = nowTime;
		
		String processingInfo = buildProcessedInfoXml(doneQueueSize);
		ICrawlerDB db = crawlerController.getCrawlerDB();
		db.updateSourceProcessingInfo(src.getId(), queueSize, doneQueueSize, processingInfo);		
	}
    
    private String buildProcessedInfoXml(long doneQueueSize) {
        long lastTime = src.getProcessingLastTime();
        long newTime = new Date().getTime();
        long pageCount = doneQueueSize - src.getProcessingLastProcessedPageCount();
        int currentSpeed = 0;
        try {
            currentSpeed = (int) (pageCount / ((newTime - lastTime) / 1000)); 
        } catch (Exception e) {}
        long elapsedTime = src.getProcessingElapsedTime() + (newTime - lastTime);
        
        String processingLastTime = String.valueOf(newTime);
        String processingCurrentSpeed = String.valueOf(currentSpeed);
        String processingElapsedTime = String.valueOf(elapsedTime);
        
        src.setProcessingElapsedTime(elapsedTime);
        src.setProcessingLastProcessedPageCount(doneQueueSize);
        src.setProcessingLastTime(newTime);
       
        String ret = "<?xml version=\"1.0\"?>";
        ret += "<infos>";
        ret += "<elapsedtime>" + processingElapsedTime + "</elapsedtime>";
        ret += "<lasttime>" + processingLastTime + "</lasttime>";
        ret += "<currentspeed>" + processingCurrentSpeed + "</currentspeed>";
        ret += "</infos>";
        return ret;
    }
}
