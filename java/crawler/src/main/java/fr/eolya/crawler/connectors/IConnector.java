package fr.eolya.crawler.connectors;

import java.util.Map;

import fr.eolya.crawler.ICrawlerController;
import fr.eolya.crawler.cache.IDocumentCache;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;

public interface IConnector {
	public int processItem(Map<String,Object> item, long threadId);
    public boolean initialize(Logger logger, XMLConfig config, ISource src, ISourceItemsQueue queue, ICrawlerController controller);
	public void setDocumentCache(IDocumentCache docCache);
	public String getName();
    public int getMaxThreads(int suggestedMaxThreads);
    public void close(boolean crawlerStopRequested, boolean pause, boolean pauseBySchedule);
    public void incrProcessedItemCount(long processedItemCount);
}
