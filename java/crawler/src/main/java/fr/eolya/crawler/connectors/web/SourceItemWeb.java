package fr.eolya.crawler.connectors.web;

import fr.eolya.crawler.connectors.ISourceItem;
import fr.eolya.crawler.connectors.SourceItem;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class SourceItemWeb extends SourceItem implements ISourceItem {

	private static String ID_SOURCE = "id_source";
	private static String URL = "url";
	private static String REFERRER_URL = "referrer_url";
	private static String REFERRER_CHARSET = "referrer_charset";
	private static String URL_START = "url_start";
	private static String DEPTH = "depth";
	private static String CONTENT_TYPE = "content_type";
	private static String CRAWL_LAST_TIME= "crawl_last_time";
	private static String CRAWL_STATUS = "crawl_status";
	private static String CRAWL_STATUS_MESSAGE = "crawl_status_message";
	private static String CRAWL_MODE = "crawl_mode";
	private static String CONDGET_ETAG = "condget_etag";
	private static String CONDGET_LAST_MODIFIED = "condget_last_modified";
	private static String EXTRA1 = "extra1";
	private static String EXTRA2 = "extra2";
	private static String ROOT_URL_MODE = "root_url_mode";
	private static String ALLOW_OTHER_DOMAIN = "allow_other_domain";
	private static String PUBLISHED_TIME = "published_time";
	private static String REDIRECTION_COUNT= "redirection_count";

	
	public static SourceItemWeb newInstance(Map<String,Object> itemData)  
	{  
		if (itemData==null) return null;
		return new SourceItemWeb(itemData);
	}  
	 
	public SourceItemWeb(Map<String,Object> itemData) {
		this.itemData = itemData;
	}

	public SourceItemWeb(int sourceId, String url, int depth, String url_referrer, String url_start, String contentType, String refererCharSet, Date crawlLastTime, Date publishedDate, String crawlStatus, String crawlStatusMessage, String crawlMode, String condget_eTag, String condget_lastModified, String extra1, String extra2, String rootUrlMode, Boolean allowOtherDomain) {

		itemData = new HashMap<String,Object>();

		itemData.put(ID_SOURCE, new Integer(sourceId));
		itemData.put(URL, url);
		itemData.put(DEPTH, new Integer(depth));
		itemData.put(REFERRER_URL, url_referrer);
		itemData.put(REFERRER_CHARSET, refererCharSet);
		itemData.put(URL_START, url_start);
		itemData.put(CONTENT_TYPE, contentType);
		itemData.put(CRAWL_LAST_TIME, crawlLastTime);
		itemData.put(PUBLISHED_TIME, publishedDate);
		itemData.put(CRAWL_STATUS, crawlStatus);
		itemData.put(CRAWL_STATUS_MESSAGE, crawlStatusMessage);
		itemData.put(CRAWL_MODE, crawlMode);
		itemData.put(CONDGET_ETAG, condget_eTag);
		itemData.put(CONDGET_LAST_MODIFIED, condget_lastModified);
		itemData.put(EXTRA1, extra1);
		itemData.put(EXTRA2, extra2);
		itemData.put(ROOT_URL_MODE, rootUrlMode);
		itemData.put(ALLOW_OTHER_DOMAIN, allowOtherDomain);
		itemData.put(REDIRECTION_COUNT, 0);
	}
	
	private void setInt(String attribute, int value) {
		itemData.put(attribute, new Integer(value).intValue());
	}

	private void setString(String attribute, String value) {
		itemData.put(attribute, value);
	}

	private void setDate(String attribute, Date value) {
		itemData.put(attribute, value);
	}

	private void setBoolean(String attribute, boolean value) {
		itemData.put(attribute, new Boolean(value).booleanValue());
	}

	private int getInt(String attribute) {
		return ((Integer)itemData.get(attribute)).intValue();
	}

	private String getString(String attribute) {
		return (String)itemData.get(attribute);
	}

	private Date getDate(String attribute) {
		return (Date)itemData.get(attribute);
	}

	private boolean getBoolean(String attribute) {
		return ((Boolean)itemData.get(attribute)).booleanValue();
	}

	public int getDepth() {
		try {
		return getInt(DEPTH);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	public void setDepth(int depth) {
		setInt(DEPTH, depth);
	}

	public String getUrlStart() {
		return getString(URL_START);
	}

	public void setUrlStart(String urlStart) {
		setString(URL_START, urlStart);
	}

	public boolean isAllowOtherDomain() {
		return getBoolean(ALLOW_OTHER_DOMAIN);
	}

	public void setAllowOtherDomain(boolean allowOtherDomain) {
		setBoolean(ALLOW_OTHER_DOMAIN, allowOtherDomain);
	}

	public String getRootUrlMode() {
		return getString(ROOT_URL_MODE);
	}

	public void setRootUrlMode(String rootUrlMode) {
		setString(ROOT_URL_MODE, rootUrlMode);
	}

	public String getUrl() {
		String tempUrl = getString(URL).toLowerCase().trim();		
		if (!tempUrl.startsWith("http://") && !tempUrl.startsWith("https://") && !tempUrl.startsWith("ftp://")) return "http://" + getString(URL).trim();
		return getString(URL);
	}

	public String getHost() {
		URL url;
		try {
			url = new URL(getString(URL));
			return url.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getReferrer() {
		if (getString(REFERRER_URL)==null || "".equals(getString(REFERRER_URL))) return "";
		String tempUrl = getString(REFERRER_URL).toLowerCase().trim();		
		if (!tempUrl.startsWith("http://") && !tempUrl.startsWith("https://") && !tempUrl.startsWith("ftp://")) return "http://" + getString(REFERRER_URL).trim();
		return getString(REFERRER_URL);
	}	

	public int getSourceId() {
		return getInt(ID_SOURCE);
	}

	public String getContentType() {
		return StringUtils.trimToEmpty(getString(CONTENT_TYPE));
	}	

	public String getRefererCharSet() {
		return StringUtils.trimToEmpty(getString(REFERRER_CHARSET));
	}	

	public String getCondgetETag() {
		return StringUtils.trimToEmpty(getString(CONDGET_ETAG));
	}	

	public String getCondgetLastModified() {
		return StringUtils.trimToEmpty(getString(CONDGET_LAST_MODIFIED));
	}	

	public void setCondgetETag(String condget_eTag) {
		setString(CONDGET_ETAG, condget_eTag);
	}	

	public void setCondgetLastModified(String condget_lastModified) {
		setString(CONDGET_LAST_MODIFIED, condget_lastModified);
	}	

	public Date getCrawlLastTime() {
		if (getDate(CRAWL_LAST_TIME)==null) return new java.util.Date();
		return getDate(CRAWL_LAST_TIME);
	}	

	public int getCrawlStatus() {
		return getInt(CRAWL_STATUS);
	}	

	public String getCrawlMode() {
		return StringUtils.trimToEmpty(getString(CRAWL_MODE));
	}   

	public String getCrawlStatusMessage() {
		return StringUtils.trimToEmpty(getString(CRAWL_STATUS_MESSAGE));
	}	

	public void setReferrer(String url_referrer) {
		setString(REFERRER_URL, url_referrer);
	}

	public void setContentType(String contentType) {
		setString(CONTENT_TYPE, contentType);
	}

	public void setCrawlLastTime(Date crawlLastTime) {
		setDate(CRAWL_LAST_TIME, crawlLastTime);
	}

	public void setCrawlStatus(int crawlStatus) {
		setInt(CRAWL_STATUS, crawlStatus);
	}

	public void setCrawlStatusMessage(String crawlStatusMessage) {
		setString(CRAWL_STATUS_MESSAGE, crawlStatusMessage);
	}

	public void setCrawlMode(String crawlMode) {
		setString(CRAWL_MODE, crawlMode);
	}

	public void setExtra1(String value) {
		setString(EXTRA1, value);
	}	

	public String getExtra1() {
		return getString("EXTRA1");
	}	

	public void setExtra2(String value) {
		setString("EXTRA2", value);
	}	

	public String getExtra2() {
		return getString("EXTRA2");
	}	

	public int getRedirectionCount() {
		return getInt(REDIRECTION_COUNT);
	}

	public void setRedirectionCount(int redirectionCount) {
		setInt(REDIRECTION_COUNT, redirectionCount);
	}

	public Map<String,Object> getMap() {
		return this.itemData;
	}


	//	public String toJson() {
	//
	//		String allowOtherDomain = "false";
	//		if (isAllowOtherDomain()) allowOtherDomain = "true";
	//
	//		//SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss"); 
	//		//String crawlLastTime = formatter.format(getCrawlLastTime()); 
	//		
	//		//String publishedDate = "";
	//		//if (getPublishedDate()!=null) publishedDate = formatter.format(getPublishedDate()); 
	//
	//		Map<String,Object> doc = new HashMap<String,Object>();
	//		doc.put("sourceId", getSourceId());
	//		doc.put("url", getUrl());
	//        doc.put("url_lc", getUrl().toLowerCase());
	//		doc.put("depth", getDepth());
	//		doc.put("url_referrer", getReferrer());
	//		doc.put("url_start", getUrlStart());
	//		doc.put("contentType", getContentType());
	//		doc.put("refererCharSet", getRefererCharSet());
	//		doc.put("crawlLastTime", getCrawlLastTime());
	//		doc.put("publishedDate", getPublishedDate());
	//		doc.put("crawlStatus", getCrawlStatus());
	//        doc.put("crawlMode", getCrawlMode());
	//		doc.put("condget_eTag", getCondgetETag());
	//		doc.put("condget_lastModified", getCondgetLastModified());
	//		doc.put("extra1", getExtra1());
	//		doc.put("extra2", getExtra2());
	//		doc.put("rootUrlMode", getRootUrlMode());
	//		doc.put("allowOtherDomain", allowOtherDomain);
	//		
	//		ObjectMapper mapper = new ObjectMapper();
	//		
	//		try {
	//			return mapper.writeValueAsString(doc);
	//		} catch (JsonProcessingException e) {
	//			e.printStackTrace();
	//			return null;
	//		}
	//	}
	//
	//	public static SourceTasksQueueItemWeb fromJson (String json) {
	//		
	//		if (json==null) return null;
	//
	//		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	//
	//		SourceTasksQueueItemWeb item;
	//		try {
	//			Map<String,Object> doc = JSONHelper.getJSONMapString(json);
	//
	//			Date crawlLastTime = null;
	//			Date publishedDate = null;
	//			try {
	//				String date = StringUtils.trimToEmpty(doc.get("crawlLastTime"));
	//				if (!"".equals(date)) crawlLastTime = formatter.parse(date);
	//				date = StringUtils.trimToEmpty(doc.get("publishedDate"));
	//				if (!"".equals(date)) publishedDate = formatter.parse(date);
	//			} catch (ParseException e) {
	//				e.printStackTrace();
	//			}
	//			item = new SourceTasksQueueItemWeb(
	//					((Integer)doc.get("sourceId")).intValue(),
	//					(String)doc.get("url"), 
	//					((Integer)doc.get("depth")).intValue(),
	//					doc.get("url_referrer"), 
	//					doc.get("url_start"), 
	//					doc.get("contentType"), 
	//					doc.get("refererCharSet"), 
	//					crawlLastTime, 
	//					publishedDate,
	//					doc.get("crawlStatus"), 
	//                    doc.get("crawlMode"), 
	//					doc.get("condget_eTag"), 
	//					doc.get("condget_lastModified"), 
	//					doc.get("extra1"), 
	//					doc.get("extra2"), 
	//					doc.get("rootUrlMode"), 
	//					"true".equals(doc.get("allowOtherDomain"))
	//					);
	//		} catch (NumberFormatException e) {
	//			e.printStackTrace();
	//			return null;
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//			return null;
	//		}
	//
	//		return item;
	//	}


	public Date getPublishedDate() {
		return null;
	}

}
