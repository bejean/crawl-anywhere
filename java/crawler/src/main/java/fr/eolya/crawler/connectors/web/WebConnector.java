package fr.eolya.crawler.connectors.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import crawlercommons.sitemaps.UnknownFormatException;
import fr.eolya.crawler.ICrawlerController;
import fr.eolya.crawler.cache.DocumentCacheItem;
import fr.eolya.crawler.connectors.Connector;
import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.crawler.connectors.ISource;
import fr.eolya.crawler.connectors.web.robots.Robots;
import fr.eolya.crawler.database.ICrawlerDB;
import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.crawler.queue.QueueFactory;
import fr.eolya.extraction.tika.TikaWrapper;
import fr.eolya.utils.CrawlerUtilsCommon;
import fr.eolya.utils.GeoLocalisation;
import fr.eolya.utils.Logger;
import fr.eolya.utils.ScriptSnippet;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLConfig;
import fr.eolya.utils.http.HttpUtils;
import fr.eolya.utils.nosql.IDBConnection;

public class WebConnector extends Connector implements IConnector {

	private SourceWeb src;
	private String ignoreUrlFields;
	private String ignoreUrlFieldsNoSessionId;
	private List<String> removeDocHttpStatus = null;

	private Map<String, String> authCookies = null;
	private Map<String, String> authBasicLogin = null;
	
	private long lastPageReadTime = 0;

	private Robots robots = null;

	private String scriptName = ""; 
	private StartingUrls startingUrls = null;

	private final int memlogMaxDepth = 1;

	private static final Object monitor = new Object();

	public boolean initialize(Logger logger, XMLConfig config, ISource src, ISourceItemsQueue queue, ICrawlerController controller) {

		this.src = (SourceWeb) src;
		try {
			if (!initializeInternal(logger, config, src, queue, controller)) return false;
			
			if (this.src.getAuthMode()!=0) {
				if (this.src.getAuthMode()==3) {
					authBasicLogin = new HashMap<String, String>();
					authBasicLogin.put("login",this.src.getAuthLogin());
					authBasicLogin.put("password",this.src.getAuthPasswd());					
				} else {
					authCookies = HttpUtils.getAuthCookies(this.src.getAuthMode(), this.src.getAuthLogin(), this.src.getAuthPasswd(), this.src.getAuthParam(), 
							config.getProperty("/crawler/proxy/param[@name='host']", ""),
							config.getProperty("/crawler/proxy/param[@name='port']", ""),
							config.getProperty("/crawler/proxy/param[@name='exclude']", ""),
							config.getProperty("/crawler/proxy/param[@name='username']", ""),
							config.getProperty("/crawler/proxy/param[@name='password']", ""));
				}
			}
		} 		
		catch (Exception e) {
			logger.logStackTrace(e, true);
			return false;
		}

		ICrawlerDB db = crawlerController.getCrawlerDB();
		if (db.updateSourceStatusStartup(src.getId(), queue.size(), queue.getDoneQueueSize())) {
			if (queue.getDoneQueueSize()>0) {
				if (src.getProcessingElapsedTime()==0) {
					Date d = this.src.getCrawlLastTimeStart();
					Date n = new Date();
					src.setProcessingElapsedTime(n.getTime() - d.getTime());
				}
			} else {
				src.setProcessingElapsedTime(0);
			}
		}

		String removeDoc = config.getProperty("/crawler/param[@name='removedoc_httpstatus']", "");
		if (removeDoc!=null && !"".equals(removeDoc)) removeDocHttpStatus = Arrays.asList(removeDoc.replaceAll("\\s*", "").split(","));

		String urlIgnoreFields = config.getProperty("/crawler/param[@name='ignore_url_fields']", "");
		if (this.src.getUrlIgnoreFields()!=null && !"".equals(this.src.getUrlIgnoreFields())) {
			if (!"".equals(urlIgnoreFields)) urlIgnoreFields += ',';
			urlIgnoreFields += this.src.getUrlIgnoreFields();
		}		
		ignoreUrlFields = urlIgnoreFields;
		ignoreUrlFieldsNoSessionId = this.src.getUrlIgnoreFieldsNoSessionId();

		startingUrls = this.src.getStartingUrls();
		try {
			boolean wildcardsAllowed = ("1".equals(config.getProperty("/crawler/param[@name='robots_wildcard_allowed']", "0")));
			robots = new Robots(new URL(startingUrls.getUrlHome()), null, getUserAgent (config.getProperty("/crawler/param[@name='user_agent']", "CaBot"), this.src.getUserAgent()), wildcardsAllowed);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			robots = null;
		} catch (Exception e2) {
			e2.printStackTrace();
			robots = null;
		} 

		/* TODO : V4 */
		// check geo localisation
		if (("1".equals(config.getProperty("/crawler/param[@name='geo_location']", "0")))) {
			XMLConfig srcExtra = this.src.getExtra();
			String srcIP = "";
			if (srcExtra!=null) {
				srcIP = srcExtra.getProperty("/geoip/ip", "");
			}
			GeoLocalisation geo;
			try {
				URL hostUrl = new URL(startingUrls.getUrlHome());
				geo = new GeoLocalisation (hostUrl.getHost(), srcIP, null);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return false;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			}
			if (geo.resolve("geoiptool") && geo.hasChanged()) {
				srcExtra.setProperty("/", "geoip", "");
				srcExtra.setProperty("/geoip", "ip", geo.getIp());
				srcExtra.setProperty("/geoip", "latitude", geo.getLatitude());
				srcExtra.setProperty("/geoip", "longitude", geo.getLongitude());
				srcExtra.setProperty("/geoip", "countrycode", geo.getCountryCode());
				srcExtra.setProperty("/geoip", "countryname", geo.getCountryName());
				srcExtra.setProperty("/geoip", "area", geo.getArea());
				srcExtra.setProperty("/geoip", "city", geo.getCity());
				this.src.setExtra(srcExtra);
			}
		}
		
		String scriptPath = config.getProperty("/crawler/param[@name='scripts_path']", "");
		scriptPath = Utils.getValidPropertyPath(scriptPath, null, "HOME");
		if (scriptPath!=null && !"".equals(scriptPath)) {
			scriptName = ScriptSnippet.getScriptFilename (scriptPath + "/" + this.src.getAccountId(), startingUrls.getUrlHome());
			if (scriptName==null || "".equals(scriptName))
				scriptName = ScriptSnippet.getScriptFilename (scriptPath, startingUrls.getUrlHome());
		}
		
        if (src.isReset() || src.isClear() || src.isRescan() || src.isRescanFromCache()) {
        	 dh.resetSource(String.valueOf(src.getId()));
        }
		
		return true;
	}
	
	public void close(boolean crawlerStopRequested, boolean pause, boolean pauseBySchedule) {
		ICrawlerDB db = crawlerController.getCrawlerDB();
		db.updateSourceStatusStop(src, queue.getQueueSize(), queue.getDoneQueueSize(), crawlerStopRequested, pause, pauseBySchedule, config);
	}
	
	//public int processItem(String jsonItem, long threadId) {
	public int processItem(Map<String,Object> itemData, long threadId) {

		try {		
			String rawPage = null;
			String contentType = null;
			//String contentEncoding= null;
			String refererCharSet = null;

			boolean follow = true;
			boolean index = true;
			boolean isFeed = false;
			
			boolean simulateHttps = "1".equals(config.getProperty("/crawler/param[@name='simulate_https']", "0"));

			Links links = null;

			//SourceTasksQueueItemWeb currentUrlItem = SourceTasksQueueItemWeb.fromJson(jsonItem);
			SourceItemWeb currentUrlItem = new SourceItemWeb(itemData);

			/*
			 * Normalize url and remove parameters to be ignored
			 * This will avoid duplicate url just due to not used parameters or parameters ordering 
			 */
			String currentNormalizedUrl = HttpUtils.urlNormalize(currentUrlItem.getUrl(), null);
			String currentNormalizedUrl2 = getUrlWithoutIgnoredFields(currentNormalizedUrl);
			if (!currentNormalizedUrl2.equals(currentNormalizedUrl)) {
				logger.log("[" + String.valueOf(threadId) + "] Ignored fields removed = " + currentNormalizedUrl + " -> " + currentNormalizedUrl2);
				currentNormalizedUrl = currentNormalizedUrl2;
			}

			URL pageURL = new URL(currentNormalizedUrl);

			/*
			 * Ignore this page if robots.txt ask it
			 */
			if (robots!=null && !"1".equals(config.getProperty("/crawler/param[@name='bypass_robots_file']", "0")) && !robots.isUrlAllowed(pageURL)) return 0;

			int maxCrawlDepth = src.getDepth();
			if (maxCrawlDepth==0) maxCrawlDepth = Integer.parseInt(config.getProperty("/crawler/param[@name='max_depth']", "2"));
			int level = currentUrlItem.getDepth();

			logger.log("[" + String.valueOf(threadId) + "]Processing page " + pageURL.toExternalForm() + "(" + String.valueOf(level) + "/" + queue.getDoneQueueSize() + "/" + queue.getQueueSize() + ")");
			if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("Processing page : " + pageURL.toExternalForm());

			String urlMode = "a";

			/*
			 * manage protocol conflict according to protocol strategy
			 */
			boolean write = true;
			if (!src.isRescan() && !src.isRescanFromCache()) {
				String alternateUrl = getAlternateProtocolUrl(pageURL.toExternalForm());
				if (alternateUrl!=null) {
					WebPageLoader urlLoader = new WebPageLoader();
					if (simulateHttps) urlLoader.setSimulateHttps(simulateHttps);
					int statusCode = urlLoader.getHeadStatusCode(alternateUrl);
					write = (statusCode!=200);
					if (!write) {
						logger.log("[" + String.valueOf(threadId) + "]     won't write due to duplicate protocol");
						urlMode = "l";
					}
					urlLoader.close();
				}
			}

			WebPageLoader urlLoader = null;
			if (src.isRescanFromCache()) {
				String dbCacheType = config.getProperty("/crawler/cache/param[@name='dbtype']", "");
				String dbCacheName = config.getProperty("/crawler/cache/param[@name='dbname']", "");
				urlLoader = new WebPageLoader(WebPageLoader.CACHE_ONLY, dbCacheType, crawlerController.getDBConnection(false), dbCacheName, "pages_cache", String.valueOf(src.getId()));
			} else {
				if (!"0".equals(src.getUrlPerMinute())) {
					synchronized (monitor) {
						if (lastPageReadTime!=0) {
							long delay = 60000 / Integer.parseInt(src.getUrlPerMinute());
							long n = new Date().getTime();
							if ((n-lastPageReadTime) < delay) {
								Utils.sleep((int)(delay-(n-lastPageReadTime)));
							}
						}
						lastPageReadTime = new Date().getTime();
					}
				}
				urlLoader = new WebPageLoader();
				if (simulateHttps) urlLoader.setSimulateHttps(simulateHttps);
			}
			
			try {
				// load page
				urlLoader.setUserAgent(getUserAgent (config.getProperty("/crawler/param[@name='user_agent']", "CaBot"), this.src.getUserAgent()));
				if (authCookies!=null) urlLoader.setCookies(authCookies);
				if (authBasicLogin!=null) urlLoader.setBasicLogin(authBasicLogin);

				int ret = 0;
				int checkForDeletionStatusCode = 0;

				if (queue.isCheckDeletionMode()) {
					checkForDeletionStatusCode = urlLoader.getHeadStatusCode(pageURL.toExternalForm());
					if (checkForDeletionStatusCode==200) 
						ret = WebPageLoader.LOAD_SUCCESS;
					else
						ret = WebPageLoader.LOAD_ERROR;
				}
				else {
					ret = urlLoader.openRetry(pageURL.toExternalForm(), 3);
				}

				if (ret == WebPageLoader.LOAD_SUCCESS) {
					if (!queue.isCheckDeletionMode()) {
						if (level == 0 && "m".equals(currentUrlItem.getRootUrlMode())) {
							// process sitemaps
							processSitemaps( currentUrlItem, urlLoader, currentNormalizedUrl, pageURL, "", threadId);
							return 1;
						}

						contentType = urlLoader.getContentType();
						if (contentType==null)
						{
							logger.log("[" + String.valueOf(threadId) + "] rejected due to not unknown content-type");
							if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    rejected due to not unknown content-type");
							urlLoader.close();
							urlLoader = null;
							return 0;							
						} else {
							logger.log("[" + String.valueOf(threadId) + "] content-type: " + contentType);

							String contentTypeExclude = config.getProperty("/crawler/param[@name='contenttype_exclude']", "").trim();
							String contentTypeInclude = config.getProperty("/crawler/param[@name='contenttype_include']", "").trim();

							if (!isAcceptedContentType(contentType, contentTypeInclude, contentTypeExclude)) {
								logger.log("[" + String.valueOf(threadId) + "] rejected due to not accepted content-type : " + contentType);
								if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    rejected due to not accepted content-type : " + contentType);
								urlLoader.close();
								urlLoader = null;
								return 0;							
							}
						}

						//String contentEncoding = urlLoader.getContentEncoding();

						int maxContentLength = Integer.parseInt(config.getProperty("/crawler/param[@name='max_page_length']", "0"));
						if (maxContentLength > 0 && urlLoader.getContentLength() > 0 && maxContentLength < urlLoader.getContentLength()) {
							logger.log("[" + String.valueOf(threadId) + "] rejected due to too large content-length : " + String.valueOf(urlLoader.getContentLength()));
							urlLoader.close();
							urlLoader = null;
							return 0;							
						}

						HashMap<String,String> metas = new HashMap<String,String>();
						metas.put("name", src.getName());
						metas.put("country", src.getCountry());
						metas.put("collections", src.getCollections());
						metas.put("tags", src.getTags());
						metas.put("comment", src.getComment());
						metas.put("contact", src.getContact());
						metas.put("language", src.getLanguage());

						metas.put("meta_custom", getUrlMeta(pageURL.toExternalForm(), src.getFilteringRules(), src.getMetadata()));

						HashMap<String,String> params = new HashMap<String,String>();
						params.put("useragent", getUserAgent (config.getProperty("/crawler/param[@name='user_agent']", "CaBot"), this.src.getUserAgent()));

						params.put("url", pageURL.toExternalForm());
						params.put("referrer", currentUrlItem.getReferrer());
						params.put("depth", Integer.toString(level));
						params.put("languageDetectionList", src.getLanguageDetectionList());
						params.put("automaticCleaning", src.getAutomaticCleaning());
						if (urlLoader.getContentLength()>0)
							params.put("contentSize", Integer.toString(urlLoader.getContentLength()));

						if (!WebPageLoader.isRss(contentType, null)) {
                            SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String firstCrawlDate = StringUtils.trimToEmpty(queue.getCreated(itemData));
							Date d = null;
							if (firstCrawlDate == null || "".equals(firstCrawlDate) ) {
                            	d = new Date();
                            } else {
                            	d = new Date(Long.parseLong(firstCrawlDate));
                            }	
                            params.put("firstCrawlDate", dateFormat.format(d.getTime()));
							
							if (!WebPageLoader.isHtmlOrText(contentType)) {
								if ((isIndexedUrl(pageURL.toExternalForm(), src.getFilteringRules())) && (!startingUrls.isNotIndexableStartingUrl(pageURL.toExternalForm()))){
									if (write) logger.log("[" + String.valueOf(threadId) + "]     Send to document handler " + pageURL.toExternalForm());
									/*
									 * all content-type except html, text and flash
									 */
									params.put("refererCharSet", currentUrlItem.getRefererCharSet());
									params.put("contentType", contentType);

									// send to document handler as a Stream
									try {
										if (!src.isTest() && write) {
											String sendUrl = getUrlWithoutSessionIdFields(pageURL.toExternalForm());
											sendUrl = getUrlWithoutIgnoredFields(sendUrl);
											if (docCache==null) {
												dh.sendDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString(), urlLoader.getStream(), params, metas, src.getExtra(), this);												
											} else {
												docCache.put(sendUrl, urlLoader.getStream(), 0, params, metas, src.getExtra());	
												DocumentCacheItem cacheItem = docCache.get(sendUrl);
												dh.sendDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString(), cacheItem.streamData, params, metas, src.getExtra(), this);																								
											}
											//dh.sendDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString(), urlLoader.getStream(), params, metas, src.getExtra(), this);
											//if (docCache!=null) docCache.put(sendUrl, urlLoader.getStream(), 0, params, metas, src.getExtra());											
										}
									} catch(Exception e) {
										e.printStackTrace();
									}
								}
								else {
									urlMode = "l";
									logger.log("[" + String.valueOf(threadId) + "]     Do not send to document handler due to source rules " + pageURL.toExternalForm());
								}
							} 
							else {
								/*
								 * html, text and flash
								 */
								String charSet = null;
								String declaredLanguage =null;

								if (contentType.toLowerCase().startsWith("application/x-shockwave-flash"))
								{
									params.put("originalContentType", "application/x-shockwave-flash");
									contentType = "text/html; charset=utf-8";

									String swfToHtmlPath = Utils.getValidPropertyPath(config.getProperty("/crawler/param[@name='swfToHtmlPath']", ""), null, "HOME");

									TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_HTML, TikaWrapper.CONTENT_TYPE_SWF);
									tikaWrapper.setSwfToHtmlPath(swfToHtmlPath);
									tikaWrapper.process(urlLoader.getStream());
									rawPage = tikaWrapper.getText();
									
									//MultiFormatTextExtractor extractor = new MultiFormatTextExtractor();
									//extractor.setSwfToHtmlPath(swfToHtmlPath);
									//rawPage = extractor.swfInputStreamToHtml(urlLoader.getStream());

									if (urlLoader.getContentLength()==0)
										params.put("contentSize", Integer.toString(rawPage.length()));

									// Extract Links
									logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - parsing links");
									if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - parsing links");
									links = new Links(rawPage, pageURL.toExternalForm(), false, scriptName);
									if (links!=null) {
										String[] pushLink = Utils.mergeStringArrays(links.getLinks0(), links.getLinks1());
										int i=1;
										if (pushLink!=null) {
											for (String s : pushLink) {
												metas.put("link"+String.valueOf(i++),s);
											}   
										}
									}

									charSet = "utf-8";
									declaredLanguage = HttpUtils.getHtmlDeclaredLanguage(rawPage);
								} 
								else {
									/*
									HttpStream ws = new HttpStream(urlLoader.getStream(), "", contentType, contentEncoding);
									rawPage = ws.getString();
									charSet = ws.getCharSet();
									declaredLanguage = ws.getDeclaredLanguage();
									ws.clear();
									*/
									rawPage = urlLoader.getString();
									charSet = urlLoader.getCharSet();
									declaredLanguage = urlLoader.getDeclaredLanguage();
									
									if (urlLoader.getContentLength()==0)
										params.put("contentSize", Integer.toString(rawPage.length()));

									// Re-check if it is a feed ?
									if (WebPageLoader.isFeed(rawPage)) {
										isFeed = true;
									}
									else {
										if (WebPageLoader.isHtml(contentType)) {
											HashMap<String, String> m = HttpUtils.extractMetas(rawPage);
											if (m!=null && m.size() > 0) {
												for (Map.Entry<String, String> item : m.entrySet()) {
													metas.put(item.getKey().replaceAll(" ", "_"), item.getValue());
													if ("robots".equals(item.getKey().toLowerCase())) {
														if (item.getValue().toLowerCase().indexOf("nofollow")!=-1)
															follow = false;
														if (item.getValue().toLowerCase().indexOf("noindex")!=-1)
															index = false;
													}
												}
											}
										}
									}

									// Extract Links
									logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - parsing links");
									if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - parsing links");
									links = new Links(rawPage, pageURL.toExternalForm(), WebPageLoader.isRss(contentType, null) || isFeed, scriptName);
									if (links!=null) {
										String[] pushLink = Utils.mergeStringArrays(links.getLinks0(), links.getLinks1());
										if (pushLink!=null) {
											int i=1;
											for (String s : pushLink) {
												metas.put("link"+String.valueOf(i++),s);
											}   
										}
									}

									if (("1".equals(config.getProperty("/crawler/param[@name='bypass_robots_meta']", "0"))) || (index)) {
										if ((isIndexedUrl(pageURL.toExternalForm(), src.getFilteringRules())) && (!startingUrls.isNotIndexableStartingUrl(pageURL.toExternalForm()))){
											if (write) logger.log("[" + String.valueOf(threadId) + "]     Send to document handler " + pageURL.toExternalForm());
											params.put("contentType", contentType);
											params.put("contentCharSet", charSet);
											params.put("declaredLanguage", declaredLanguage);

											// send to document handler as a String
											try {
												if (!src.isTest() && write) {
													String sendUrl = getUrlWithoutSessionIdFields(pageURL.toExternalForm());
													sendUrl = getUrlWithoutIgnoredFields(sendUrl);
													dh.sendDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString(), rawPage, params, metas, src.getExtra(), this);
													if (docCache!=null) {
														InputStream stream = new ByteArrayInputStream(rawPage.getBytes("UTF-8"));
														docCache.put(sendUrl, stream, 0, params, metas, src.getExtra());											
													}
												}
											} catch(Exception e) {
												e.printStackTrace();
											}
										}
										else {
											urlMode = "l";
											logger.log("[" + String.valueOf(threadId) + "]     Do not send to document handler due to source rules " + pageURL.toExternalForm());
										}
									}
									else {
										urlMode = "s";
										logger.log("[" + String.valueOf(threadId) + "]     Do not send to document handler due to robots meta " + pageURL.toExternalForm());									
									}
									refererCharSet = charSet;
								}
							}
						}
						else {
							/*
							HttpStream ws = new HttpStream(urlLoader.getStream(), "", contentType, contentEncoding);
							rawPage = ws.getString();
							ws.clear();					
							*/
							rawPage = urlLoader.getString();

							if (urlLoader.getContentLength()==0)
								params.put("contentSize", Integer.toString(rawPage.length()));

							isFeed = true;

							// Extract Links
							logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - parsing links");
							if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - parsing links");
							links = new Links(rawPage, pageURL.toExternalForm(), WebPageLoader.isRss(contentType, null) || isFeed, scriptName);
							if (links!=null) {
								String[] pushLink = Utils.mergeStringArrays(links.getLinks0(), links.getLinks1());
								int i=1;
								for (String s : pushLink) {
									metas.put("link"+String.valueOf(i++),s);
								}   
							}
						}

						if (isFeed) {
							if ("1".equals(config.getProperty("/crawler/param[@name='rss_extract']", "0"))) {
								params.put("contentType", contentType);
								InputStream xmlInput = new ByteArrayInputStream(rawPage.getBytes("UTF-8"));

								// send to document handler as a String
								try {
									if (!src.isTest() && write) {
										String sendUrl = getUrlWithoutSessionIdFields(pageURL.toExternalForm());
										sendUrl = getUrlWithoutIgnoredFields(sendUrl);
										dh.sendDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString(), xmlInput, params, metas, src.getExtra(), this);
										if (docCache!=null) {
											xmlInput = new ByteArrayInputStream(rawPage.getBytes("UTF-8"));
											docCache.put(sendUrl, xmlInput, 0, params, metas, src.getExtra());											
										}
									}
								} catch(Exception e) {
									e.printStackTrace();
								}								
							}
						}
					}
				} // if (ret == WebPageLoader.LOAD_SUCCESS) 
				else
				{ // if (ret != WebPageLoader.LOAD_SUCCESS) 
					int httpStatusCode = 0;
					if (queue.isCheckDeletionMode()) 
						httpStatusCode = checkForDeletionStatusCode;
					else
						httpStatusCode = urlLoader.getErrorCode();

					// Remove job and remove from cache
					if (removeDocHttpStatus!=null && removeDocHttpStatus.contains(String.valueOf(httpStatusCode))) {
						// check 
						String startUrl = currentUrlItem.getUrlStart();
						WebPageLoader urlLoaderCheck = new WebPageLoader();
						if (simulateHttps) urlLoader.setSimulateHttps(simulateHttps);
						try {
							// load page
							urlLoaderCheck.setUserAgent(getUserAgent (config.getProperty("/crawler/param[@name='user_agent']", "CaBot"), this.src.getUserAgent()));
							if (authCookies!=null) urlLoaderCheck.setCookies(authCookies);
							if (authBasicLogin!=null) urlLoader.setBasicLogin(authBasicLogin);

							//if (urlLoaderCheck.openRetry(3) == WebPageLoader.LOAD_SUCCESS) {
							int statusCode = urlLoaderCheck.getHeadStatusCode(startUrl);
							if (statusCode==200) {
								logger.log("[" + String.valueOf(threadId) + "]     Page not available (" + removeDocHttpStatus + ") -> send delete job");                                    
								String sendUrl = getUrlWithoutSessionIdFields(pageURL.toExternalForm());
								dh.removeDoc(new Integer(currentUrlItem.getSourceId()).toString(), sendUrl, new Integer(src.getAccountId()).toString());                            
								if (docCache!= null) docCache.remove(sendUrl);
							}
						} catch (Exception e) {}
					}                        

					// Update item in queue with crawl status + conditional get info + timestanp
					String message = "";
					message = String.valueOf(urlLoader.getErrorCode()) + " - " + urlLoader.getErrorMessage();
					currentUrlItem.setContentType("");
					currentUrlItem.setCrawlStatus(urlLoader.getResponseStatusCode());
					currentUrlItem.setCrawlStatusMessage(urlLoader.getResponseReasonPhrase());
					currentUrlItem.setCrawlMode(urlMode);
					currentUrlItem.setCrawlLastTime(new Date());
					currentUrlItem.setCondgetETag("");
					currentUrlItem.setCondgetLastModified("");
					queue.updateDone(currentUrlItem.getMap()); 
					
					urlLoader.getResponseStatusCode();

					if ((ret == WebPageLoader.LOAD_PAGEREDIRECTED) && (!queue.isCheckDeletionMode())) { 
						int maxRedirection = Integer.parseInt(config.getProperty("/crawler/param[@name='max_redirection']", "6"));
						if (currentUrlItem.getRedirectionCount()<=maxRedirection) {
							String strLink = urlLoader.getRedirectionLocation();
							if (StringUtils.trimToNull(strLink)==null) {
								urlLoader.close();
								urlLoader = null;
								return 0;								
							}
							
							strLink = HttpUtils.urlGetAbsoluteURL(pageURL.toExternalForm(), strLink);
							String normalizedStartUrl = HttpUtils.urlNormalize(currentUrlItem.getUrlStart(), null).toLowerCase();			

							if (!isAccepetedUrl ( strLink, normalizedStartUrl, src.getHostAliases(), currentUrlItem.getRootUrlMode(), currentUrlItem.isAllowOtherDomain(), threadId, currentUrlItem.getDepth())) {
								urlLoader.close();
								urlLoader = null;
								return 0;
							}

							/*
							Map<String,Object> itemDataDebug = queue.getDone(strLink);
							if (itemDataDebug==null) {
								int ggg = 0;
							}
							*/
							
							//SourceItemWeb doneUrlItem = new SourceItemWeb(queue.getDone(strLink));
							SourceItemWeb doneUrlItem = SourceItemWeb.newInstance(queue.getDone(strLink));
							if (doneUrlItem==null || (level+1)<doneUrlItem.getDepth()) {
								//urlDone = (doneUrlItem!=null);
								//if (!urlDone) {
								logger.log("[" + String.valueOf(threadId) + "] " + strLink + " (redirection: " + String.valueOf(currentUrlItem.getRedirectionCount()+1) + ") added to queue");
								if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + strLink + " (redirection: " + String.valueOf(currentUrlItem.getRedirectionCount()+1) + ") added to queue");
								SourceItemWeb urlItemToPush = new SourceItemWeb(currentUrlItem.getSourceId(), strLink, level, currentUrlItem.getReferrer(), currentUrlItem.getUrlStart(), "", refererCharSet, null, null, null, "", "", "", "", "", "", currentUrlItem.getRootUrlMode(), currentUrlItem.isAllowOtherDomain());

								if (!queue.contains(urlItemToPush.getUrl())) {
									urlItemToPush.setRedirectionCount(currentUrlItem.getRedirectionCount()+1);
									queue.push(urlItemToPush.getMap());
								}
							} else {
								logger.log("[" + String.valueOf(threadId) + "] " + strLink + " (redirection: " + String.valueOf(currentUrlItem.getRedirectionCount()+1) + ") ignored (already done)");
								if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + strLink + " (redirection: " + String.valueOf(currentUrlItem.getRedirectionCount()+1) + ") ignored (already done)");
							}
							urlLoader.close();
							urlLoader = null;
							return 0;
						}
						else
						{
							urlLoader.close();
							urlLoader = null;
							return 0;
						}
					}
					else
					{
						if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + message);
						urlLoader.close();
						urlLoader = null;
						return 0;
					}
				} // if (ret != WebPageLoader.LOAD_SUCCESS) 
				urlLoader.close();
			}
			catch (Exception e)
			{
				// Update item in queue with crawl status + conditional get info + timestanp
				currentUrlItem.setContentType(contentType);
				currentUrlItem.setCrawlStatus(urlLoader.getResponseStatusCode());
				currentUrlItem.setCrawlStatusMessage(urlLoader.getResponseReasonPhrase());
				currentUrlItem.setCrawlMode(urlMode);
				currentUrlItem.setCrawlLastTime(new Date());
				if (urlLoader!=null) {
					currentUrlItem.setCondgetETag(urlLoader.getCondGetETag());
					currentUrlItem.setCondgetLastModified(urlLoader.getCondGetLastModified());
					urlLoader.close();
					urlLoader = null;
				}
				queue.updateDone(currentUrlItem.getMap()); 

				//if (currentUrlItem.getDepth()==0)
				//	queue.setInfo(e.getMessage());

				logger.log("Error with URL : " + pageURL.toExternalForm());
				logger.logStackTrace(e, true);
				return 0;
			}

			// TODO : v4 - double update here if ret != WebPageLoader.LOAD_SUCCESS ???
			
			// Update item in queue with crawl status + conditional get info + timestanp
			currentUrlItem.setContentType(contentType);
			currentUrlItem.setCrawlStatus(urlLoader.getResponseStatusCode());
			currentUrlItem.setCrawlStatusMessage(urlLoader.getResponseReasonPhrase());
			currentUrlItem.setCrawlMode(urlMode);
			currentUrlItem.setCrawlLastTime(new Date());
			currentUrlItem.setCondgetETag(urlLoader.getCondGetETag());
			currentUrlItem.setCondgetLastModified(urlLoader.getCondGetLastModified());
			queue.updateDone(currentUrlItem.getMap()); 
			urlLoader.close();
			urlLoader = null;
			
			if (!HttpUtils.urlBelongSameHost(null, pageURL.toExternalForm(), src.getHostAliases())) {
				if (!currentUrlItem.isAllowOtherDomain() || currentUrlItem.getDepth()!=0) {	
					logger.log("[" + String.valueOf(threadId) + "] skip link parsing due to invalid host");
					if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("        skip link parsing due to invalid host");
					return 1;		
				}
			}

			// If checkfordeletion mode : exit (do not inject new urls)
			if (queue.isCheckDeletionMode()) {
				logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - checkfordeletion mode - skip link parsing");
				return 1;
			}
			
			// If rescan mode : exit (do not inject new urls)
			if (src.isRescan() || src.isRescanFromCache()) {
				logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - rescan mode - skip link parsing");
				return 1;
			}

			// HTML / RSS ???
			if (!WebPageLoader.isHtmlOrText(contentType) && !WebPageLoader.isRss(contentType, null)) return 1;

			// TODO : Send page to cache

			if (rawPage==null) {
				logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - no content");
				if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - no content");
				return 1;
			}

			if (!follow) {
				logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - no follow");
				if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - no follow");
			}

			boolean deeper = true;

			if (isStartingUrl (pageURL.toExternalForm()) || isLinksParsedUrl(pageURL.toExternalForm(), src.getFilteringRules())) {
				//if (verbose) logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - parsing links");
				//if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    parsing links");
				if (!WebPageLoader.isRss(contentType, null) && !isFeed) {
					if (("1".equals(config.getProperty("/crawler/param[@name='bypass_robots_meta']", "0"))) || (follow)) {

						// If RSS / Links
						if (!"s".equals(currentUrlItem.getRootUrlMode()) && currentUrlItem.getDepth()==1) {
							deeper = false;
							logger.log("Max depth reached for rss, links or sitemaps mode (1)");
						} else {
							// If actual level is >= to maxLevel : exit
							if (level >= maxCrawlDepth) {
								deeper = false;
								logger.log("Max depth reached :" + Integer.toString(maxCrawlDepth));
							}
						}

						if (!deeper && links!=null) links.links1=null;

					} else {
						links=null;
					}
				}
			}
			else {
				links=null;
			}

			int linksCount = 0;
			if (links!= null && links.links0 != null) {
				linksCount += links.links0.size();
				for (String strLink : links.links0) {
					pushLink (strLink, level, level, currentUrlItem, currentNormalizedUrl, pageURL, refererCharSet, threadId);
				}
			}
			if (links!= null && links.links1 != null) {
				linksCount += links.links1.size();
				for (String strLink : links.links1) {
					pushLink (strLink, level, level+1, currentUrlItem, currentNormalizedUrl, pageURL, refererCharSet, threadId);
				}
			}
			if (deeper) {
				logger.log("[" + String.valueOf(threadId) + "] " + pageURL.toExternalForm() + " - " + String.valueOf(linksCount) + " links found");
				if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + pageURL.toExternalForm() + " - " + String.valueOf(linksCount) + " links found");
			}

		} catch (Exception e) {

		}
		return 1;
	}

	public static ISource createSourceInstance(Integer id, String className, String crawlMode, Map<String, Object> srcData) {
		try {
			return new SourceWeb (id.intValue(), className, crawlMode, srcData);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ISourceItemsQueue createSourceItemsQueueInstance(String type, IDBConnection con, String dbName, String dbCollName, ISource source) {

		SourceWeb src = (SourceWeb) source;

		StartingUrls startingUrls = src.getStartingUrls();
		if (startingUrls==null) return null;

		ISourceItemsQueue sourceQueue = QueueFactory.getSourceItemsQueueInstance(type, src.getId(), con, dbName, dbCollName);
		if (sourceQueue==null) return null;
		
		if (src.isReset() || src.isClear()) 
			sourceQueue.reset();

		if (src.isClear()) return sourceQueue;

		// TODO : v4 - Utile ???
		if (sourceQueue.getQueueSize()>0 && !src.isRescan() && !src.isRescanFromCache())
			sourceQueue.start();
		else {
			if (!src.isRescan() && !src.isRescanFromCache()) {
				int startDepth = 0;
				if (src.isDeeper()) {
					startDepth = sourceQueue.getCurrentMaxDepth();
				}
				sourceQueue.reStart(startDepth);
			} else {
				sourceQueue.reScan();
			}

		}
		// Utile ???

		if (!src.isRescan() && !src.isRescanFromCache() && !src.isDeeper()) {
			// Add or update starting urls
			boolean haveSiteMode = false;
			for (int i=0; i<startingUrls.size(); i++) {
				if ("s".equals(startingUrls.get(i).mode)) haveSiteMode = true;
			}
			for (int i=0; i<startingUrls.size(); i++) {
				if (!startingUrls.get(i).onlyFirstCrawl || !src.isFirstCrawlCompleted()) {
					if (haveSiteMode && !src.isFirstCrawlCompleted() && !"s".equals(startingUrls.get(i).mode)) continue;
					try {
						sourceQueue.push(startingUrls.get(i).getMap(src.getId()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return sourceQueue;
	}

	public String getName() {
		return "web";
	}

	public int getMaxThreads(int suggestedMaxThreads) {
		return suggestedMaxThreads;
	}

	public static String getUrlIgnoreFields(String url, String urlRules, String srcIgnoreFields) {

		if (urlRules!=null && urlRules.startsWith("<")) {

			String path = "";
			try {
				URL u = new URL(url);
				path = u.getFile();
			}
			catch (MalformedURLException e) {
				path = url;
			}

			SAXReader reader = new SAXReader();
			reader.setValidation(false);
			try {
				Document document = reader.read(new StringReader(urlRules));
				@SuppressWarnings("unchecked")
				List<Node> nodes = document.selectNodes("//rules/rule");
				if (nodes!=null && nodes.size()>0) {
					for (int i = 0; i<nodes.size(); i++) {
						Element rule = (Element) nodes.get(i);
						@SuppressWarnings("unchecked")
						List<Element> ruleItems = rule.elements();
						String ope = null;
						String mode = null;
						String pat = null;
						String ignoreparam = null;
						for (int j = 0; j<ruleItems.size(); j++) {
							Element item = ruleItems.get(j);
							if ("ope".equals(item.getName())) ope = item.getText();
							if ("mode".equals(item.getName())) mode = item.getText();
							if ("pat".equals(item.getName())) pat = item.getText();
							if ("ignoreparam".equals(item.getName())) ignoreparam = item.getText();
						}
						if (mode==null || ope==null || pat==null || ignoreparam==null) continue;
						if ("skip".equals(mode) || "links".equals(mode) || "".equals(ignoreparam)) continue;

						boolean applyRule = false;
						if ("path".equals(ope) && path.startsWith(pat)) {
							applyRule = true;
						}
						else {
							if ("match".equals(ope)) {
								Pattern p = Pattern.compile(pat);
								Matcher m = p.matcher(path);
								if (m.find()) applyRule = true;
							}
						}

						if (applyRule && ignoreparam!=null && !"".equals(ignoreparam)) {
							if (srcIgnoreFields==null) srcIgnoreFields = "";
							if (!"".equals(srcIgnoreFields)) {
								srcIgnoreFields += ",";
							}
							srcIgnoreFields += ignoreparam;
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return srcIgnoreFields;
	}

	private String getUserAgent (String fromProperties, String fromSource) {
		if (fromSource!=null && !"".equals(fromSource)) return fromSource;
		if (fromProperties!=null && !"".equals(fromProperties)) return fromProperties;
		return "";
	}

	private String getUrlWithoutSessionIdFields(String url) {
		if (ignoreUrlFields!=null && !"".equals(ignoreUrlFields)) {
			url = HttpUtils.urlRemoveParameters ( url, ignoreUrlFields);
		}
		return url;
	}

	private String getUrlWithoutIgnoredFields (String url) {
		String fields = getUrlIgnoreFields(url, this.src.getFilteringRules(), ignoreUrlFieldsNoSessionId);
		if (fields!=null && !"".equals(fields)) {
			url = HttpUtils.urlRemoveParameters ( url, fields);
		}
		return url;
	}

	private String getAlternateProtocolUrl(String url) {
		if (src.getProtocolStrategy()==0) return null;
		try {
			URL pageURL = new URL(url);
			if ("http".equals(pageURL.getProtocol()) && src.getProtocolStrategy()==2) {
				return url.replace("http://", "https://");
			}
			if ("https".equals(pageURL.getProtocol()) && src.getProtocolStrategy()==1) {
				return url.replace("https://", "http://");
			}
			return null;
		} catch(Exception e) {
			return null;
		}
	}


	public static String getUrlMeta(String url, String urlRules, String srcMeta) {

		ArrayList<String> metas = new ArrayList<String>();
		if (srcMeta!=null && !"".equals(srcMeta)) {
			String[] aSrcMeta = srcMeta.split("\n");
			for (String s: aSrcMeta) {
				s = s.trim();
				if (!"".equals(s)) {
					String[] aItems = s.split(":");
					if (aItems.length==2) {
						String key = aItems[0].trim().toLowerCase();
						if (key.charAt(0)!='#') {
							String value = aItems[1].trim();
							boolean found = false;
							for (int j=0; j<metas.size(); j++) {
								if (metas.get(j).equals(key+":"+value)) found = true;
							}
							if (!found) metas.add(key + ":" + value);
						}
					}
				}
			}
		} 

		if (urlRules!=null && urlRules.startsWith("<")) {

			String path = "";
			try {
				URL u = new URL(url);
				path = u.getFile();
			}
			catch (MalformedURLException e) {
				path = url;
			}

			SAXReader reader = new SAXReader();
			reader.setValidation(false);
			try {
				Document document = reader.read(new StringReader(urlRules));
				@SuppressWarnings("unchecked")
				List<Node> nodes = document.selectNodes("//rules/rule");
				if (nodes!=null && nodes.size()>0) {
					for (int i = 0; i<nodes.size(); i++) {
						Element rule = (Element) nodes.get(i);
						@SuppressWarnings("unchecked")
						List<Element> ruleItems = rule.elements();
						String ope = null;
						String mode = null;
						String pat = null;
						String meta = null;
						for (int j = 0; j<ruleItems.size(); j++) {
							Element item = ruleItems.get(j);
							if ("ope".equals(item.getName())) ope = item.getText();
							if ("mode".equals(item.getName())) mode = item.getText();
							if ("pat".equals(item.getName())) pat = item.getText();
							if ("meta".equals(item.getName())) meta = item.getText();
						}
						if (mode==null || ope==null || pat==null || meta==null) continue;
						if ("skip".equals(mode) || "links".equals(mode) || "".equals(meta)) continue;

						boolean applyRule = false;
						if ("path".equals(ope) && path.startsWith(pat)) {
							applyRule = true;
						}
						else {
							if ("match".equals(ope)) {
								Pattern p = Pattern.compile(pat);
								Matcher m = p.matcher(path);
								if (m.find()) applyRule = true;
							}
						}

						if (applyRule) {
							String[] aMeta = meta.split("\\|");
							for (String s: aMeta) {
								s = s.trim();
								if (!"".equals(s)) {
									String[] aItems = s.split(":");
									if (aItems.length==2) {
										String key = aItems[0].trim().toLowerCase();
										if (key.charAt(0)!='#') {
											String value = aItems[1].trim();
											boolean remove = false;
											if (key.startsWith("-")) {
												key=key.substring(1);
												remove = true;
											}
											boolean found = false;
											for (int j=0; j<metas.size(); j++) {
												if (metas.get(j).equals(key+":"+value)) {
													found = true;
													if (remove) metas.set(j, "");
												}
											}
											if (!found && !remove) metas.add(key + ":" + value);
										}
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		String v = "";
		if (metas!=null && metas.size() > 0) {
			for (int i=0;i<metas.size(); i++) {
				if (!"".equals(metas.get(i))) {
					String[] aItems = metas.get(i).split(":");
					if (aItems.length==2) {
						String key = aItems[0].trim();
						String value = aItems[1].trim();
						if (!"".equals(v)) v += "|";
						v += key.replaceAll(" ", "_") + ":" + value;
					}
				}
			}
			//if (!"".equals(v)) metas.put("meta_custom", v);
		}
		return v;
	}

	public static boolean isLinksParsedUrl(String url, String filteringRules) {

		String mode = CrawlerUtilsCommon.getUrlMode(url, filteringRules, "a");
		if ("s".equals(mode) || "g".equals(mode))
			return false;
		else
			return true;
	}

	public static boolean isIndexedUrl(String url, String filteringRules) {

		String mode = CrawlerUtilsCommon.getUrlMode(url, filteringRules, "a");
		if ("s".equals(mode) || "l".equals(mode))
			return false;
		else
			return true;
	}

	public static boolean isAcceptedUrl(String url, String filteringRules) {

		String mode = CrawlerUtilsCommon.getUrlMode(url, filteringRules, "a");
		if ("s".equals(mode))
			return false;
		else
			return true;
	}

	private void processSitemaps(SourceItemWeb currentUrlItem, WebPageLoader pageLoader, String currentNormalizedUrl, URL pageURL, String refererCharSet, long threadId) {

		List<String> links = new ArrayList<String>();
		int level = 1;

		try {
			crawlercommons.sitemaps.SiteMapParser parser = new SiteMapParser();
			AbstractSiteMap asm = parser.parseSiteMap(pageLoader.getContentType(), IOUtils.toByteArray(pageLoader.getStream()), new URL(currentUrlItem.getUrl()));

			if (asm.isIndex()) {
				// push all url with depth = 0
				level = 0;
				Collection<AbstractSiteMap> sm = ((SiteMapIndex)asm).getSitemaps();
				Iterator<AbstractSiteMap> i=sm.iterator();
				while(i.hasNext()) {
					SiteMap s = (SiteMap) i.next();
					URL url = s.getUrl();
					links.add(url.toExternalForm());
				}
			}
			else {
				// push all url with depth = 1
				level = 1;
				Collection<SiteMapURL> u = ((SiteMap)asm).getSiteMapUrls();
				Iterator<SiteMapURL> i = u.iterator();  
				while(i.hasNext()) {
					SiteMapURL s = (SiteMapURL) i.next();
					URL url = s.getUrl();					
					if (!src.isFirstCrawlCompleted() || src.isReset() || s.getLastModified() == null || src.getCrawlLastTimeStart().before(s.getLastModified())) {
						links.add(url.toExternalForm());	
					}					
				}
			}
			for (String strLink : links) {
				pushLink (strLink, 0, level, currentUrlItem, currentNormalizedUrl, pageURL, refererCharSet, threadId);
			}			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return;
	}

	private void pushLink (String strLink, int currentLevel, int nextLevel, SourceItemWeb currentUrlItem, String currentNormalizedUrl, URL pageURL, String refererCharSet, long threadId) {

		try {		
			strLink = StringUtils.trimToNull(strLink);
			if (strLink==null) {
				return;
			}
			strLink = HttpUtils.urlGetAbsoluteURL(pageURL.toExternalForm(), strLink);
			strLink = HttpUtils.urlNormalize(strLink, src.getHost());			
			String strLink2 = getUrlWithoutIgnoredFields(strLink);
			if (!strLink2.equals(strLink)) {
				logger.log("[" + String.valueOf(threadId) + "] Ignored fields removed = " + strLink + " => " + strLink2);
				strLink = strLink2;
			}
			if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("    " + strLink);

			String normalizedStartUrl = HttpUtils.urlNormalize(currentUrlItem.getUrlStart(), null).toLowerCase();			
			if (!isAccepetedUrl (strLink, normalizedStartUrl, src.getHostAliases(), currentUrlItem.getRootUrlMode(), currentUrlItem.isAllowOtherDomain(), threadId, currentUrlItem.getDepth()))
				return;

			/*
			 * Push the link into the queue
			 */
			boolean push = true;
			SourceItemWeb urlItemToPush = new SourceItemWeb(currentUrlItem.getSourceId(), strLink, nextLevel, currentNormalizedUrl, currentUrlItem.getUrlStart(), "", refererCharSet, null, null, null, "", "", "", "", "", "", currentUrlItem.getRootUrlMode(), currentUrlItem.isAllowOtherDomain());
			//SourceItemWeb doneUrlItem = new SourceItemWeb(queue.getDone(urlItemToPush.getUrl()));

			if (queue.getDone(urlItemToPush.getUrl())!=null) {
				SourceItemWeb doneUrlItem = new SourceItemWeb(queue.getDone(urlItemToPush.getUrl()));
				if ( (nextLevel)>=doneUrlItem.getDepth() && "s".equals(currentUrlItem.getRootUrlMode()) ) {
					push = false;
					logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to already done");
				}
			} else {
				if (queue.contains(urlItemToPush.getUrl())) {
					push = false;
					logger.log("[" + String.valueOf(threadId) + "] " + strLink + " already in queue");
				}               
			}
			if (push) {
				logger.log("[" + String.valueOf(threadId) + "] " + strLink + " added to queue");
				if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("        " + strLink + " added to queue");
				queue.push(urlItemToPush.getMap());
			}

			/*
			 * Push the alternate link into the queue according to protocol strategy
			 */
			String alternateUrl = getAlternateProtocolUrl(strLink);
			if (alternateUrl!=null) {
				push = true;
				urlItemToPush = new SourceItemWeb(currentUrlItem.getSourceId(), alternateUrl, nextLevel, currentNormalizedUrl, currentUrlItem.getUrlStart(), "", refererCharSet, null, null, null, "", "", "", "", "", "", currentUrlItem.getRootUrlMode(), currentUrlItem.isAllowOtherDomain());
				//urlItemToPush.setUrl(alternateUrl);
				//SourceItemWeb doneUrlItem = new SourceItemWeb(queue.getDone(urlItemToPush.getUrl()));

				if (queue.getDone(urlItemToPush.getUrl())!=null) {
					SourceItemWeb doneUrlItem = new SourceItemWeb(queue.getDone(urlItemToPush.getUrl()));
					if ( (nextLevel)>=doneUrlItem.getDepth() && "s".equals(currentUrlItem.getRootUrlMode()) ) {
						push = false;
						logger.log("[" + String.valueOf(threadId) + "] " + alternateUrl + " rejected due to already done");
					}
				} else {
					if (queue.contains(urlItemToPush.getUrl())) {
						push = false;
						logger.log("[" + String.valueOf(threadId) + "] " + alternateUrl + " already in queue");
					}               
				}
				if (push) {
					logger.log("[" + String.valueOf(threadId) + "] " + alternateUrl + " added to queue");
					if (currentUrlItem.getDepth()<=memlogMaxDepth) src.memLogAppend("        " + alternateUrl + "added to queue");
					queue.push(urlItemToPush.getMap());
				}   
			}            
		} catch (Exception e) {
			logger.log(strLink);
			logger.logStackTrace(e, true);
		}

	}

	private boolean isAccepetedUrl (String strLink, String normalizedStartUrl, List<String> hostAliases, String startUrlMode, boolean allowOtherDomain, long threadId, int depth) {

		try {
			if (StringUtils.trimToNull(strLink)==null) return false;

			URL urlLink = new URL(strLink);
			URL pageURL = new URL(normalizedStartUrl);

			// Filtre l'url par rapport à l'extension
			if (strLink.lastIndexOf(".")!=1 && strLink.lastIndexOf(".")!=strLink.length())
			{
				String extension = urlLink.getPath();
				if (extension!=null && !"".equals(extension) && extension.lastIndexOf(".")!=-1) {
					extension = extension.substring(extension.lastIndexOf(".")+1);

					String extensionExclude = config.getProperty("/crawler/param[@name='extension_exclude']", "").trim();

					if (extension.matches("[a-zA-Z0-9]*") && !isAcceptedExtension(extension, extensionExclude))
					{
						logger.log("[" + String.valueOf(threadId) + "] rejected due to not accepted extension : " + extension);
						if (depth<=memlogMaxDepth) src.memLogAppend("        rejected due to not accepted extension : " + extension);
						return false;							
					}
				}
			}

			//if (hostAliases!=null && startUrlMode!=null) {
			
			/*
				// Filtre l'url par rapport au serveur
				if ("s".equals(startUrlMode) || allowOtherDomain) {
					if (!HttpUtils.urlBelongSameHost(pageURL.toExternalForm(), strLink, hostAliases))				
					{
						logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to invalid host (" + normalizedStartUrl + ")");
						if (depth<=memlogMaxDepth) src.memLogAppend("        rejected due to invalid host (" + normalizedStartUrl + ")");
						return false;							
					}
				}
			*/
			// Filtre l'url par rapport au serveur
			if (!HttpUtils.urlBelongSameHost(pageURL.toExternalForm(), strLink, hostAliases) && !allowOtherDomain) {
				logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to invalid host (" + normalizedStartUrl + ")");
				if (depth<=memlogMaxDepth) src.memLogAppend("        " + strLink + " rejected due to invalid host (" + normalizedStartUrl + ")");
				return false;							
			}


				// Filtre l'url par à sa filiation
				if ("s".equals(startUrlMode)) {
					if (!"0".equals(src.getChildOnly()))
					{
						if ("1".equals(src.getChildOnly()) || ("2".equals(src.getChildOnly()) && "1".equals(config.getProperty("/crawler/param[@name='child_only']", "0"))))
						{
							URL urlNormalizedStartUrl = new URL(normalizedStartUrl);
							if (!HttpUtils.isChildOf(urlLink,urlNormalizedStartUrl))
							{
								logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to invalid path (" + normalizedStartUrl + ")");
								if (depth<=memlogMaxDepth) src.memLogAppend("        " + strLink + " rejected due to invalid path (" + normalizedStartUrl + ")");
								return false;							
							}
						}
					}
				}
			//}

			// Filtre l'url par rapport aux règles de chemin
			if (!"".equals(src.getFilteringRules()))
			{
				if (!isAcceptedUrl(strLink, src.getFilteringRules()))
				{
					logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to filtering rules");
					if (depth<=memlogMaxDepth) src.memLogAppend("        " + strLink + " rejected due to filtering rules");
					return false;							
				}
			}

			// Do not push url that belong to binary file list that do not have to be recrawl due to period setting
			// TODO: V4 - optimisation des binaires
			/*
            if (urlQueue.containsPreviousCrawledBinaryItemsList(strLink))
            {
                logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to binary file period recrawl setting");
                return false;							
            }
			 */

			// Filtre l'url par rapport aux règles du fichier robots.txt
			if (robots!=null && !isStartingUrl(strLink) && !"1".equals(config.getProperty("/crawler/param[@name='bypass_robots_file']", "0")) && !robots.isUrlAllowed(urlLink)) 						
			{
				logger.log("[" + String.valueOf(threadId) + "] " + strLink + " rejected due to robots.txt exclusion rules");
				if (depth<=memlogMaxDepth) src.memLogAppend("        " + strLink + " rejected due to robots.txt exclusion rules");
				return false;							
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean isStartingUrl (String url) {
		for (int i=0; i<startingUrls.size(); i++) {
			if (url.equals(startingUrls.get(i).url)) return true;   
		}
		return false;
	}
	
    public synchronized void incrProcessedItemCount(long processedItemCount) {
        long queueSize = queue.getQueueSize();
        /*
        if (src.isResetFromCache()) {
            queueSize = Math.max(0,cache.getCacheSize() - processedItemCount);
        }
        */
        updateProcessingInfo(src.getId(), queueSize, queue.getDoneQueueSize(), processedItemCount); //, src.isRescanFromCache());
    }
}