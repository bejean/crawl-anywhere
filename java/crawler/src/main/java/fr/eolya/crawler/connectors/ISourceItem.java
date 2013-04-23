package fr.eolya.crawler.connectors;

import java.util.Date;
import java.util.Map;

public interface ISourceItem{
	public int getDepth();
	public String getUrlStart() ;
	public void setUrlStart(String urlStart);
	public String getUrl();
	public String getHost();
	public int getSourceId();
	public String getContentType();
	public Date getCrawlLastTime();
	public int getCrawlStatus();
	public String getCrawlStatusMessage();
	public void setContentType(String contentType) ;
	public void setCrawlLastTime(Date crawlLastTime);
	public void setCrawlStatus(int crawlStatus);
	public void setCrawlStatusMessage(String crawlStatusMessage);
	public void setExtra1(String value);
	public String getExtra1();
	public void setExtra2(String value);
	public String getExtra2();
	public Date getPublishedDate();
	public Map<String,Object> getMap();
}
