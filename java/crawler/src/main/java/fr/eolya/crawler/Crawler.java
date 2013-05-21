package fr.eolya.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import gnu.getopt.Getopt;

import fr.eolya.crawler.connectors.ConnectorFactory;
import fr.eolya.crawler.connectors.ISource;
import fr.eolya.crawler.database.CrawlerDBFactory;
import fr.eolya.crawler.database.ICrawlerDB;
import fr.eolya.crawler.queue.ISourceQueue;
import fr.eolya.crawler.queue.QueueFactory;
import fr.eolya.crawler.utils.CrawlerUtils;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;
import fr.eolya.utils.nosql.DBConnectionFactory;
import fr.eolya.utils.nosql.IDBConnection;

public class Crawler implements ICrawlerController {

	private Logger logger;
	private XMLConfig config;
	private boolean stopRequested;
	private List<String> countryInclude = null;
	private List<String> countryExclude = null;
	private IDBConnection dbConnection = null;
	private ICrawlerDB crawlerDB = null;

	public Crawler(XMLConfig config) throws UnknownHostException {
		this.config = config;
	}

	private static void usage() {
		System.out.println("Usage : java Crawler -p <properties file> [-a <account id>] [-s <source id>] [-c <crawler engine id>] [-o] [-e] [-d] [-r] [-l] [-i]");
		System.out.println("    -a : account id (default = 1)");
		System.out.println("    -s : source id (default = 1)");
		System.out.println("    -c : crawler engine id");
		System.out.println("    -o : once");
		System.out.println("    -e : suspicious only");
		System.out.println("    -d : reset source (-s required)");
		System.out.println("    -k : reset source from cache (-s required)");
		System.out.println("    -r : rescan source (-s required)");
		System.out.println("    -l : rescan source deeper (-s required)");
		System.out.println("    -i : interactive only (reset or rescan source request in web ui)");
		System.out.println("    -t : test only");
		System.out.println("    -v : verbose");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			usage();
			System.exit(-1);
		}

		Getopt g = new Getopt("Crawler", args, "p:a:s:c:oerditvk");
		g.setOpterr(false);
		int c;

		boolean once = false;
		boolean suspiciousOnly = false;
		boolean reScan = false;
		boolean interactiveOnly = false;
		boolean reset = false;
		boolean resetFromCache = false;
		boolean deeper = false;
		boolean test = false;
		String propFileName = "";
		String accountId = "1";
		String sourceId = "";
		String engineId = "";
		boolean verbose = false;

		while ((c = g.getopt()) != -1) {
			switch(c) {
			case 'p':
				propFileName = g.getOptarg();
				break;

			case 'a':
				accountId = g.getOptarg();
				if ("all".equals(accountId)) accountId="";
				break;

			case 's':
				sourceId = g.getOptarg();
				break;

			case 'c':
				engineId = g.getOptarg();
				break;

			case 'o':
				once = true;
				break;

			case 'i':
				interactiveOnly = true;
				break;

			case 'e':
				suspiciousOnly = true;
				once = true;
				break;

			case 'r':
				reScan = true;
				break;

			case 'k':
				resetFromCache = true;
				break;

			case 'd':
				reset = true;
				break;

			case 'l':
				deeper = true;
				break;

			case 't':
				test = true;
				once = true;
				break;

			case 'v':
				verbose = true;
				break;
			}
		}

		if ((reset || reScan || deeper || resetFromCache) && "".equals(sourceId)) {
			System.out.println("Error: source required with -r or -d option");
			System.exit(-1);
		}

		if ("".equals(propFileName)) {
			System.out.println("Error: no properties file specified");
			System.exit(-1);
		}

		propFileName = Utils.getValidPropertyPath(propFileName, null, "HOME");
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

		String witnessFilesPath = config.getProperty("/crawler/param[@name='witness_files_path']");
		witnessFilesPath = Utils.getValidPropertyPath(witnessFilesPath, null, "HOME");
		if (witnessFilesPath == null || "".equals(witnessFilesPath)) {
			System.out.println("Error : missing witness_files_path propertie");
			System.exit(-1);
		}

		String pidFileName = witnessFilesPath + "/crawler.pid";
		if (test) pidFileName = witnessFilesPath + "/crawler_test.pid";
		File filePid = new File(pidFileName);
		if (filePid.exists()) {
			System.out.println("A crawler instance is already running");
			System.exit(-1);
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePid.getAbsolutePath()));
			out.write(Utils.getProcessId());
			out.close();
		}
		catch (Exception e) {
			System.out.println("Error while creating file : " + filePid.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			Crawler crawler = new Crawler(config);
			crawler.run(accountId, sourceId, engineId, once, suspiciousOnly, interactiveOnly, reScan, reset, deeper, resetFromCache, test, verbose);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		filePid.delete();
	}

	public void run(
			String accountId, 
			String sourceId, 
			String engineId, 
			boolean once, 
			boolean suspiciousOnly, 
			boolean interactiveOnly,
			boolean reScan,
			boolean reset, 
			boolean deeper,
			boolean resetFromCache,
			boolean test,
			boolean verbose)
					throws InstantiationException, IllegalAccessException {

		boolean forceOnce = ("1".equals(config.getProperty("/crawler/param[@name='force_once']", "0")));
		if (forceOnce) once = true;

		String logFileName = "";
		if (test) logFileName = config.getProperty("/crawler/param[@name='log_filename_test']");
		if ("".equals(logFileName)) logFileName = config.getProperty("/crawler/param[@name='log_filename']");
		logFileName = Utils.getValidPropertyPath(logFileName, null, "HOME");
		logger = new Logger(logFileName);

		String witnessFilesPath = Utils.getValidPropertyPath(config.getProperty("/crawler/param[@name='witness_files_path']"), null, "HOME");

		String pidFileName = witnessFilesPath + "/crawler.pid";
		if (test) pidFileName = witnessFilesPath + "/crawler_test.pid";

		String stopFileName = witnessFilesPath + "/crawler.stop";
		if (test) stopFileName = witnessFilesPath + "/crawler_test.stop";

		File filePid = new File(pidFileName);
		File fileStop = new File(stopFileName);

		countryInclude = Arrays.asList(config.getProperty("/crawler/param[@name='country_include']", "").replaceAll("\\s*", "").split(","));
		countryExclude = Arrays.asList(config.getProperty("/crawler/param[@name='country_exclude']", "").replaceAll("\\s*", "").split(","));

		stopRequested = false;

		int limit = Integer.parseInt(config.getProperty("/crawler/param[@name='max_simultaneous_source']", "4"));
		if (!"".equals(sourceId)) limit = 1;

		logger.log("=================================");
		logger.log("Crawler starting");
		logger.log("    Simultaneous sources crawled : " + String.valueOf(limit));
		if (!"".equals(accountId))
			logger.log("    account : " + accountId);
		if (!"".equals(engineId))
			logger.log("    engine : " + engineId);
		if (once)
			logger.log("    mode once");
		if (!"".equals(sourceId))
			logger.log("    source : " + sourceId);
		if (suspiciousOnly)
			logger.log("    mode suspicious");
		if (reScan)
			logger.log("    mode rescan");
		if (reset)
			logger.log("    mode reset");
		if (deeper)
			logger.log("    mode deeper");
		if (interactiveOnly)
			logger.log("    mode interactive only");
		if (test)
			logger.log("    mode test");
		if (verbose)
			logger.log("    mode verbose");
		logger.log("");


		String dbType = config.getProperty("/crawler/database/param[@name='dbtype']", "");
		String dbName = config.getProperty("/crawler/database/param[@name='dbname']", "");

		dbConnection = getDBConnection(true);
		if (dbConnection==null) {
			logger.log("Failed to connect do database !");
			System.out.println("Failed to connect do database !");
			return;
		}
		ISourceQueue sourceQueue = QueueFactory.getSourceQueueInstance(dbType, dbConnection, dbName, "sources", test, interactiveOnly, suspiciousOnly, accountId, sourceId, engineId);
		crawlerDB = CrawlerDBFactory.getCrawlerDBInstance(dbType, dbConnection, dbName);

		logger.log("=================================");
		logger.log("");

		crawlerDB.fixStartupSourcesStatus();

		ThreadPoolExecutor sourceExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(limit);

		boolean bFinished = false;
		while (!stopRequested && !bFinished) {
			try {
				stopRequested = fileStop.exists() || !filePid.exists();
				if (stopRequested) break;
				
				// Refresh PID file time 
				try {
					FileUtils.touch(filePid);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}

				// How many sources are enqueued ?
				long countSource = sourceQueue.size();
				logger.log("    Sources to be crawled : " + String.valueOf(countSource));

				// If all threads are still processing a source wait and retry
				if (!sourceExecutor.getQueue().isEmpty() && (sourceExecutor.getActiveCount()==limit)) {	
					logger.log("    All threads are busy : wait and retry in a few seconds");
					Utils.sleep(15000);	
					continue;
				}

				// We pop a new source from source queue only if thread pool queue is empty
				// we want to pop a new source from source queue at the very last time 
				// as source queue content can change at any time 

				if (countSource==0 && once) {
					// try waiting 5 minutes
					logger.log("    No more source to crawl : start waiting 5 minutes");
					if ("".equals(sourceId)) {
						int waitingSince = 0;
						stopRequested = fileStop.exists() || !filePid.exists();
						while (waitingSince<300*1000 && countSource==0 && !stopRequested) {
							Utils.sleep(5000);
							waitingSince += 5000;
							countSource = sourceQueue.size();
							stopRequested = fileStop.exists() || !filePid.exists();
						}
					}
					if (countSource == 0) {
						// No source to crawl after waiting 5 minutes => stop crawling
						if (!stopRequested) logger.log("    No more source to crawl after waiting 5 minutes and mode once : stop crawling");
						bFinished = true;
						continue;
					}
				}

				//String json = sourceQueue.pop();
				Map<String,Object> srcData = sourceQueue.pop();
				//if (json!=null) {
				if (srcData!=null) {
					//HashMap<String,String> srcData = JSONHelper.getJSONMapString(json);
					//String srcId = String.valueOf(JSONHelper.getValueAsId((String)srcData.get("id")));
					String srcId = String.valueOf(srcData.get("id"));

					if (CrawlerUtils.isAcceptedCountry((String)srcData.get("country"), countryInclude, countryExclude) || !"".equals(sourceId)) {

						String sourceCrawlMode = CrawlerUtils.getSourceCrawlMode(Integer.parseInt((String)srcData.get("crawl_mode")), reScan, reset, deeper, resetFromCache) ;

						// Build the source item according to its type and so its class
						ISource src = ConnectorFactory.getSourceInstance(crawlerDB.getSourceClass((String)srcData.get("type")), srcId, sourceCrawlMode, srcData);

						if (src!=null && src.isCrawlAllowedBySchedule()) {
							logger.log("        Pushing source : " + String.valueOf(src.getId()));
							sourceExecutor.submit(new ProcessorSource(src, config, logger, this));
						} else {
							logger.log("        Skip source due to schedule : " + String.valueOf(src.getId()));									    
						}
					} else {
						logger.log("        Skip source due to country : " + srcId);                                      							    
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Stop requested ?
			stopRequested = fileStop.exists() || !filePid.exists();
			if (!bFinished && !stopRequested) Utils.sleep(15000);						
		}

		// Stop threads
		CrawlerUtils.executorShutdownAndWait(sourceExecutor, 0, 1, TimeUnit.DAYS);
	}

	/*
	 * (non-Javadoc)
	 * @see fr.eolya.crawler.IController#stopRequested()
	 */
	public boolean stopRequested() {
		return stopRequested;
	}
	public IDBConnection getDBConnection(boolean forceReconnect) {
		if (dbConnection==null || forceReconnect) {
			if (dbConnection!=null) {
				dbConnection.close();
				dbConnection = null;
			}
			String dbType = config.getProperty("/crawler/database/param[@name='dbtype']", "");
			String dbHost = config.getProperty("/crawler/database/param[@name='dbhost']", "localhost");
			int dbPort = Integer.parseInt(config.getProperty("/crawler/database/param[@name='dbport']", ""));
			String dbUser = config.getProperty("/crawler/database/param[@name='dbuser']", "");
			String dbPassword = config.getProperty("/crawler/database/param[@name='dbpassword']", "");
			dbConnection = DBConnectionFactory.getDBConnectionInstance(dbType, dbHost, dbPort, dbUser, dbPassword);
		}
		return dbConnection;
	}

	public ICrawlerDB getCrawlerDB() {
		return crawlerDB;
	}
}
