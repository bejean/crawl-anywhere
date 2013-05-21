package fr.eolya.simplepipeline.stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import fr.eolya.simplepipeline.SimplePipelineUtils;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLUtils;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.IndexerQueueWriter">
 *		<!--param name="solr">http://localhost:8080/solr/core</param-->
 *		<param name="queuedir">/tmp/out</param>
 *		<param name="tempfilenameprefix">t.</param>
 *		<param name="filenameprefix">j.</param>
 *		<param name="solrmappings">/Data/Projects/CrawlAnywhere/dev/java/simplepipeline/solrmapping.xml</param>
 *      <param name="solrboosts">config/pipeline/solrboost.xml</param>
 *      <param name="solrboostkey">item_id</param>
 *      <param name="esmappings">config/pipeline/esmapping.xml</param> 
 *	</stage>
 *
 * Solr mapping definition file sample :
 * 
 *  <solrmappings>
 *  	<mapping source="a"   target="field_a" />
 *  	<mapping source="b,c" target="field_b,field_c" enabled="no" />
 *  	<mapping source="d,e" target="field_d" format="[%1$s] - %2$s" />
 *  	<mapping value="a"    target="field_e" />
 *      <mapping source="a"   target="field_e" split="," normalization="lowercase" />
 *      <mapping source="meta_custom_(.*)" target="$_str,$_text" />    
 *  </solrmappings>
 *  
 *  Where :
 *  	"source" attribute list the source elements (each element in the list an xpath expression)
 *  	"target" attribute list the target solr fields
 *  	"value" attribute provide a string to be copied in the target solr field
 *  	"format" attribute provide a java Formatter output format. Default format is "%1$s %2$s ... %n$s". http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html
 *      "normalization" attribute can be "uppercase", "lowercase" or "date"
 *
 *		a mapping is enabled if "enabled" attribute = "" or "1" or "y" ou "yes" or "on" or is missing
 */

public class IndexerQueueWriter extends Stage {
    
    private int maxQueueSize = 0;
    private String queueDir = null;
    
    private HashMap<String,Document> mappingDoc = null;
    //private Document solrMappingDoc = null;
    //private Document esMappingDoc = null;
    private ArrayList<UrlBoost> urlBoosts = null;
    private String sorlBoostKey = null;
    
    
    /**
     * Perform initialization.
     */
    public void initialize() {
        super.initialize();
        
        // Output queue
        queueDir = props.getProperty("queuedir");
        queueDir = Utils.getValidPropertyPath(queueDir, null, "HOME");
        if (logger!=null) logger.log("    SolrIndexerQueueWriter - output queue = " + queueDir);
        
        loadMappings();
        
        sorlBoostKey = props.getProperty("solrboostkey");
        if (sorlBoostKey==null) sorlBoostKey = "";
        loadBoosts();
        if (logger!=null && urlBoosts!=null) logger.log("    SolrIndexerQueueWriter - boost rules : " + String.valueOf(urlBoosts.size()));
        
        String maxQueueSizetemp = props.getProperty("maxqueuesize");
        if (maxQueueSizetemp != null && !"".equals(maxQueueSizetemp) && NumberUtils.isNumber(maxQueueSizetemp)) 
            maxQueueSize = Integer.valueOf(maxQueueSizetemp);
    }
    
    private void loadMappings() {
        try {
            
            mappingDoc = new HashMap<String,Document>();
                            
            // Open Solr mapping definition file
            SAXReader reader = new SAXReader();
            reader.setValidation(false);
            String mappings = props.getProperty("solrmappings");
            mappings = Utils.getValidPropertyPath(mappings, null, "HOME");
            File f = new File(mappings);
            if (f==null || !f.exists()) {
                if (logger!=null && mappings!=null) logger.log("    SolrIndexerQueueWriter - Error loading Solr mapping rules. The rules file is missing : " + mappings);
            } else {
                if (logger!=null && mappings!=null) logger.log("    SolrIndexerQueueWriter - loading Solr mapping rules : " + mappings);
                mappingDoc.put("solr", reader.read(f));                
            }
            
            reader = new SAXReader();
            reader.setValidation(false);
            mappings = props.getProperty("esmappings");
            mappings = Utils.getValidPropertyPath(mappings, null, "HOME");
            f = new File(mappings);
            if (f==null || !f.exists()) {
                if (logger!=null && mappings!=null) logger.log("    SolrIndexerQueueWriter - Error loading ES mapping rules. The rules file is missing : " + mappings);
            } else {
                if (logger!=null && mappings!=null) logger.log("    SolrIndexerQueueWriter - loading ES mapping rules : " + mappings);
                mappingDoc.put("es", reader.read(f));                
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }        
    }
    
    private void loadBoosts() {
        
        try {
            urlBoosts = null;
            
            // Open Solr boosts definition file
            SAXReader reader = new SAXReader();
            reader.setValidation(false);
            
            String solrBoosts = props.getProperty("solrboosts");
            solrBoosts = Utils.getValidPropertyPath(solrBoosts, null, "HOME");
            
            File f = new File(solrBoosts);
            if (f==null || !f.exists()) {
                if (logger!=null && solrBoosts!=null) logger.log("    SolrIndexerQueueWriter - Error loading boost rules. The rules file is missing : " + solrBoosts);
                return;
            }
            
            if (logger!=null && solrBoosts!=null) logger.log("    SolrIndexerQueueWriter - loading boost rules : " + solrBoosts);
            
            Document boostdoc = reader.read(f);
            String defaultBoost = "";
            String defaultFields = "";
            
            Element defaults = (Element) boostdoc.selectSingleNode("//defaults");
            if (defaults!=null) {
                defaultBoost = defaults.attributeValue("boost");
                defaultFields = defaults.attributeValue("fields");
            }
            
            urlBoosts = new ArrayList<UrlBoost>();
            
            @SuppressWarnings("unchecked")
			List<Element> urlsList = boostdoc.selectNodes("//urls");
            Iterator<Element> urlsIter = urlsList.iterator();
            while (urlsIter.hasNext()) {
                Element urls = (Element) urlsIter.next();
                String boost = urls.attributeValue("boost");
                String fields = urls.attributeValue("fields");
                @SuppressWarnings("unchecked")
				List<Element> urlList = urls.elements();
                Iterator<Element> urlIter = urlList.iterator();
                while (urlIter.hasNext()) {
                    Element url = (Element) urlIter.next();
                    String match = url.attributeValue("match");
                    String pattern = url.getText();
                    urlBoosts.add(new UrlBoost(match, pattern, boost, fields, defaultBoost, defaultFields));
                    if (logger!=null && solrBoosts!=null) logger.log("    SolrIndexerQueueWriter - add boost rule for : " + pattern);
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } 
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
        
        if (logger!=null) logger.log("    solr document creation");
        
        String targetType = doc.getElementAttribute("//job", "target_type");
        if (!"solr".equals(targetType) && !"es".equals(targetType)) {
            if (logger!=null) logger.log("    not a Solr or elasticsearch target type : cancel");
            java.util.Date endTime = new java.util.Date();
            processingTime += (endTime.getTime() - startTime.getTime());
            
            if (nextStage != null)
                nextStage.processDoc(doc);  
            return;     
        }
        
        // Input 
        Document document = doc.getDocument();
        
        // Output
        Document solrqueuedoc = DocumentFactory.getInstance().createDocument("utf-8");
        solrqueuedoc.setXMLEncoding("utf-8");
        //solrqueuedoc.clearContent();
        
        String action = doc.getElementAttribute("//job", "action");
        if (action==null || "".equals(action))
            action = "add";
        
        Element job = solrqueuedoc.addElement("doc");
        job.addAttribute("action", action);		
        job.addAttribute("target_type", targetType);        
        
        
        String targetName = doc.getElementAttribute("//job", "target_name");
        
        //target_parameters="http://a/a"
        String targetUrl = doc.getElementAttribute("//job", "target_parameters");
        if (targetUrl!=null & !"".equals(targetUrl))
            job.addAttribute("target_url", targetUrl);		
        
        
        UrlBoost urlBoost = null;
        if ("solr".equals(targetType)) {
            if (!"".equals(sorlBoostKey) && urlBoosts!=null) {        
                Element e = ((Element) document.selectSingleNode("//" + sorlBoostKey));      
                if (e!=null) {
                    urlBoost = getUrlBoost(e.getText());
                    if (urlBoost!=null && urlBoost.all) {
                        if (logger!=null && urlBoost!=null) logger.log("    SolrIndexerQueueWriter - apply doc boost : " + urlBoost.boost);
                        job.addAttribute("boost", urlBoost.boost);                        
                    }
                }
            }    
        }
        
        String fileQueueDir  = getTargetQueueDir(targetName, targetType);
        if (fileQueueDir!=null && !"".equals(fileQueueDir)) {
            fileQueueDir = Utils.getValidPropertyPath(fileQueueDir, null, "HOME");
        }
        else {
            fileQueueDir = queueDir;
        }
        
        File queue = new File(fileQueueDir);
        if (queue==null || (queue.exists() && !queue.isDirectory())) {
            if (logger!=null) logger.log("        error with target queue : " + fileQueueDir);  
            throw new IOException("error with target queue : " + fileQueueDir);
        }
        
        //List<Element> list = mappingdoc.selectNodes("//mapping");
        List<Element> list = getTargetMapping(targetName, targetType);
        Iterator<Element> iter = list.iterator();
        while (iter.hasNext()) {
            Element mappingElement = (Element) iter.next();
            String enabled = mappingElement.attributeValue("enabled");
            if (enabled==null) 
                enabled = "";
            else
                enabled = enabled.trim().toLowerCase();
            
            if ("".equals(enabled) || "1".equals(enabled) || "y".equals(enabled) || "yes".equals(enabled) || "on".equals(enabled)) {
                
                boolean doMap = true;
                Element conditionElement = (Element) mappingElement.selectSingleNode("condition");
                if (conditionElement!=null) {
                    String conditionSource = conditionElement.attributeValue("source");
                    String conditionType = conditionElement.attributeValue("type");
                    String conditionValue = conditionElement.getText();
                    
                    String sourceValue = "";
                    if (conditionSource != null && !"".equals(conditionSource)) {
                        sourceValue = doc.getElementText("//" + conditionSource);
                    }
                    
                    if (sourceValue!=null && !"".equals(sourceValue)) {
                        if ("in".equals(conditionType) && !isIn(sourceValue,conditionValue))
                            doMap = false;
                        if ("not_in".equals(conditionType) && isIn(sourceValue,conditionValue))
                            doMap = false;
                    }
                    else
                        doMap = false;
                }
                
                if ("resetsource".equals(action)) {
                    String temp = mappingElement.attributeValue("value");
                    if (temp!=null)
                        doMap=false;
                }
                
                if (doMap) {
                    boolean noData = true;
                    String sources[] = null;
                    String temp = mappingElement.attributeValue("source");
                	String targets[] = mappingElement.attributeValue("target").split(",");

                	if (temp!=null && !"".equals(temp)) {
                    	
                        if (temp.indexOf("(")!=-1 && temp.indexOf(")")!=-1) {
                            // regex
                            // <mapping source="meta_custom_(.*)"   target="1"  />
                            
                            @SuppressWarnings("unchecked")
							List<Element> fields = ((Element) document.selectSingleNode("job")).elements();                            
                            for (int i=0;i<fields.size(); i++) {
                                Element e = fields.get(i);
                                String name = e.getName();
                                String target = Utils.regExpExtract(name, temp, 1);                              
                                if (target!=null) {
                                    String value = e.getText();
                                    if (value!=null && !"".equals(value)) {
                                        String values[] = new String[1];
                                        values[0] = value;
                                        
                                        //String targets[] = mappingElement.attributeValue("target").split(",");
                                        for (int j=0;j<targets.length; j++) {
                                            targets[j] = targets[j].replace("$", target).trim();
                                        }
                                        addElement(values, targets, job, mappingElement, urlBoost);
                                    }
                                }
                            }
                        } else {
                            sources = temp.split(",");
                            if (sources.length==1) {
                                String source = temp.trim();
                                @SuppressWarnings("unchecked")
								List<Element> fields = ((Element) document.selectSingleNode("job")).elements();                            
                                for (int i=0;i<fields.size(); i++) {
                                    
                                    Element e = fields.get(i);
                                    String name = e.getName();
                                    //String target = mappingElement.attributeValue("target");
                                    
                                    if (source.equals(name)) {
                                        String value = e.getText();
                                        if (value!=null && !"".equals(value)) {
                                            String values[] = new String[1];
                                            values[0] = value;
                                            
                                            //String targets[] = mappingElement.attributeValue("target").split(",");
                                            //for (int j=0;j<targets.length; j++) {
                                            //    targets[j] = targets[j].replace("$", target).trim();
                                            //}
                                            addElement(values, targets, job, mappingElement, urlBoost);
                                        }
                                    }
                                }
                            }
                            else {
                                for (int i=0; i<sources.length; i++) {
                                    String source = sources[i].trim();
                                    
                                    Element e = (Element) document.selectSingleNode("//" + source);
                                    if (e == null)
                                        sources[i] = "";
                                    else
                                        sources[i] = e.getText();
                                    
                                    if (sources[i]!=null && !"".equals(sources[i])) noData = false;
                                }
                                //String targets[] = mappingElement.attributeValue("target").split(",");
                                if (!noData) addElement(sources, targets, job, mappingElement, urlBoost);                                
                            }
                        }
                    }
                    else {
                        temp = mappingElement.attributeValue("value");
                        if (temp!=null)
                            sources = temp.split(",");
                        for (int i=0; i<sources.length; i++) {
                            sources[i] = sources[i].trim();
                            if (sources[i]!=null && !"".equals(sources[i])) noData = false;
                        }
                        //String targets[] = mappingElement.attributeValue("target").split(",");
                        if (!noData) addElement(sources, targets, job, mappingElement, urlBoost);
                    }
                }
            }
        }
        
        String tempQueueDir = SimplePipelineUtils.getTransformedPath(fileQueueDir, doc);
        File tgtDir = new File(tempQueueDir);
        tgtDir.mkdirs();
        
        if (maxQueueSize>0) {
            int currentSize = -1;
            while (currentSize > maxQueueSize || currentSize==-1) {
                if (currentSize > maxQueueSize) {
                    if (logger!=null) logger.log("       pause due to maximum queue size reached");
                    Utils.sleep(15000);	
                }
                if (sc.stopRequested()) return;
                queue = new File(tempQueueDir);
                if (queue.isDirectory()) currentSize = queue.listFiles().length;	
            }
        }
        
        String outputFile = XMLUtils.writeToQueue(Utils.getJobUid(), solrqueuedoc, tempQueueDir, props.getProperty("tempfilenameprefix"), props.getProperty("filenameprefix"), true, false);
        if (logger!=null) logger.log("    SolrIndexerQueueWriter - writed = " + outputFile);
        
        java.util.Date endTime = new java.util.Date();
        processingTime += (endTime.getTime() - startTime.getTime());
        
        if ("".equals(outputFile)) {
            stageList.setStagesStatus(StageList.STATUS_ERROR);
            return;
        }
        
        if (nextStage != null) {
            nextStage.processDoc(doc);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Element> getTargetMapping(String targetName, String targetType) {
        
        List<Element> targets = mappingDoc.get(targetType).selectNodes("//target");
        if (targets==null || targets.size()==0) return mappingDoc.get(targetType).selectNodes("//mapping");
        
        Iterator<Element> iter = targets.iterator();
        while (iter.hasNext()) {
            Element targetElement = (Element) iter.next();
            String name = targetElement.attributeValue("name");
            if ((name==null || "".equals(name)) && (targetName==null || "".equals(targetName))) return targetElement.selectNodes("mapping");
            if ((name!=null && targetName!=null && name.equals(targetName))) return targetElement.selectNodes("mapping");
        }
        iter = targets.iterator();
        while (iter.hasNext()) {
            Element targetElement = (Element) iter.next();
            String name = targetElement.attributeValue("name");
            if (name==null || "".equals(name) || "default".equals(name)) return targetElement.selectNodes("mapping");
        }        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private String getTargetQueueDir(String targetName, String targetType) {
        
        List<Element> targets = mappingDoc.get(targetType).selectNodes("//target");
        if (targets==null || targets.size()==0) return null;
        
        Iterator<Element> iter = targets.iterator();
        while (iter.hasNext()) {
            Element targetElement = (Element) iter.next();
            String name = targetElement.attributeValue("name");
            String queue = targetElement.attributeValue("queue_dir");
            if ((name==null || "".equals(name)) && (targetName==null || "".equals(targetName))) return queue;
            if ((name!=null && targetName!=null && name.equals(targetName))) return queue;
        }
        iter = targets.iterator();
        while (iter.hasNext()) {
            Element targetElement = (Element) iter.next();
            String name = targetElement.attributeValue("name");
            String queue = targetElement.attributeValue("queue_dir");
            if (name==null || "".equals(name)) return queue;
        }
        
        return null;
    }
    
    private void addElement(String sources[], String targets[], Element job, Element mappingElement, UrlBoost urlBoost) {
        
        String format = getFormat(mappingElement);
        String split = getSplit(mappingElement);
        String normalization = getNormalization(mappingElement);
        
        String strOut = "";
        if ("".equals(format)) {
            // without provided format, just concat
            for (int i=0; i<sources.length; i++) {
                if (i>0) strOut += " ";
                strOut += sources[i];
            }
        }
        else {
            // format
            strOut = String.format(format, (Object[])sources);
        }
        
        if (!"".equals(strOut)) {
            strOut = normalizevalue(strOut, normalization);
            
            for (int i=0; i<targets.length; i++) {
            	
            	targets[i] = String.format(targets[i], (Object[])sources);
            	
                targets[i] = targets[i].trim();
                if ("".equals(split)) {
                    Element e = job.addElement(targets[i]);
                    e.setText(strOut);
                    if (urlBoost!=null && urlBoost.fields.contains(targets[i])) {
                        if (logger!=null && urlBoost!=null) logger.log("    SolrIndexerQueueWriter - apply field boost : " + targets[i] + " -> " + urlBoost.boost);
                        e.addAttribute("boost", urlBoost.boost);                        
                    }
                }
                else {
                    String aOut[] = strOut.split(split);
                    for (int j=0; j<aOut.length; j++) {                        
                        Element e = job.addElement(targets[i]);
                        e.setText(aOut[j].trim());
                        if (urlBoost!=null && urlBoost.fields.contains(targets[i])) {
                            if (logger!=null && urlBoost!=null) logger.log("    SolrIndexerQueueWriter - apply field boost : " + targets[i] + " -> " + urlBoost.boost);
                            e.addAttribute("boost", urlBoost.boost);                        
                        }
                    }
                }
            }
        } 
    }
    
    private String normalizevalue(String value, String normalization) {
        String res = value;
        if ("lowercase".equals(normalization.toLowerCase()))
            res = res.toLowerCase();
        if ("uppercase".equals(normalization.toLowerCase()))
            res = res.toUpperCase();
        if ("date".equals(normalization.toLowerCase())) {
            res = null;
            Pattern p = Pattern.compile("[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}(\\s|T)[0-9]{2}:[0-9]{2}:[0-9]{2}.*");
            Matcher m = p.matcher(value);
            if (m.find()) res = value;

            if (res==null) {
                p = Pattern.compile("[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}");
                m = p.matcher(value);
                if (m.find()) res = value + " 00:00:00";
            }
            
            if (res==null) {
                if (logger!=null) logger.log("    date rejected : " + res);
                res = "";
            }
        }       
        return res;
    }
    
    private boolean isIn(String value, String valuesList) {
        if (value == null || "".equals(value)) return false;
        
        if (valuesList != null && !"".equals(valuesList)) {
            value = value.toLowerCase();

            // si value est dans valuesList, on retourne true
            String[] aValue = valuesList.split(",");
            for (int i = 0; i < aValue.length; i++) {
                if (value.startsWith(aValue[i].trim())) return true;
            }
            return false;
        }
        return false;
    }
    
    private String getFormat(Element mappingElement) {
        String format = mappingElement.attributeValue("format");
        if (format==null) format = "";   
        format = format.replace("\\n", "\n");
        return format;
    }
    
    private String getSplit(Element mappingElement) {
        String split = mappingElement.attributeValue("split");
        if (split==null) split = "";   
        return split;
    }
    
    private String getNormalization(Element mappingElement) {
        String normalization = mappingElement.attributeValue("normalization");
        if (normalization==null) normalization = "";   
        return normalization;
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
    
    private UrlBoost getUrlBoost(String url) {
        for (int i=0; i<urlBoosts.size(); i++) {
            UrlBoost urlBoost = urlBoosts.get(i);
            if ("regex".equals(urlBoost.match)) {
                Pattern p = Pattern.compile(urlBoost.urlPattern);
                Matcher m = p.matcher(url);
                if (m.find()) {
                    return urlBoost;
                }
            }
            if ("exact".equals(urlBoost.match) && url.equals(urlBoost.urlPattern)) return urlBoost;
            if ("contains".equals(urlBoost.match) && url.indexOf(urlBoost.urlPattern)!=-1) return urlBoost;
        }
        return null;
    }
    
    /**
     * 
     */
    public class UrlBoost {
        public String match;
        public String urlPattern;
        public String boost;
        public ArrayList<String> fields;
        public boolean all;
        
        public UrlBoost(String match, String urlPattern, String boost, String csvFields, String boostDefault, String csvFieldsDefault) {
            this.match = match;
            this.urlPattern = urlPattern;
            this.all = false;
            
            if (boost==null || "".equals(boost)) boost = boostDefault;
            if (boost==null || "".equals(boost)) boost = "1.0";
            
            Float f = null;
            try {
                f = Float.parseFloat(boost);
            } catch(Exception e) {
                e.printStackTrace();
            }
            if (f == null) boost = "1.0";
            
            if (csvFields==null || "".equals(csvFields)) csvFields = csvFieldsDefault;
            if (csvFields==null || "".equals(csvFields)) csvFields = "*";
            
            this.boost = boost;
            this.fields = new ArrayList<String>(Arrays.asList(csvFields.split(",")));
            for(int i=0; i<this.fields.size(); i++) { 
                this.fields.set(i, this.fields.get(i).trim());
                if ("*".equals(this.fields.get(i))) this.all = true;
            }
        }
    }
}
