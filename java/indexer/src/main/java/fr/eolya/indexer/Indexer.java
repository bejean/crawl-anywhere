package fr.eolya.indexer;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.commons.io.FileUtils;
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
    private boolean solrUseJavaBin = false;
    
    protected String settingsDirectoryPath = null;
    protected XMLConfig config;
    
    private static void usage() {
        System.out.println("Usage : java Indexer -p <properties file> [-o] [-v] [-r] [-c] [-s url] [-f]");
        System.out.println("    -o : once");
        System.out.println("    -v : verbose");
        System.out.println("    -r : reset index");
        System.out.println("    -c : optimize index only");
        System.out.println("    -s : solr core url");
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
            indexer.run(once, verbose, resetindex, optimizeOnly, solrCoreUrl, forceUrl);
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
    
    public void run(boolean once, boolean verbose, boolean resetindex, boolean optimizeOnly, String solrCoreUrl, boolean forceUrl) {
        
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
        solrUseJavaBin = ("javabin".equals(config.getProperty("/indexer/solr/param[@name='jababin']", "")));
        
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
        
        String solrUrl = "";
        String solrCoreName = null;
        if (solrCoreUrl!=null && !"".equals(solrCoreUrl)) {
            solrUrl = solrCoreUrl;
        } else {
            solrUrl = config.getProperty("/indexer/solr/param[@name='baseurl']");
            if (solrUrl!=null && !"".equals(solrUrl)) {
                solrCoreName = config.getProperty("/indexer/solr/param[@name='corename']");
                //if (solrCoreName != null && !"".equals(solrCoreName))
                //    solrUrl = solrUrl + solrCoreName;
            } else {
	            String solrHome = config.getProperty("/indexer/solr/param[@name='home']");
	            if (solrHome!=null && !"".equals(solrHome)) {
	            	solrUrl = solrHome;
	                solrCoreName = config.getProperty("/indexer/solr/param[@name='corename']");
	                //if (solrCoreName != null && !"".equals(solrCoreName))
	                //    solrUrl = solrUrl + solrCoreName;
	            }
            }
        }
        
        if (solrUrl!=null && !"".equals(solrUrl)) {
            SolrCore solrCore = createSolrCore(solrUrl, solrCoreName, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler, solrUseJavaBin);
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
                                int ret = processOneFile(filesQueue + "/" + fl[i].getName(), solrUrl, forceUrl, verbose);
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
    
    private int processOneFile(String fileName, String solrUrl, boolean forceUrl, boolean verbose) {
        
        boolean allOk = true;
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
                
                Collection<InputDocument> docs = new ArrayList<InputDocument>();
                
                Element job = (Element) iterJobs.next();
                
                String targetUrl = solrUrl;
                
                // Specific solr core url ?
                String docTargetUrl = job.attributeValue("target_url");                
                if (docTargetUrl!=null && !"".equals(docTargetUrl) && !(!"".equals(targetUrl) && forceUrl)) {
                    engine = engines.get(docTargetUrl);
                    if (engine==null) {
                        engine = createEngine(docTargetUrl, null, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler, solrUseJavaBin);
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
                        
                        @SuppressWarnings("unchecked")
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
                            engine.deleteByQuery(query);
                            if ("resetsource".equals(action) && !engine.commit(true)) {
                                logger.log("     error during commit");
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
                        InputDocument doc = new InputDocument();
                        String boostDoc = job.attributeValue("boost");
                        if (boostDoc!=null && !"".equals(boostDoc)) {
                            doc.setDocumentBoost(boostDoc);
                        }
                        
                        @SuppressWarnings("unchecked")
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
    
    
    private IEngine createEngine(String docTargetUrl, String corename, int commitWithin, int commitEach, int optimizeEach, Logger logger, boolean verbose, String requestHandler, boolean solrUseJavaBin) {
        createSolrCore(docTargetUrl, corename, commitWithin, commitEach, optimizeEach, logger, verbose, requestHandler, solrUseJavaBin);
        return null;
    }
    
    private SolrCore createSolrCore(String solrUrl, String corename, int commitWithin, int commitEach, int optimizeEach, Logger logger, boolean verbose, String requestHandler, boolean solrUseJavaBin) {
        logger.log("Create new Solr core connection : " + solrUrl);
        SolrCore solrCore;
		try {
			solrCore = new SolrCore(solrUrl, corename, commitWithin, commitEach, optimizeEach, logger, requestHandler, solrUseJavaBin);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}        
        solrCore.setOutputStackTrace(verbose);
        
        if (!solrCore.connect())
            return null;
        
        if (!solrCore.ping()) 
            return null;
        
        return solrCore;
    }
    
    private void enginesCommit(boolean force) {
        Iterator<String> itr = engines.keySet().iterator();
        while (itr.hasNext()) {
            String url = itr.next();
            engines.get(url).commit(force);
        }
    }
    
    private void enginesTerminate(boolean optimize) {
        Iterator<String> itr = engines.keySet().iterator();
        while (itr.hasNext()) {
            String url = itr.next();
            engines.get(url).terminate(optimize);
        }
    }
}
