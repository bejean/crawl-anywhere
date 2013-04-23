package fr.eolya.crawler.connectors.web;

import java.util.Map;

public class StartingUrl {
	public String url;
	public String mode;
	public boolean onlyFirstCrawl;
	public boolean allowOtherDomain;

	public StartingUrl (String url, String mode,boolean onlyFirstCrawl, boolean allowOtherDomain) {
		this.url = url;
		this.mode = mode;
		this.onlyFirstCrawl = onlyFirstCrawl;
		this.allowOtherDomain = allowOtherDomain;
	}

	private SourceItemWeb getItemWeb(int sourceId, String url) {
		return new SourceItemWeb(sourceId, url, 0, "", url, "", "", null, null, "", "", mode, "", "", "", "", mode, allowOtherDomain);		
		/*	
		 public SourceItemWeb(int sourceId, String url, int depth, String url_referrer, String url_start, String contentType, 
		 String refererCharSet, Date crawlLastTime, Date publishedDate, String crawlStatus, String crawlMode, String condget_eTag, String condget_lastModified, 
		 String extra1, String extra2, String rootUrlMode, Boolean allowOtherDomain) {
		 */
	}

	public Map<String,Object> getMap(int id) {
		return getItemWeb(id, url.replaceAll("\"", "\\\"")).getMap();
	}
}
