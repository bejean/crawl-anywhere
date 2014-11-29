package fr.eolya.crawler.connectors;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;

abstract public class Source {

	protected Map<String,Object> srcData;

	protected String className;
	protected int id;
	protected XMLConfig params;
	protected XMLConfig extra;
	protected XMLConfig processingInfo;
	protected String crawlMode;
	protected Logger logger;
	protected StringBuffer memLog;
	protected int depth;

	protected long processingElapsedTime;
	protected long processingLastTime;
	protected long processingLastProcessedPageCount;

	public Source (int id, String className, String crawlMode, Map<String,Object> srcData) throws IOException {

		this.srcData = srcData;
		this.memLog = new StringBuffer();

		params = new XMLConfig();
		params.loadString((String)srcData.get("params"));

		extra = new XMLConfig();
		extra.loadString((String)srcData.get("extra"));

		this.id = id;
		this.className = className;
		this.crawlMode = crawlMode;     

		this.depth = getSrcDataInt("crawl_maxdepth", 0);

		processingElapsedTime = 0;
		processingLastTime = new Date().getTime();
		processingLastProcessedPageCount = 0;
		processingInfo = new XMLConfig();
		processingInfo.loadString((String)srcData.get("processing_info"));
		if (processingInfo!=null) {
			String elapsedTime = processingInfo.getProperty("/infos/elapsedtime");
			if (elapsedTime!=null && Utils.isStringNumeric(elapsedTime))
				processingElapsedTime = Long.parseLong(processingInfo.getProperty("/infos/elapsedtime"));
		}
	}

	public String getClassName() {
		return className;
	}

	public long getProcessingElapsedTime() {
		return processingElapsedTime;
	}

	public void setProcessingElapsedTime(long processingElapsedTime) {
		this.processingElapsedTime = processingElapsedTime;
	}

	public long getProcessingLastTime() {
		return processingLastTime;
	}

	public void setProcessingLastTime(long processingLastTime) {
		this.processingLastTime = processingLastTime;
	}

	public long getProcessingLastProcessedPageCount() {
		return processingLastProcessedPageCount;
	}

	public void setProcessingLastProcessedPageCount(long processingLastProcessedPageCount) {
		this.processingLastProcessedPageCount = processingLastProcessedPageCount;
	}

//	public void setParam(String name, String value) {
//		params.setProperty("/params", name, value);
//	}

//	public String getParamsAsXml() {
//		return params.asXml();
//	}

	public int getDepth() {
		return depth;
	}

	protected Date getSrcDataDate(String name) {
		if (srcData.containsKey(name)) return (Date)srcData.get(name);
		return null;
	}

//	protected int getSrcDataInt(String name) {
//		if (srcData.containsKey(name)) {
//			Object o = srcData.get(name);
//			try {
//				if (o instanceof String) {  
//					return Integer.parseInt((String) o);
//				} 
//				if (o instanceof Integer) {  
//					return ((Integer) o).intValue();
//				}
//				if (o instanceof Long) {  
//					return ((Long) o).intValue();
//				}
//				return 0;
//			} 
//			catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		return 0;
//	}
	
	
	protected int getSrcDataInt(String name) throws IOException {
		if (srcData.containsKey(name)) {
			Object o = srcData.get(name);
			if (o instanceof String) { 
				if ("".equals((String) o)) {
					throw new IOException();
				}
				return Integer.parseInt((String) o);
			} 
			if (o instanceof Integer) {  
				return ((Integer) o).intValue();
			}
			if (o instanceof Long) {  
				return ((Long) o).intValue();
			}
		}
		throw new IOException();
	}


	protected int getSrcDataInt(String name, int defaultValue) {
		try {
			return getSrcDataInt(name);			
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	protected boolean getSrcDataBoolean(String name) {
		return "1".equals(getSrcDataString(name));
	}

	protected String getSrcDataString(String name) {
		if (srcData.containsKey(name)) return (String)srcData.get(name);
		return params.getProperty("/params/" + name);
	}

	protected String getSrcDataString(String name, String defaultValue) {
		String value = getSrcDataString(name);
		if (value==null || "".equals(value)) return defaultValue;
		return value;
	}

	protected String getSrcDataStringAsXml(String name) {
		if (srcData.containsKey(name)) return (String)srcData.get(name);
		return params.getPropertyAsXml("/params/" + name);
	}

	protected String getSrcDataStringAsXml(String name, String defaultValue) {
		String value = getSrcDataStringAsXml(name);
		if (value==null || "".equals(value)) return defaultValue;
		return value;
	}

	public void memLogAppend (String message) {
		if (memLog.length()>32768) return;
		memLog.append(message + "\n");
		if (memLog.length()>32765) memLog.append(message + "...");
	}

	public String memLogGet () {
		return memLog.toString();
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public boolean isFirstCrawlCompleted() {
		return getSrcDataBoolean("crawl_firstcompleted") && !this.isReset();
	}

	public void setFirstCrawlCompleted(boolean firstCrawlCompleted) {
		if (firstCrawlCompleted)
			srcData.put("crawl_firstcompleted", "1");
		else
			srcData.put("crawl_firstcompleted", "0");
	}

	public boolean isDeleted() {
		return getSrcDataBoolean("deleted");
	}

	public boolean isDisabled() {
		return getSrcDataBoolean("disabled");
	}

	public int getId() {
		return id;
	}

	public int getAccountId(){
		return getSrcDataInt("id_account", 1);
	}

	public int getTargetId() {
		return getSrcDataInt("id_target", 1);
	}

	public boolean isRescan() {
		return ISource.CRAWL_PROCESS_MODE_RESCAN.equals(crawlMode);
	}

	public boolean isReset() {
		return ISource.CRAWL_PROCESS_MODE_RESET.equals(crawlMode);
	}

	public boolean isDeeper() {
		return ISource.CRAWL_PROCESS_MODE_DEEPER.equals(crawlMode);
	}

	public boolean isRescanFromCache() {
		return ISource.CRAWL_PROCESS_MODE_RESCAN_FROM_CACHE.equals(crawlMode);
	}

	public boolean isClear() {
		return ISource.CRAWL_PROCESS_MODE_CLEAR.equals(crawlMode);
	}

//	public boolean isCheckForDeletion() {
//		return false;
////		return "6".equals(crawlMode);
//	}

	public int getType(){
		return getSrcDataInt("type", 1);
	}

	public boolean isTest(){
		return ("2".equals(getSrcDataString("enabled")));
	}

	public String getName(){
		return getSrcDataString("name");
	}

	private Date dateStringToDate(String date) {
		if ("".equals(date) || date==null) return null;

		if (StringUtils.isNumeric(date)) {
			Date d = new Date();
			d.setTime(Long.parseLong(date));
			return d;
		} else {
			DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			try {
				return sdf.parse(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;		
	}

	public Date getCrawlLastTimeStart() {
		return dateStringToDate(getSrcDataString("crawl_lasttime_start"));
	}

	public Date getCrawlLastTimeEnd() {
		return dateStringToDate(getSrcDataString("crawl_lasttime_end"));
	}

	public Date getCrawlLastTimeCheckForDeletion(){
		return dateStringToDate(getSrcDataString("crawl_lasttime_checkfordeletion"));
	}

	public String getLanguage() {
		return getSrcDataString("language");
	}

	public String getLanguageDetectionList() {
		return getSrcDataString("language_detection_list");
	}

	public String getCountry() {
		return getSrcDataString("country");
	}

	public String getTags() {
		String tag = getSrcDataString("tag");
		if (tag!=null) tag = tag.replaceAll("_", " ");
		return tag;
	}

	public String getCollections() {
		String collection = getSrcDataString("collection");
		if (collection!=null) collection = collection.replaceAll("_", " ");
		return collection;	
	}

	public String getComment() {
		return getSrcDataString("comment");
	}

	public String getContact(){
		return getSrcDataString("contact");
	}

	public String getChildOnly() {
		return getSrcDataString("crawl_childonly");
	}

	public String getUrlConcurrency() {
		if (!"0".equals(getUrlPerMinute())) return "1";
		return getSrcDataString("crawl_url_concurrency", "1");
	}

	public String getUrlPerMinute() {
		return getSrcDataString("crawl_url_per_minute", "0");
	}

	public String getFilteringRules() {
		return getSrcDataStringAsXml("crawl_filtering_rules", "");
	}

	public void setExtra(XMLConfig extra) {
		this.extra = extra;
	}

	public XMLConfig getExtra() {
		return extra;
	}

	public int getAuthMode() {
		return getSrcDataInt("auth_mode", 0);
	}

	public String getAuthLogin() {
		return getSrcDataString("auth_login");
	}

	public String getAuthPasswd() {
		return getSrcDataString("auth_passwd");
	}

	public String getAuthParam(){
		return getSrcDataString("auth_param");
	}

	public Date getCurrentCrawlNextTime() {
		//if (getSrcDataDate("crawl_nexttime")==null || "".equals(getSrcDataString("crawl_nexttime"))) return "0";
		return getSrcDataDate("crawl_nexttime");
	}

	public String getCrawlPeriod() {
		return getSrcDataString("crawl_minimal_period");
	}

	public String getMetadata() {
		return getSrcDataString("metadata");
	}

	public boolean isCrawlAllowedBySchedule() {

		if (getSrcDataStringAsXml("crawl_schedule")==null || "".equals(getSrcDataStringAsXml("crawl_schedule"))) return true;

		Document document = null;
		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		try {
			document = reader.read(new StringReader(getSrcDataStringAsXml("crawl_schedule")));
		} catch (DocumentException e) {
			return true;
		}

		@SuppressWarnings("unchecked")
		List<Node> nodes = document.selectNodes("//schedules/schedule");
		if (nodes == null)
			return true;

		int countEnabledSchedule = 0;

		for (int i=0; i<nodes.size(); i++) {
			String day = nodes.get(i).selectSingleNode("day").getText();
			int start = Integer.parseInt(nodes.get(i).selectSingleNode("start").getText());
			int stop = Integer.parseInt(nodes.get(i).selectSingleNode("stop").getText());
			String enabled = nodes.get(i).selectSingleNode("enabled").getText();

			if (!"true".equals(enabled)) continue;
			countEnabledSchedule++;

			if (!"all".equals(day) && !Utils.getCurrentDayName().equals(day)) continue;
			int currentHour = Utils.getCurrentHour();
			if (currentHour>=start && currentHour < stop) return true;
		}

		if (countEnabledSchedule==0)
			return true;
		else {
			if (logger != null) logger.log("Crawl not allowed by schedule rule for source : " + getName() + "(" + this.id + ")");
			return false;
		}
	}



}
