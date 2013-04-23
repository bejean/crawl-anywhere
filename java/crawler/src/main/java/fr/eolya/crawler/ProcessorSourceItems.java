package fr.eolya.crawler;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;

public class ProcessorSourceItems extends Processor implements Callable<Integer> {

	private IConnector connector;
	private ISourceControler sourceController;
	private ISourceItemsQueue queueItems;
	private long lastActivityTimeStamp;
	private int id;

	public ProcessorSourceItems(ISourceItemsQueue queueItems, IConnector connector, XMLConfig config, Logger logger, ICrawlerController crawlerController, ISourceControler sourceController, int id) {
		super(config, logger, crawlerController);
		this.sourceController = sourceController;
		this.connector = connector;
		this.queueItems = queueItems;
		this.id = id;
		setLastActivityTimeStamp();
	}

	public Integer call() throws Exception {
		// pop new urls from the queue until queue is empty
		Map<String,Object> itemData = null;
		for (itemData = queueItems.pop(); itemData != null && !sourceController.stopRun(); itemData = queueItems.pop()) {
			// Process the item
			process(itemData);
		}
		Utils.sleep(10000);
		return 1;
	}

	private void process(Map<String,Object> itemData) {
		try {
			int count = connector.processItem(itemData, getCurrentThreadId());
			sourceController.incrementProcessedItemCount(count);
			if (id==0) {
				connector.incrProcessedItemCount(count);
				setLastActivityTimeStamp();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long getCurrentThreadId() {
		return Thread.currentThread().getId();
	}
	
	public synchronized void setLastActivityTimeStamp() {
		lastActivityTimeStamp = new Date().getTime();
	}

}
