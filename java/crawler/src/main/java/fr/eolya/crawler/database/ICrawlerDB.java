package fr.eolya.crawler.database;

import java.util.HashMap;

import fr.eolya.crawler.connectors.ISource;
import fr.eolya.utils.XMLConfig;

public interface ICrawlerDB {

	public String getVersion();
	public String getSourceClass(String sourceType);
	public HashMap<String,String> getTarget(String targetId);
	public void close();
	public void fixStartupSourcesStatus();	
	public boolean updateSourceStatusStartup(int id, long queueSize, long doneQueueSize);
	public boolean updateSourceStatusStop(ISource src, long queueSize, long doneQueueSize, boolean crawlerStopRequested, boolean pause, boolean pauseBySchedule, XMLConfig config);
	public String getSourceCrawlProcessStatus(int id);
	public boolean updateSourceProcessingInfo(int id, long queueSize, long doneQueueSize, String processingInfo);
	public boolean updateSourceLog(ISource src, int retention);
}
