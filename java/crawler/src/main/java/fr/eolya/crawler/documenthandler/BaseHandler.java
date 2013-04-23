package fr.eolya.crawler.documenthandler;

import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;

//abstract public class BaseHandler implements IDocumentHandler {
abstract public class BaseHandler {
    
    public static int STATUS_OK = 0;
    public static int STATUS_PAUSE_REQUIRED = 1;
    
    private String outputType = null;
    private String targetName = null;
    private String targetType = null;
    private String targetParameters = null;
    private String targetQueueDir = null;
    protected XMLConfig config = null;
    Logger logger = null;
    
    public BaseHandler() {}
    
    public void init(String outputType, String targetName, String targetType, String targetParameters, String targetQueueDir, XMLConfig config) {
        
        this.targetName = targetName;
        
        if (outputType==null || "".equals(outputType)) {
            this.outputType = "default";
        }
        else {
            this.outputType = outputType;			
        }
        
        if (targetType==null || "".equals(targetType)) {
            this.targetType = "default";
        }
        else {
            this.targetType = targetType;			
        }
        
        this.targetParameters = targetParameters;
        
        this.targetQueueDir = targetQueueDir;
        
        this.config = config;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
        
    protected String getProperty(String name) {
        return config.getProperty("/crawler/documenthandler/" + outputType + "/param[@name='" + name + "']", "");
    }
    
    protected int getPropertyInt(String name) {
        return Integer.parseInt(config.getProperty("/crawler/documenthandler/" + outputType + "/param[@name='" + name + "']", ""));
    }
    
    protected String getProperty(String name, String defValue) {
        return config.getProperty("/crawler/documenthandler/" + outputType + "/param[@name='" + name + "']", defValue);
    }
    
    protected int getPropertyInt(String name, String defValue) {
        return Integer.parseInt(config.getProperty("/crawler/documenthandler/" + outputType + "/param[@name='" + name + "']", defValue));
    }
    
    public String getOutputType() {
        return outputType;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public String getTargetParameters() {
        return targetParameters;
    }
    
    public String getTargetQueueDir() {
        return targetQueueDir;
    }
}
