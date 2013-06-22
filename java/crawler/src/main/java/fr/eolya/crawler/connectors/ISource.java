package fr.eolya.crawler.connectors;

import java.util.Date;

import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;

public interface ISource {

	public static String CRAWL_PROCESS_STATUS_NONE = "0";
	public static String CRAWL_PROCESS_STATUS_CRAWLING = "1";
	public static String CRAWL_PROCESS_STATUS_PAUSE_REQUESTED = "2";
	public static String CRAWL_PROCESS_STATUS_RESUME_REQUESTED = "3";
	public static String CRAWL_PROCESS_STATUS_PAUSE_BY_SCHEDULE = "4";
	public static String CRAWL_PROCESS_STATUS_STOP_REQUESTED = "5";

	public static String CRAWL_PROCESS_MODE_NONE = "0";
	public static String CRAWL_PROCESS_MODE_RESCAN = "1";
	public static String CRAWL_PROCESS_MODE_RESET = "2";
	public static String CRAWL_PROCESS_MODE_DEEPER = "3";
	public static String CRAWL_PROCESS_MODE_RESCAN_FROM_CACHE = "4";
	public static String CRAWL_PROCESS_MODE_CLEAR = "5";
	
	// !!!!!!!!!!!!!!!!!!!
	//public static String CRAWL_PROCESS_MODE_CHECK_FOR_DELETION = "7";
	//public static String CRAWL_PROCESS_MODE_CLEAN = "6";
	// !!!!!!!!!!!!!!!!!!!
	
	public void memLogAppend (String message) ;
	public StringBuffer memLogGet () ;
	public void setLogger(Logger logger) ;
	public boolean isFirstCrawlCompleted();
	public void setFirstCrawlCompleted(boolean firstCrawlCompleted);
	public int getDepth();
	public int getId() ;
	public int getAccountId();
	public int getTargetId();
    public boolean isDeleted();
    public boolean isDisabled();
	public boolean isRescan();
    //public boolean isCheckForDeletion();
	public boolean isReset();
	public boolean isDeeper();
    public boolean isClear();
	public boolean isRescanFromCache();
	public int getType();
	public boolean isTest();
	public String getName();
	public String getLanguage();
	public String getLanguageDetectionList();
	public String getCountry();
	public String getTags();
	public String getCollections();
	public String getComment();
	public String getContact();
	public String getUrlConcurrency();
	public String getUrlPerMinute();
	public String getFilteringRules();
    public String getMetadata();
	public void setExtra(XMLConfig extra);
	public XMLConfig getExtra();
	public int getAuthMode();
	public String getAuthLogin();
	public String getAuthPasswd();
	public String getAuthParam();
	public Date getCurrentCrawlNextTime();
	public String getCrawlPeriod();
	public boolean isCrawlAllowedBySchedule();
	public String getDescription();
	
	public String getParamsAsXml();

    public long getProcessingElapsedTime();
    public void setProcessingElapsedTime(long processingElapsedTime);
    public long getProcessingLastTime();
    public void setProcessingLastTime(long processingLastTime);
    public long getProcessingLastProcessedPageCount();
    public void setProcessingLastProcessedPageCount(long processingLastProcessedPageCount);
    
    public String getClassName();
    
	public boolean isOptimized();
}
