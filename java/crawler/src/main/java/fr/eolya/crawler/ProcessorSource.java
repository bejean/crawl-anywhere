package fr.eolya.crawler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import fr.eolya.crawler.cache.DocumentCacheFactory;
import fr.eolya.crawler.cache.IDocumentCache;
import fr.eolya.crawler.connectors.ConnectorFactory;
import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.crawler.connectors.ISource;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.crawler.utils.CrawlerUtils;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;

public class ProcessorSource extends Processor implements Callable<Integer>, ISourceControler {

	private ISource src;
	private ISourceItemsQueue queueItems;
	private IDocumentCache docCache;
	private IConnector connector;
	private static final Object monitor = new Object();

	private int maxThreads;
	private long processedItemCount;

	public ProcessorSource(ISource src, XMLConfig config, Logger logger, ICrawlerController controller) {
		super(config, logger, controller);
		this.src = src;
		processedItemCount = 0;
		System.out.println("Creating id : " + src.getId());
	}

	public Integer call() throws Exception {
		System.out.println("Starting id : " + src.getId());

		connector = ConnectorFactory.getConnector(src.getClassName());
		if (connector==null) {
			// TODO: V4 - updateSourceStatusDueToStartupError();
			throw new InstantiationException("Fail too instanciate connetor (type = " + src.getType() + ")");
		}

		String dbType = config.getProperty("/crawler/queues/param[@name='dbtype']", "");
		String dbQueuesName = config.getProperty("/crawler/queues/param[@name='dbname']", "");
		if (StringUtils.trimToNull(dbType)==null || StringUtils.trimToNull(dbQueuesName)==null) {
			dbType = config.getProperty("/crawler/database/param[@name='dbtype']", "");
			dbQueuesName = config.getProperty("/crawler/database/param[@name='dbname']", "");
		}

		queueItems = ConnectorFactory.getSourceItemsQueueInstance(src, dbType, crawlerController.getDBConnection(false), dbQueuesName, "pages" );
		if (queueItems==null) {
			// TODO: V4 - updateSourceStatusDueToStartupError();
			throw new InstantiationException("Fail too initalize connetor (type = " + src.getType() + ")");
		}

		if (!connector.initialize( logger, config, src, queueItems, crawlerController)) {
			// TODO: V4 - updateSourceStatusDueToStartupError();
			throw new InstantiationException("Fail too initalize connetor (type = " + src.getType() + ")");
		}
		
		String dbCacheType = config.getProperty("/crawler/cache/param[@name='dbtype']", "");
		String dbCacheName = config.getProperty("/crawler/cache/param[@name='dbname']", "");
        if (!"".equals(dbCacheType)) {
        	docCache = DocumentCacheFactory.getDocumentCacheInstance(dbCacheType, crawlerController.getDBConnection(false), dbCacheName, "pages_cache", String.valueOf(src.getId()));
        	if (docCache==null) {
    			// TODO: V4 - updateSourceStatusDueToStartupError();
    			throw new InstantiationException("Fail too initalize cache (type = " + dbCacheType + ")");        		
        	}
        	if (src.isReset()) docCache.reset();
        	connector.setDocumentCache(docCache);
        }

		// maxThreads
		if ("0".equals(src.getUrlConcurrency())) {
			String temp = config.getProperty("/crawler/param[@name='max_simultaneous_item_per_source']", "2");
			maxThreads = Integer.parseInt(temp);
		}
		else {
			maxThreads = Integer.parseInt(src.getUrlConcurrency());
		}
		maxThreads = connector.getMaxThreads(maxThreads);

		ThreadPoolExecutor sourceItemsExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
		for (int i=0;i<maxThreads;i++) {
			if (i==1) Utils.sleep(10000);
			if (i>1) Utils.sleep(5000);
			sourceItemsExecutor.submit(new ProcessorSourceItems(queueItems, connector, config, logger, crawlerController, this, i));
		}

		// Stop threads
		CrawlerUtils.executorShutdownAndWait(sourceItemsExecutor, 10000, 1, TimeUnit.DAYS);

		// Notify the ThreadController that we're done
		if (isPauseRequested()) {
			if (isPauseRequested())
				logger.log("Stop crawling - pause requested");
			else
				logger.log("Stop crawling - suspended");
			paused(false);
		}
		else {
			boolean isScheduledPause = !src.isCrawlAllowedBySchedule();
			if (isScheduledPause) {
				logger.log("Stop crawling - pause scheduled");
				paused(true);
			}
			else {
				if (isStopRequested()) 
					logger.log("Stop crawling - stop requested");
				else
					logger.log("Stop crawling - queue empty");
				finished();
			}
		}		

		Utils.sleep(10000);

		System.out.println("Terminating id : " + src.getId());
		return null;
	}

	public void incrementProcessedItemCount(int count) {
		synchronized (monitor) {
			processedItemCount += count;
		}
	}

	public long getProcessedItemCount() {
		return processedItemCount;
	}

	public boolean stopRun() {
		boolean isScheduledPause = !src.isCrawlAllowedBySchedule();
		return isPauseRequested() || isStopRequested() || isScheduledPause;
	}

	private boolean isPauseRequested() {
		return ISource.CRAWL_PROCESS_STATUS_PAUSE_REQUESTED.equals(getCrawlProcessStatus());
	}

	private boolean isStopRequested() {
		return ISource.CRAWL_PROCESS_STATUS_STOP_REQUESTED.equals(getCrawlProcessStatus()) || crawlerController.stopRequested();
	}

	private String getCrawlProcessStatus() {
		return crawlerController.getCrawlerDB().getSourceCrawlProcessStatus(src.getId());
	}

	/**
	 * Called by a thread to tell the controller that it is about to stop.
	 * The threadId is handed over to the MessageReceiver.
	 */
	private void finished() {

		//if (!src.isResetFromCache()) Utils.sleep(5000);
		logger.log("Finish crawling for source : " + src.getId());
		logger.log("    queue size : " + String.valueOf(queueItems.getQueueSize()));

		//connector.updateSource(false, false);
		crawlerController.getCrawlerDB().updateSourceStatusStop(src, queueItems.getQueueSize(), queueItems.getDoneQueueSize(), isStopRequested(), false, false, config);

		//			if (!receiver.stopRequested()) {
		//			    tasks.clear();
		//			}
		//			
		//			tasks.close();
		//			receiver.finished(threadId);

	}

	/**
	 * Called by a thread to tell the controller that it is about to stop.
	 * The threadId is handed over to the MessageReceiver.
	 */
	private void paused(boolean bySchedule) {
		logger.log("Pause crawling for source : " + src.getId());
		logger.log("    queue size : " + String.valueOf(queueItems.getQueueSize()));
		
		//connector.updateSource(true, bySchedule);
		crawlerController.getCrawlerDB().updateSourceStatusStop(src, queueItems.getQueueSize(), queueItems.getDoneQueueSize(), false, true, bySchedule, config);
		
		//			receiver.finished(threadId);
	}
}
