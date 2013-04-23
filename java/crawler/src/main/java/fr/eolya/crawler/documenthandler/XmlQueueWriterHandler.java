package fr.eolya.crawler.documenthandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.utils.Base64;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;
import fr.eolya.utils.XMLUtils;

public class XmlQueueWriterHandler extends BaseHandler implements IDocumentHandler {
    
    private int checkQueueSizeEach = 10;
    private int checkQueueSizeNext = 0;
    
    public XmlQueueWriterHandler() {}
    
    public void init(String outputType, String targetName, String targetType, String targetParameters, String targetQueueDir, XMLConfig config) {
        super.init(outputType, targetName, targetType, targetParameters, targetQueueDir, config);
        checkQueueSizeEach = getPropertyInt("checkqueuesize_each", "10");
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public int status() {
        
        if (checkQueueSizeNext == 0) {
            String jobsPath = getProperty("jobspath");
            if (getTargetQueueDir()!=null && !"".equals(getTargetQueueDir())) jobsPath = getTargetQueueDir();
            jobsPath = Utils.getValidPropertyPath(jobsPath, null, "HOME");
            
            int jobsLimit = getPropertyInt("jobslimit", "0");
            
            int jobsCount = 0;
            
            if (jobsLimit > 0 && jobsPath != null) {
                File f = new File(jobsPath);
                if (f.isDirectory()) {
                    jobsCount = f.listFiles().length;
                }
                if (jobsCount > jobsLimit)
                    return STATUS_PAUSE_REQUIRED;
            }
            checkQueueSizeNext = checkQueueSizeEach;
        } else
            checkQueueSizeNext--;
        
        return STATUS_OK;
    }
    
    public void sendDoc(String sourceId, String itemId, String accountId, InputStream streamData, HashMap<String, String> params,
                    HashMap<String, String> metas, XMLConfig extra, IConnector cnx) {
        sendDocInternal(sourceId, itemId, accountId, streamData, null, params, metas, extra, cnx);
    }
    
    public void sendDoc(String sourceId, String itemId, String accountId, String rawData, HashMap<String, String> params,
                    HashMap<String, String> metas, XMLConfig extra, IConnector cnx) {
        sendDocInternal(sourceId, itemId, accountId, null, rawData, params, metas, extra, cnx);
    }
    
    public void resetSource(String sourceId) {

        String uid = Utils.getJobUid();
        
        Document doc = DocumentFactory.getInstance().createDocument("utf-8");
        doc.setXMLEncoding("utf-8");
        
        Element job = doc.addElement("job");
        
        // action
        job.addAttribute("action", "resetsource");
        
        // target
        job.addAttribute("target_name", getTargetName());
        job.addAttribute("target_type", getTargetType());
        job.addAttribute("target_parameters", getTargetParameters());
        
        // source id
        job.addElement("source_id").addText(sourceId);
        
        try {
            String jobsPath = getProperty("jobspath");
            if (getTargetQueueDir()!=null && !"".equals(getTargetQueueDir())) jobsPath = getTargetQueueDir();
            jobsPath = Utils.getValidPropertyPath(jobsPath, null, "HOME");
            String xmlFile = XMLUtils.writeToQueue(uid, doc, jobsPath, "t.", "j.", true, false);
            if (logger != null && xmlFile!=null) logger.log("                           File : " + xmlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void removeDoc(String sourceId, String itemId, String accountId) {
        
        String uid = Utils.getJobUid();
        
        Document doc = DocumentFactory.getInstance().createDocument("utf-8");
        doc.setXMLEncoding("utf-8");
        
        Element job = doc.addElement("job");
                
        // action
        job.addAttribute("action", "remove");
        
        // target
        job.addAttribute("target_name", getTargetName());
        job.addAttribute("target_type", getTargetType());
        job.addAttribute("target_parameters", getTargetParameters());
        
        job.addElement("source_id").addText(sourceId);
        job.addElement("account_id").addText(accountId);
        
        // item id
        job.addElement("item_id").addText(itemId);
        
        if (logger != null) logger.log("                           Url : " + itemId);
        try {
            System.out.println("Writing : " + itemId);
            String jobsPath = getProperty("jobspath");
            if (getTargetQueueDir()!=null && !"".equals(getTargetQueueDir())) jobsPath = getTargetQueueDir();
            jobsPath = Utils.getValidPropertyPath(jobsPath, null, "HOME");
            String xmlFile = XMLUtils.writeToQueue(uid, doc, jobsPath, "t.", "j.", true, false);
            if (logger != null && xmlFile!=null) logger.log("                           File : " + xmlFile);
        } catch (IOException e) {
            System.out.println(itemId);
            e.printStackTrace();
        }
        
    }
    
    protected void sendDocInternal(String sourceId, String itemId, String accountId, InputStream streamData, String rawData,
                    HashMap<String, String> params, HashMap<String, String> metas, XMLConfig extra, IConnector cnx) {
        
        String uid = Utils.getJobUid();
        
        Document doc = DocumentFactory.getInstance().createDocument("utf-8");
        doc.setXMLEncoding("utf-8");
        
        Element job = doc.addElement("job");
        
        // action
        job.addAttribute("action", "add");
        
        // target
        job.addAttribute("target_name", getTargetName());
        job.addAttribute("target_type", getTargetType());
        job.addAttribute("target_parameters", getTargetParameters());
        
        if (extra!=null) {
            // extra
            try {
                Element e1 = job.addElement("extra");
                Element e2 = (Element) extra.getDocument().getRootElement().clone();
                e1.add(e2);
            }
            catch (Exception e) {}
        }
        
        // source type
        job.addElement("source_type").addText(cnx.getName());
        
        // source id
        job.addElement("source_id").addText(sourceId);
        job.addElement("account_id").addText(accountId);
        
        // source name
        if (metas.get("name")!=null)
            job.addElement("source_name").addText(metas.get("name"));
        
        // source collections
        if (metas.get("collections")!=null)
            job.addElement("source_collections").addText(metas.get("collections"));
        
        // source tags
        if (metas.get("tags")!=null)
            job.addElement("source_tags").addText(metas.get("tags"));
        
        // source comment
        if (metas.get("comment")!=null)
            job.addElement("source_comment").addText(metas.get("comment"));
        
        // source contact
        if (metas.get("contact")!=null)
            job.addElement("source_contact").addText(metas.get("contact"));
        
        // source country
        if (metas.get("country")!=null)
            job.addElement("source_country").addText(metas.get("country"));
        
        // source language
        if (metas.get("language")!=null)
            job.addElement("source_language").addText(metas.get("language"));
        
        // meta custom
        if (metas.get("meta_custom")!=null && !"".equals(metas.get("meta_custom"))) {
            String[] aMetas = metas.get("meta_custom").split("\\|");
            for (int i=0;i<aMetas.length; i++) {
                String[] aItems = aMetas[i].split(":");
                if (aItems.length==2) {
                    String key = aItems[0].trim();
                    String value = aItems[1].trim();
                    job.addElement("meta_custom_" + key).addText(value);
                }
            }
        }
        
        // meta links
        int i = 1;
        while (true) {
            String link = metas.get("link"+String.valueOf(i++));
            if (link==null || "".equals(link)) break;
            job.addElement("item_link").addText(link);
        }
        
        // user agent
        if (params.get("useragent")!=null)
            job.addElement("source_useragent").addText(params.get("useragent"));
        
        // source user agent
        if (params.get("languageDetectionList")!=null)
            job.addElement("source_candidatelanguages").addText(params.get("languageDetectionList"));
        
        // item id
        job.addElement("item_id").addText(itemId);
        
        // item url
        if (params.get("url")!=null)
            job.addElement("item_url").addText(params.get("url"));
        
        // item referrer
        if (params.get("referrer")!=null)
            job.addElement("item_referrer").addText(params.get("referrer"));
        
        // item type mime
        if (params.get("contentType")!=null)
            job.addElement("item_contenttype").addText(params.get("contentType"));
        
        // item content size
        if (params.get("contentSize")!=null)
            job.addElement("item_contentsize").addText(params.get("contentSize"));

        // item charset
        if (params.get("contentCharSet")!=null)
            job.addElement("item_charset").addText(params.get("contentCharSet"));
        
        // item referer charset
        if (params.get("refererCharset")!=null)
            job.addElement("item_referercharset").addText(params.get("refererCharset"));
        
        // item declared language 
        if (params.get("declaredLanguage")!=null)
            job.addElement("item_declaredlanguage").addText(params.get("declaredLanguage"));
        
        // item original contenttype (flash became text/html)
        if (params.get("originalContentType")!=null)
            job.addElement("item_originalcontenttype").addText(params.get("originalContentType"));
        
        // item first crawl date
        if (params.get("firstCrawlDate")!=null)
            job.addElement("item_firstcrawldate").addText(params.get("firstCrawlDate"));
        
        // item depth
        if (params.get("depth")!=null)
            job.addElement("item_depth").addText(params.get("depth"));
        
        // automatic cleaning
        if (params.get("automaticCleaning")!=null) {
            if ("1".equals(params.get("automaticCleaning")))
                job.addElement("item_clean_method").addText("boilerpipe_article");
            if ("2".equals(params.get("automaticCleaning")))
                job.addElement("item_clean_method").addText("boilerpipe_default");
            if ("3".equals(params.get("automaticCleaning")))
                job.addElement("item_clean_method").addText("boilerpipe_canola");
            if ("4".equals(params.get("automaticCleaning")))
                job.addElement("item_clean_method").addText("snacktory");
        }
        
        for (Map.Entry<String, String> item : metas.entrySet()) {
            if (item.getKey().startsWith("meta_")) {
                job.addElement("item_" + item.getKey().replace(':', '_').replace('-', '_').replace('.', '_').replace('/', '_')).addText(item.getValue());
            }
        }
        
        // content (base64)
        boolean isBase64 = false;
        if (streamData!=null || rawData!=null)
        {
            String contentBase64 = "";
            try {
                if (streamData!=null) {
                    isBase64 = true;
                    contentBase64 = Base64.inputStreamToStringBase64(streamData);
                    job.addElement("content").addText(contentBase64).addAttribute("encoding", "base64");
                }
                else {	
                    job.addElement("content").addText(StringEscapeUtils.escapeHtml4(rawData));
                }
            } catch (IOException e) {
                System.out.println(itemId);
                e.printStackTrace();
            } catch (OutOfMemoryError oo) {
                System.out.println("OOE - Too big file?");
                oo.printStackTrace();
            }
        }
        
        if (logger != null) logger.log("                           Url : " + itemId);
        try {
            System.out.println("Writing : " + itemId);
            String jobsPath = getProperty("jobspath");
            if (getTargetQueueDir()!=null && !"".equals(getTargetQueueDir())) jobsPath = getTargetQueueDir();
            jobsPath = Utils.getValidPropertyPath(jobsPath, null, "HOME");
            String xmlFile = XMLUtils.writeToQueue(uid, doc, jobsPath, "t.", "j.", true, isBase64);
            if (logger != null && xmlFile!=null) logger.log("                           File : " + xmlFile);
        } catch (IOException e) {
            if (logger != null) logger.log("                           Error : " + e.getMessage());
            System.out.println(itemId);
            e.printStackTrace();
        } catch (OutOfMemoryError oo) {
            if (logger != null) logger.log("                           Error : " + oo.getMessage());
            System.out.println("OOE - Too big file?");
            oo.printStackTrace();
        }
        
    }
}
