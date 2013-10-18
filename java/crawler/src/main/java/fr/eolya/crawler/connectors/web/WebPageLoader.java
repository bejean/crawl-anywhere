package fr.eolya.crawler.connectors.web;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.eolya.crawler.cache.DocumentCacheFactory;
import fr.eolya.crawler.cache.DocumentCacheItem;
import fr.eolya.crawler.cache.IDocumentCache;
import fr.eolya.utils.http.HttpLoader;
import fr.eolya.utils.http.HttpStream;
import fr.eolya.utils.nosql.IDBConnection;

public class WebPageLoader {

	public static final int LOAD_ERROR = -1;
	public static final int LOAD_SUCCESS = 0;
	public static final int LOAD_PAGEUNCHANGED = 1;
	public static final int LOAD_PAGEREDIRECTED = 2;

	public static final int CACHE_NONE = 0;
	//public static final int CACHE_FIRST = 1;
	public static final int CACHE_ONLY = 2;

	private HttpLoader httpLoader = null;
	private HttpStream ws = null;
	private String contentType = null;
	
	private IDocumentCache cache = null;
	private DocumentCacheItem cacheItem = null;

	private int cacheMode = CACHE_NONE; 

	public WebPageLoader(int cacheMode, String type, IDBConnection con, String dbName, String dbCollName, String sourceId) {
		this.cacheMode = cacheMode;
		if (this.cacheMode!=CACHE_ONLY) httpLoader = new HttpLoader(); 
		if (this.cacheMode!=CACHE_NONE) cache = DocumentCacheFactory.getDocumentCacheInstance(type, con, dbName, dbCollName, sourceId);
		cacheItem = null;
	}

	public WebPageLoader() {
		this(CACHE_NONE, null, null, null, null, null);
	}

	public void setSimulateHttps(boolean simulate) {
		if (httpLoader!=null) httpLoader.setSimulateHttps(simulate);
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void setUserAgent(String userAgent) {
		if (httpLoader!=null) httpLoader.setUserAgent(userAgent);
	}

	public void setCookies(Map<String, String> cookies) {
		if (httpLoader!=null) httpLoader.setCookies(cookies);
	}

	public void setBasicLogin(Map<String, String> authBasicLogin) {
		if (httpLoader!=null) httpLoader.setBasicLogin(authBasicLogin);
	}

	public int getHeadStatusCode(String url) {
		if (httpLoader!=null) return httpLoader.getHeadStatusCode(url);
		if (cache!=null) {
			if (cacheItem!=null) return 200;
			if (cache.contains(url)) return 200;
			return 404;
		}
		return 0;
	}

	public int openRetry(String url, int maxRetry) {
		if (httpLoader!=null) return httpLoader.openRetry(url, maxRetry);
		if (cache!=null) {
			try {
				cacheItem = cache.get(url);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (cacheItem!=null) return LOAD_SUCCESS;
		}
		return LOAD_ERROR;
	}

	public void close() {
		if (ws!=null) ws.clear();
		if (httpLoader!=null) httpLoader.close();
	}

	public String getContentType() {
		if (contentType!=null) return contentType;
		if (httpLoader!=null) return httpLoader.getContentType();
		if (cache!=null && cacheItem!=null) return cacheItem.params.get("contentType");
		return "";
	}

	public String getContentEncoding() { 
		if (httpLoader!=null) return httpLoader.getContentEncoding();
		if (cache!=null && cacheItem!=null) return cacheItem.params.get("contentCharSet");
		return "";
	}

	public int getContentLength() {
		if (httpLoader!=null) return httpLoader.getContentLength();
		if (cache!=null && cacheItem!=null) {
			if ("".equals(StringUtils.trimToEmpty(cacheItem.params.get("contentSize")))) return 0;
			return Integer.parseInt(cacheItem.params.get("contentSize"));	
		}
		return 0;
	}

	public InputStream getStream() {
		if (httpLoader!=null) return httpLoader.getStream();
		if (cache!=null && cacheItem!=null) return cacheItem.streamData;
		return null;
	}

	private HttpStream initHttpStream() {
		if (ws==null) {
			if (httpLoader!=null) ws = new HttpStream(httpLoader.getStream(), "", getContentType(), httpLoader.getContentEncoding());
			if (cache!=null && cacheItem!=null) ws = new HttpStream(cacheItem.streamData, "", getContentType(), "");
		}
		return ws;
	}
	
	public String getString() {
		if (initHttpStream()!=null) return ws.getString();
		return null;
	}

	public String getCharSet() {
		if (initHttpStream()!=null) return ws.getCharSet();
		return null;
	}

	public String getDeclaredLanguage() {
		if (initHttpStream()!=null) return ws.getDeclaredLanguage();
		return null;
	}

	public int getErrorCode() {
		if (httpLoader!=null) return httpLoader.getErrorCode();
		if (cache!=null && cacheItem!=null) return LOAD_SUCCESS;
		return LOAD_ERROR;
	}

	public String getErrorMessage() {
		if (httpLoader!=null) return httpLoader.getErrorMessage();
		if (cache!=null && cacheItem!=null) return "";
		return "";
	}

	public int getResponseStatusCode() {
		if (httpLoader!=null) return httpLoader.getResponseStatusCode();
		if (cache!=null && cacheItem!=null) return 200;
		return 404;
	}

	public String getResponseReasonPhrase() {
		if (httpLoader!=null) return httpLoader.getResponseReasonPhrase();
		if (cache!=null && cacheItem!=null) return "";
		return "Not Found";
	}
	
	public String getRedirectionLocation() {
		if (httpLoader!=null) return httpLoader.getRedirectionLocation();
		return "";
	}

	public String getCondGetETag() {
		if (httpLoader!=null) return httpLoader.getCondGetETag();
		return "";
	}	

	public String getCondGetLastModified() {
		if (httpLoader!=null) return httpLoader.getCondGetLastModified();
		return "";
	}	
	
	public static boolean isHtmlOrText(String contentType) {
		return HttpLoader.isHtmlOrText(contentType);
	}
	public static boolean isRss(String contentType, String rawPage) {
		return HttpLoader.isRss(contentType, rawPage);
	}
    public static boolean isFeed(String rawPage) {
		return HttpLoader.isFeed(rawPage);
    }
    public static boolean isHtml(String contentType) {
		return HttpLoader.isHtml(contentType);
    }

}
