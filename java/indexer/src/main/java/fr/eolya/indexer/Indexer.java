package fr.eolya.indexer;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
//import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;
import gnu.getopt.Getopt;

public class Indexer {
    
	protected Logger logger;
    protected boolean stopRequested;
    
    protected Hashtable<String,IEngine> engines = null; 
    
    protected int commitEach = 0;
    protected int commitWithin = 0;
    protected int optimizeEach = 0;

    private String requestHandler = null;
    
    protected String settingsDirectoryPath = null;
    protected XMLConfig config;
    
    private static void usage() {
        System.out.println("Usage : java Indexer -p <properties file> [-o] [-v] [-r] [-c] [-s url] [-e url] [-f]");
        System.out.println("    -o : once");
        System.out.println("    -v : verbose");
        System.out.println("    -r : reset index");
        System.out.println("    -c : optimize index only");
        System.out.println("    -s : solr core url");
        System.out.println("    -e : elasticsearch index url");
        System.out.println("    -f : force url");
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }
        
        Getopt g = new Getopt("Indexer", args, "p:orcvus:f");
        g.setOpterr(false);
        int c;
        
        boolean once = false;
        boolean optimizeOnly = false;
        boolean verbose = false;
        boolean resetindex = false;
        String propFileName = "";
        String solrCoreUrl = "";
        String elasticsearchIndexUrl = "";
        boolean forceUrl = false;
        
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'p':
                    propFileName = g.getOptarg();
                    break;
                    
                case 'o':
                    once = true;
                    break;
                    
                case 'r':
                    resetindex = true;
                    break;
                    
                case 'c':
                    optimizeOnly = true;
                    break;
                    
                case 'v':
                    verbose = true;
                    break;
                    
                case 's':
                    solrCoreUrl = g.getOptarg();
                    break;
                    
                case 'e':
                    elasticsearchIndexUrl = g.getOptarg();
                    break;
                    
                case 'f':
                    forceUrl = true;
                    break;
            }
        }
        
        if ("".equals(propFileName)) {
            System.out.println("Error: no properties file specified");
            System.exit(-1);
        }
        propFileName = Utils.getValidPropertyPath(propFileName, null, "HOME");
        String settingsDirectoryPath = new File(propFileName).getParentFile().getAbsolutePath();
        
        System.out.println("Config file = " + propFileName);
        
        XMLConfig config = new XMLConfig();
        try {
            File configFile =new File(propFileName);
            if (!configFile.exists()) {
                System.out.println("Error configuration file not found [" + propFileName + "]");
                System.exit(-1);
            }
            config.loadFile(propFileName);
        } 
        catch(IOException e) {
            System.out.println("Error while reading properties file");
            e.printStackTrace();
            System.exit(-1);			
        }
        
        String witnessFilesPath = config.getProperty("/indexer/param[@name='witnessfilespath']");
        witnessFilesPath = Utils.getValidPropertyPath(witnessFilesPath, null, "HOME");
        if (witnessFilesPath == null || "".equals(witnessFilesPath)) {
            System.out.println("Error : missing witness_files_path propertie");
            System.exit(-1);
        }
        
        String witnessFilesName = config.getProperty("/indexer/param[@name='witnessfilesname']", "indexer");
        
        File filePid = new File(witnessFilesPath + "/" + witnessFilesName + ".pid");
        if (filePid.exists()) {
            System.out.println("A indexer instance is already running");
            System.exit(-1);
        }
        
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filePid.getAbsolutePath()));
            out.write(Utils.getProcessId());
            out.close();
        } catch (Exception e) {
            System.out.println("Error while creating file : " + filePid.getAbsolutePath());
            e.printStackTrace();
            System.exit(-1);
        }
        
        try {
            Indexer indexer = new Indexer(config, settingsDirectoryPath);
            indexer.run(once, verbose, resetindex, optimizeOnly, solrCoreUrl, elasticsearchIndexUrl, forceUrl);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (filePid.exists()) {
                filePid.delete();
            }
        }
    }
    
    public Indexer(XMLConfig config, String settingsDirectoryPath) {
        this.config = config;
        this.settingsDirectoryPath = settingsDirectoryPath;
    }
    
    public void run(boolean once, boolean verbose, boolean resetindex, boolean optimizeOnly, String solrCoreUrl, String elasticsearchIndexUrl, boolean forceUrl) {
        
        String logFilesPath = config.getProperty("/indexer/param[@name='logfilename']");
        logFilesPath = Utils.getValidPropertyPath(logFilesPath, null, "HOME");
        logger = new Logger(logFilesPath);
        
        String witnessFilesPath = config.getProperty("/indexer/param[@name='witnessfilespath']");
        witnessFilesPath = Utils.getValidPropertyPath(witnessFilesPath, null, "HOME");
        
        String witnessFilesName = config.getProperty("/indexer/param[@name='witnessfilesname']", "indexer");
        
        File filePid = new File(witnessFilesPath + "/" + witnessFilesName + ".pid");
        File fileStop = new File(witnessFilesPath + "/" + witnessFilesName + ".stop");
        
        String filesQueue = config.getProperty("/indexer/param[@name='queuepath']");
        filesQueue = Utils.getValidPropertyPath(filesQueue, null, "HOME");
        
        if (filesQueue==null && "".equals(filesQueue)) { // for backward compatibility
            filesQueue = config.getProperty("/indexer/param[@name='queupath']");
            filesQueue = Utils.getValidPropertyPath(filesQueue, null, "HOME");
        }
        
        String filesPattern = config.getProperty("/indexer/param[@name='queuefilepattern']");
        
        commitWithin = Integer.parseInt(config.getProperty("/indexer/solr/param[@name='commitwithin']", "0").trim());
        commitEach = Integer.parseInt(config.getProperty("/indexer/solr/param[@name='commiteach']", "0").trim());
        optimizeEach = Integer.parseInt(config.getProperty("/indexer/solr/param[@name='optimizeeach']", "0").trim());
        requestHandler = config.getProperty("/indexer/solr/param[@name='requesthandler']");
        
        int stopAfterMaxSuccessiveError = Integer.parseInt(config.getProperty("/indexer/param[@name='stopaftermaxsuccessivesolrerror']", "3").trim());
        
        logger.log("=================================");
        logger.log("Indexer starting");
        if (once)
            logger.log("    mode once");
        if (verbose)
            logger.log("    mode verbose");
        if (optimizeOnly)
            logger.log("    mode optimize only");
        logger.log("=================================");
        logger.log("");
        
        // initiate Solr
        
        engines = new Hashtable<String,IEngine> ();
        
        //solrCores = new Hashtable<String,SolrCore> ();
        String solrUrl = "";
        if (solrCoreUrl!=null && !"".equals(solrCoreUrl)) {
            solrUrl = solrCoreUrl;
        } else {
            solrUrl = config.getProperty("/indexer/solr/param[@name='baseurl']");
            if (solrUrl!=null && !"".equals(solrUrl)) {
                String solrCoreName = config.getProperty("/indexer/solr/param[@name='corename']");
                if (solrCoreName != null && !"".equals(solrCoreName))
                    solrUrl = solrUrl + solrCoreName;
            }
        }
        
        if (solrUrl!=null && !"".equals(solrUrl)) {
            SolrCore solrCore = createSolrCore(solrUrl, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler);
            if (solrCore!=null) {
                //return;
                
                if (optimizeOnly) {
                    solrCore.optimize(true);
                }
                
                if (resetindex)
                    if (!solrCore.resetIndex())
                        return;
                
                //solrCores.put(solrUrl, solrCore);
                engines.put(solrUrl, solrCore);
            }
        }
        
        
        // initiate elasticsearch
        //elasticSearchIndexes = new Hashtable<String,ElasticSearchIndex> ();
        String elasticSearchUrl = "";
        if (elasticsearchIndexUrl!=null && !"".equals(elasticsearchIndexUrl)) {
            elasticSearchUrl = elasticsearchIndexUrl;
        } else {
            elasticSearchUrl = config.getProperty("/indexer/elasticsearch/param[@name='baseurl']");
            if (elasticSearchUrl!=null && !"".equals(elasticSearchUrl)) {
                String elasticSearchIndexName = config.getProperty("/indexer/elasticsearch/param[@name='indexname']");
                if (elasticSearchIndexName != null && !"".equals(elasticSearchIndexName))
                    elasticSearchUrl = elasticSearchUrl + elasticSearchIndexName;
            }
        }
        
        if (elasticSearchUrl!=null && !"".equals(elasticSearchUrl)) {
            ElasticSearchIndex elasticSearchIndex = createElasticSearchIndex(elasticSearchUrl, commitWithin, commitEach, optimizeEach, logger, verbose);
            if (elasticSearchIndex!=null) {
                //return;
                
                if (optimizeOnly) {
                    elasticSearchIndex.optimize(true);
                }
                
                if (resetindex)
                    if (!elasticSearchIndex.resetIndex())
                        return;
                
                //elasticSearchIndexes.put(elasticSearchUrl, elasticSearchIndex);
                engines.put(elasticSearchUrl, elasticSearchIndex);
                
            }
        }
        
        if (optimizeOnly) {
            filePid.delete();
            logger.log("Indexer ending");
            return;
        }
        
        stopRequested = false;
        boolean firstNoMoreFile = true;
        try {
            while (!stopRequested) {
                stopRequested = fileStop.exists() || !filePid.exists();
                if (stopRequested) {
                    break;
                }
                
                int fileCount = 0;
                
                File f = new File(filesQueue);
                if (f.isDirectory()) {
                    File[] fl = Utils.getListFileAlphaOrder(f);
                    if (fl.length>0) {
                        
                        java.util.Date dstartloop = new java.util.Date();
                        int successiveErrorCount = 0;
                        
                        for (int i = 0; i < Math.min(fl.length, 1000); i++) {
                            stopRequested = fileStop.exists() || !filePid.exists();
                            if (stopRequested) {
                                break;
                            }
                            if (fl[i].isFile() && fl[i].getName().matches(filesPattern)) {
                                logger.log("processing : " + filesQueue + "/" + fl[i].getName());
                                System.out.println("processing : " + filesQueue + "/" + fl[i].getName());
                                int ret = processOneFile(filesQueue + "/" + fl[i].getName(), solrUrl, elasticSearchUrl, forceUrl, verbose);
                                if (ret == -2) {
                                    // Not pinging -> file will be retry later.
                                    logger.log("Solr not pinging. Current file processing will be retry later in a few seconds.");
                                    // not pinging -> pause
                                    Utils.sleep(15000);
                                }	
                                else {
                                    boolean fileSuccess = true;
                                    if (ret == -1) {
                                        // File error -> remove from queue
                                        logger.log("File error. File removed from queue !");
                                        fileSuccess = false;
                                        successiveErrorCount++;
                                    }
                                    else {
                                        successiveErrorCount=0;
                                        if (ret == 0) fileSuccess = false;
                                    }
                                    
                                    fileDone (fl[i], fileSuccess, config);
                                    fileCount++;
                                    
                                    if (successiveErrorCount>=stopAfterMaxSuccessiveError) {
                                        // Erreur fatale (solr ???)
                                        filePid.delete();
                                        logger.log("Maximum successive Solr fatal error count reached - stop process !");
                                        return;
                                    }
                                }
                            }
                        }
                        
                        java.util.Date dendloop = new java.util.Date();
                        long timeloop = (dendloop.getTime() - dstartloop.getTime());
                        
                        if (timeloop>0) {
                            String msg = "Loop : \n";
                            msg += "    time (sec)                  = " + String.valueOf(timeloop / 1000) + "\n";
                            msg += "    doc                         = " + String.valueOf(Math.min(fl.length, 1000)) + "\n";
                            msg += "    time per doc (ms)           = " + String.valueOf(timeloop / Math.min(fl.length, 1000)) + "\n";
                            msg += "    docs per minute             = " + String.valueOf((Math.min(fl.length, 1000) * 60 * 1000) / timeloop) + "\n";
                            msg += "    memory (free / max / total) = " + String.valueOf(Runtime.getRuntime().freeMemory()) + " / " + String.valueOf(Runtime.getRuntime().maxMemory()) + " / " + String.valueOf(Runtime.getRuntime().totalMemory());
                            logger.log (msg);
                        }
                    }			
                    else {	
                        if (firstNoMoreFile) {
                            enginesCommit(true);
                            firstNoMoreFile = false;
                        }
                    }
                }
                
                if (fileCount == 0 && once) {
                    enginesCommit(true);
                    boolean queueEmpty = true;
                    logger.log("    No more file to index : start waiting 5 minutes");
                    for (int j=0; j<100 && !stopRequested && queueEmpty; j++) {
                        stopRequested = fileStop.exists() || !filePid.exists();
                        Utils.sleep(3000);
                        if (queueLength(filesQueue, filesPattern) > 0) queueEmpty = false;
                    }
                    if (queueLength(filesQueue, filesPattern) == 0) {
                        if (!stopRequested) logger.log("    No more file to index after waiting 5 minutes and mode once : stop indexing");
                        break;
                    }
                }
                else {
                    enginesCommit(false);
                    firstNoMoreFile = true;
                    Utils.sleep(2000);
                }				
                
            }
            enginesTerminate(false);
        } catch (Exception e) {
            System.err.println("An error occured: ");
            e.printStackTrace();
        }
        
        filePid.delete();
        logger.log("Fin du traitement");
        return;
    }
    
    private void fileDone(File f, boolean success, XMLConfig config) {
        String tgtPath;
        if (success) {
            tgtPath = config.getProperty("/indexer/param[@name='onsuccessmoveto']");
        } else {
            tgtPath = config.getProperty("/indexer/param[@name='onerrormoveto']");
        }
        tgtPath = Utils.getValidPropertyPath(tgtPath, null, "HOME");
        
        if (tgtPath!=null && !"".equals(tgtPath)) {
            File tgtDir = new File(tgtPath);
            tgtDir.mkdirs();
            File f2 = new File(tgtPath + "/" + f.getName());
            if (f2.exists()) f2.delete();
            try {
                FileUtils.moveFile(f, f2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (f.exists())
            f.delete();	
    }
    
    private int processOneFile(String fileName, String solrUrl, String elasticsearchUrl, boolean forceUrl, boolean verbose) {
        
        boolean allOk = true;
        //SolrCore docSolrCore = null;
        IEngine engine = null;
        
        try {
            SAXReader reader = new SAXReader();
            reader.setValidation(false);
            Document document = null;
            try {
                document = reader.read(new File(fileName));
            } catch (Exception e) {
                logger.log("     error (reader.read) : " + e.getMessage());
                if (verbose) logger.logStackTrace(e, true);
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            List<Element> jobs = document.selectNodes("//doc");
            Iterator<Element> iterJobs = jobs.iterator();
            while (iterJobs.hasNext()) {
                
                //Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
                Collection<InputDocument> docs = new ArrayList<InputDocument>();
                
                Element job = (Element) iterJobs.next();
                
                String docTargetType = job.attributeValue("target_type");
                String targetUrl = "";
                if ("solr".equals(docTargetType)) targetUrl = solrUrl;
                if ("es".equals(docTargetType)) targetUrl = elasticsearchUrl;
                
                // Specific solr core url ?
                String docTargetUrl = job.attributeValue("target_url");
                //if (docTargetUrl==null || "".equals(docTargetUrl)) docTargetUrl = job.attributeValue("solr_url"); // TODO: remove in a future version
                
                //                if (docTargetUrl!=null && !"".equals(docTargetUrl) && !(!"".equals(targetUrl) && forceUrl)) {
                //                    docSolrCore = solrCores.get(docTargetUrl);
                //                    if (docSolrCore==null) {
                //                        docSolrCore = createSolrCore(docTargetUrl, commitWithin, commitEach, optimizeEach, logger, verbose);
                //                        if (docSolrCore!=null)
                //                            solrCores.put(docTargetUrl, docSolrCore);
                //                    }
                //                }
                //                else {
                //                    if (targetUrl!=null && !"".equals(targetUrl))
                //                        docSolrCore = solrCores.get(targetUrl);
                //                }
                //                
                //                if (docSolrCore!=null) {
                //                    
                //                    logger.log("    Solr url              = " + docSolrCore.getUrl());
                
                if (docTargetUrl!=null && !"".equals(docTargetUrl) && !(!"".equals(targetUrl) && forceUrl)) {
                    engine = engines.get(docTargetUrl);
                    if (engine==null) {
                        engine = createEngine(docTargetType, docTargetUrl, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler);
                        if (engine!=null)
                            engines.put(docTargetUrl, engine);
                    }
                }
                else {
                    if (targetUrl!=null && !"".equals(targetUrl))
                        engine = engines.get(targetUrl);
                }
                
                if (engine!=null) {
                    
                    logger.log("    Solr url              = " + engine.getUrl());
                    // action ?
                    String action = "";
                    String value = job.attributeValue("action");
                    if (value != null) action = value;
                    
                    if ("resetsource".equals(action) || "remove".equals(action)) {
                        String query = "";
                        
                        List<Element> childs = job.elements();
                        Iterator<Element> iterChilds = childs.iterator();
                        while (iterChilds.hasNext()) {
                            Element field = (Element) iterChilds.next();
                            
                            String fieldName = field.getName();
                            String fieldValue = field.getText();
                            
                            if (fieldValue != null && !"".equals(fieldValue)) {
                                if (!"".equals(query)) query += " AND ";
                                query += fieldName + ":\"" + fieldValue + "\"";
                            }
                        }
                        if (!"".equals(query)) {
                            if ("resetsource".equals(action)) {
                                logger.log("     reset source (" + query+ ")");
                            } else {
                                logger.log("     remove item (" + query+ ")");
                            }
                            //docSolrCore.deleteByQuery(query);
                            engine.deleteByQuery(query);
                            //if (!docSolrCore.commit(true)) {
                            if ("resetsource".equals(action) && !engine.commit(true)) {
                                logger.log("     error during commit");
                                //if (!docSolrCore.ping()) {
                                if (!engine.ping()) {
                                    logger.log("     not pinging");
                                    return -2;
                                }
                                return -1;								
                            }
                        }
                        else {
                            logger.log("     reset source error (no query)");
                        }
                    }
                    
                    if (action==null || "".equals(action) || "add".equals(action)) {
                        
                        //SolrInputDocument doc = new SolrInputDocument();
                        
                        InputDocument doc = new InputDocument(null, config.getProperty("/indexer/elasticsearch/param[@name='configured_languages']"));
                        
                        String boostDoc = job.attributeValue("boost");
                        if (boostDoc!=null && !"".equals(boostDoc)) {
                            //doc.setDocumentBoost(Float.parseFloat(boostDoc));
                            doc.setDocumentBoost(boostDoc);
                        }
                        
                        List<Element> childs = job.elements();
                        Iterator<Element> iterChilds = childs.iterator();
                        while (iterChilds.hasNext()) {
                            Element field = (Element) iterChilds.next();
                            
                            String fieldName = field.getName();
                            String fieldValue = field.getText();
                            
                            if (fieldValue != null && !"".equals(fieldValue)) {
                                
                                String boostField = field.attributeValue("boost");
                                
                                // Date ?
                                if (fieldValue.matches("[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}.*"))
                                    fieldValue = fieldValue.trim().replace(' ', 'T') + "Z";		
                                
                                if (boostField!=null && !"".equals(boostField)) {
                                    //doc.addField(fieldName, fieldValue, Float.parseFloat(boostField));
                                    doc.addField(fieldName, fieldValue, boostField);
                                } else {
                                    doc.addField(fieldName, fieldValue);                                    
                                }
                                if (verbose)
                                    logger.log("    " + fieldName + "                                      ".substring(fieldName.length()) + " = " + fieldValue);
                            }
                        }
                        docs.add(doc);
                    }
                    
                    if (docs.size() > 0) {
                        try {
                            //                            if (docSolrCore.addDocuments(docs) == -1) {
                            //                                if (!docSolrCore.ping())
                            if (engine.addDocuments(docs) == -1) {
                                if (!engine.ping())
                                    return -2;
                                return -1;
                            }
                        } catch (Exception e) {							
                            logger.log("     error add : " + e.getMessage());
                            if (verbose) logger.logStackTrace(e, true);
                            // Ping ??
                            //if (!docSolrCore.ping())
                            if (!engine.ping())
                                return -2;
                            else {
                                System.err.println("An error occured: ");
                                e.printStackTrace();
                                return 0;
                            }
                        }
                    }
                }
                else {
                    allOk = false;
                }
            }
        } catch (Exception e) {
            logger.log("     error : " + e.getMessage());
            if (verbose) logger.logStackTrace(e, false);
            System.err.println("An error occured: ");
            e.printStackTrace();
            return -1;
        }
        if (allOk)
            return 1;
        else
            return 0;
    }
    
    private int queueLength(String path, String filesPattern) {
        File f = new File(path);
        File[] fl = f.listFiles();
        int fileCount = fl.length;
        int ret = 0;
        for (int i=0; i<fileCount; i++)
            if (fl[i].isFile() && fl[i].getName().matches(filesPattern)) ret++;		
        return ret;
    }
    
    
    private IEngine createEngine(String docTargetType,String docTargetUrl, int commitWithin, int commitEach, int optimizeEach, Logger logger, boolean verbose, String requestHandler) {
        if ("solr".equals(docTargetType)) return createSolrCore(docTargetUrl, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler);
        if ("es".equals(docTargetType)) return createElasticSearchIndex(docTargetUrl, commitWithin, commitEach, optimizeEach, logger, verbose);
        return null;
    }
    
    private SolrCore createSolrCore(String solrUrl, int commitWithin, int commitEach, int optimizeEach, Logger logger, boolean verbose, String requestHandler) {
        logger.log("Create new Solr core connection : " + solrUrl);
        SolrCore solrCore = new SolrCore(solrUrl, commitWithin, commitEach, optimizeEach, logger, requestHandler);        
        solrCore.setOutputStackTrace(verbose);
        
        if (!solrCore.connect())
            return null;
        
        if (!solrCore.ping()) 
            return null;
        
        return solrCore;
    }
    
    private ElasticSearchIndex createElasticSearchIndex(String elasticSearchUrl, int commitWithin, int commitEach, int optimizeEach, Logger logger, boolean verbose) {
        logger.log("Create new elasticsearch index connection : " + elasticSearchUrl);
        
        try {
            URL url = new URL(elasticSearchUrl);
            String host = url.getHost();
            int port = url.getPort();
            if (port<=0) port = 9300;
            String index = url.getPath().replaceAll("/", "");
            ElasticSearchIndex es = new ElasticSearchIndex(host, port, index, logger);
            es.connect();
            es.open(settingsDirectoryPath, config.getProperty("/indexer/elasticsearch/param[@name='configured_languages']"), false);
            return es;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void enginesCommit(boolean force) {
        Iterator<String> itr = engines.keySet().iterator();
        while (itr.hasNext()) {
            String url = itr.next();
            engines.get(url).commit(force);
        }
        //        Iterator<String> itr = solrCores.keySet().iterator();
        //        while (itr.hasNext()) {
        //            String coreUrl = itr.next();
        //            solrCores.get(coreUrl).commit(force);
        //        }
        //        itr = elasticSearchIndexes.keySet().iterator();
        //        while (itr.hasNext()) {
        //            String coreUrl = itr.next();
        //            elasticSearchIndexes.get(coreUrl).commit(force);
        //        }
    }
    
    private void enginesTerminate(boolean optimize) {
        Iterator<String> itr = engines.keySet().iterator();
        while (itr.hasNext()) {
            String url = itr.next();
            engines.get(url).terminate(optimize);
        }
        //        Iterator<String> itr = solrCores.keySet().iterator();
        //        while (itr.hasNext()) {
        //            String coreUrl = itr.next();
        //            solrCores.get(coreUrl).terminate(optimize);
        //        }
        //        itr = elasticSearchIndexes.keySet().iterator();
        //        while (itr.hasNext()) {
        //            String coreUrl = itr.next();
        //            elasticSearchIndexes.get(coreUrl).terminate(optimize);
        //        }
    }
}
