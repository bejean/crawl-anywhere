package fr.eolya.utils.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftConditionalCommentTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Source;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class HttpUtils {

	public static String urlNormalize(String url, String preferedHost) {

		String ret_url = url.trim();

		// Perform some url nomalizations described here : http://en.wikipedia.org/wiki/URL_normalization

		try {
			// Remove last "/" - NO !!!
			//if (ret_url.lastIndexOf("/") == ret_url.length()-1)
			//	ret_url = ret_url.substring(0, ret_url.length()-1); 

			// Remove final "?" if unique in url -     http://www.example.com/display? -> http://www.example.com/display 
			if (ret_url.lastIndexOf("?") == ret_url.length()-1)
				ret_url = ret_url.substring(0, ret_url.length()-1); 

			// Fix "?&"
			int index = ret_url.indexOf("?&");
			//int l = ret_url.length()-2;
			if (index != -1) {
				if (index!=ret_url.length()-2) {
					ret_url = ret_url.substring(0, index+1) + ret_url.substring(index+2); 
				}
				else {
					ret_url = ret_url.substring(0, ret_url.length()-2);
				}
			}

			// Replace "&amp;" by "&"
			ret_url = StringEscapeUtils.unescapeHtml4(ret_url);

			// Replace " " by "%20"
			ret_url = ret_url.replace(" ", "%20");

			// Replace "'" by "%27"
			ret_url = ret_url.replace("'", "%27");

			// Replace "%5F" by "_"
			ret_url = ret_url.replace("%5f", "_");
			ret_url = ret_url.replace("%5F", "_");

			// Remove dot-segments.
			// http://www.example.com/../a/b/../c/./d.html => http://www.example.com/a/c/d.html     		
			URI uri = new URI(ret_url);
			uri = uri.normalize();
			ret_url = uri.toURL().toExternalForm(); 

			// Remove dot-segments at the beginning of the path
			// http://www.example.com/../a/d.html => http://www.example.com/a/d.html     		
			URL tempUrl = new URL(ret_url);
			String path = tempUrl.getFile();
			String pattern = "";
			while (path.startsWith("/../")) {
				path = path.substring(3);
				pattern += "/..";
			}
			if (!pattern.equals("")) {
				index = ret_url.indexOf(pattern);
				ret_url = ret_url.substring(0, index) + ret_url.substring(index + pattern.length());
			}

			// Remove default port
			if (ret_url.indexOf("http://"+uri.getHost() + ":80")!=-1) {
				ret_url = ret_url.replace("//"+uri.getHost() + ":80", "//"+uri.getHost());
			}
			if (ret_url.indexOf("https://"+uri.getHost() + ":443")!=-1) {
				ret_url = ret_url.replace("//"+uri.getHost() + ":443", "//"+uri.getHost());
			}

			// translate to prefered host (www.site.com vs site.com)
			if (preferedHost!=null && !"".equals(preferedHost)) {
				if (uri.getHost().equals("www." + preferedHost) || ("www." + uri.getHost()).equals(preferedHost)) {
					ret_url = ret_url.replace("//"+uri.getHost(), "//"+preferedHost);
				}
			}

			// Remove the fragment.
			// http://www.example.com/bar.html#section1 => http://www.example.com/bar.html 
			if (ret_url.indexOf("#")!=-1)
				ret_url = ret_url.substring(0, ret_url.indexOf("#"));

			// Reorder parameters in query string
			//ret_url = urlReorderParameters (ret_url);

			return ret_url;
		}
		catch (Exception e){}

		return ret_url;
	}

	public static String urlRemoveParameters (String url, String paramsToRemove)
	{
		if (paramsToRemove==null || "".equals(paramsToRemove)) return url;

		try {
			URL u = new URL(url);
			if (u.getQuery()==null && u.getPath().indexOf(";jsessionid=")==-1) return url;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		}

		try {

			url = url.replace("?&", "?");

			if ("*".equals(paramsToRemove)) {
				int offset = url.lastIndexOf("?");
				if (offset!=-1) return url.substring(0, offset);
			}

			paramsToRemove = paramsToRemove.replaceAll(" ", "").replaceAll(";", ",");
			String[] aToRemove = paramsToRemove.split(",");
			String tempUrl = url;
			for (int i=0; i<aToRemove.length; i++) {
				boolean found = true;
				while (found) {
					found = false;
					String re = "[?&;]" + aToRemove[i].toLowerCase() + "[=&]";
					Pattern p = Pattern.compile(re);
					Matcher m = p.matcher(tempUrl.toLowerCase());
					if (m.find()) {
						found = true;
						int start = m.start();
						int stop = start;
						if ("jsessionid".equals(aToRemove[i].toLowerCase())) {
							stop = tempUrl.indexOf("?", start+1);
							if (stop==-1) stop = tempUrl.indexOf("&", start+1);
						}
						else{
							stop = tempUrl.indexOf("&", start+1);
						}
						if (stop==-1) {
							tempUrl = tempUrl.substring(0, start);
						}
						else {
							String ope = tempUrl.substring(start, start+1);
							if (";".equals(ope)) ope = "?";
							tempUrl = tempUrl.substring(0, start) + ope + tempUrl.substring(stop+1);
						}
					}
					re = "[?&;]" + aToRemove[i].toLowerCase() + "$";
					p = Pattern.compile(re);
					m = p.matcher(tempUrl.toLowerCase());
					if (m.find()) {
						found = true;
						int start = m.start();
						int stop = start;
						if ("jsessionid".equals(aToRemove[i].toLowerCase())) {
							stop = tempUrl.indexOf("?", start+1);
							if (stop==-1) stop = tempUrl.indexOf("&", start+1);
						}
						else{
							stop = tempUrl.indexOf("&", start+1);
						}
						if (stop==-1) {
							tempUrl = tempUrl.substring(0, start);
						}
						else {
							String ope = tempUrl.substring(start, start+1);
							if (";".equals(ope)) ope = "?";
							tempUrl = tempUrl.substring(0, start) + ope + tempUrl.substring(stop+1);
						}
					}
				}
			}
			return tempUrl;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}	

	public static Map<String, String> getAuthCookies(int authMode, String authLogin, String authPasswd, String authParam, String proxyHost, String proxyPort, String proxyExclude, String proxyUser, String proxyPassword) {

		if (authMode == 0) return null;

		Map<String, String> authCookies = null;
		String[] aAuthParam = authParam.split("\\|");

		// http://www.java-tips.org/other-api-tips/httpclient/how-to-use-http-cookies.html
		DefaultHttpClient httpclient = new DefaultHttpClient();

		HttpPost httppost = new HttpPost(aAuthParam[0]);
		//httpclient.getParams().setParameter("http.useragent", "Custom Browser");
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		try
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			for (int i = 1; i < aAuthParam.length; i++) {
				String[] aPair = aAuthParam[i].split("=");
				aPair[1] = aPair[1].replaceAll("\\$\\$auth_login\\$\\$", authLogin);
				aPair[1] = aPair[1].replaceAll("\\$\\$auth_passwd\\$\\$", authPasswd);
				nameValuePairs.add(new BasicNameValuePair(aPair[0], aPair[1]));
			}
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			httppost.setHeader("ContentType", "application/x-www-form-urlencoded");
			HttpResponse response = httpclient.execute(httppost, localContext);
			HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				entity.consumeContent();
			}

			List<Cookie> cookies = httpclient.getCookieStore().getCookies();
			if (!cookies.isEmpty()) {
				authCookies = new HashMap<String, String>();
				for (Cookie c : cookies)
				{
					// TODO: What about the path, the domain ???
					authCookies.put(c.getName(), c.getValue());
				}
			}		
			httppost.abort();
		}
		catch (ClientProtocolException e)
		{
			return null;
		}
		catch (IOException e)
		{
			return null;
		}		
		return authCookies;
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static boolean urlBelongSameHost(String urlReferer, String urlHref, List<String> hostAliases) {
		if (urlReferer!=null && urlBelongSameHost(urlReferer,urlHref)) return true;
		if (hostAliases!=null) {
			for (int i=0; i<hostAliases.size(); i++) {
				hostAliases.set(i,hostAliases.get(i).trim());
				if (hostAliases.get(i).indexOf("*")==-1) {
					if (urlBelongSameHost(hostAliases.get(i), urlHref)) return true;					
				} else {
					String alias = hostAliases.get(i).replace("*", "");
					if (hostAliases.get(i).indexOf("*")==0) {
						if (urlHref.endsWith(alias)) return true;
					}
					if (hostAliases.get(i).indexOf("*")==hostAliases.get(i).length()-1) {
						if (urlHref.startsWith(alias)) return true;						
					}
				}
			}
		}
		return false;
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static boolean urlBelongSameHost(String urlReferer, String urlHref) {
		return areSameHosts(getUrlHost(urlReferer), getUrlHost(urlHref));
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private static boolean areSameHosts(String hostR, String hostH) {

		String host1 = hostR.toLowerCase().trim();
		String host2 = hostH.toLowerCase().trim();

		if (host1.startsWith("www.") && !host2.startsWith("www."))
			host2 = "www." + host2;

		if (!host1.startsWith("www.") && host2.startsWith("www."))
			host1 = "www." + host1;

		return host1.equals(host2);
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private static String getUrlHost(String url) {
		try {
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = "http://" + url;
			}
			URL u = new URL(url);
			return u.getHost();
		}
		catch (Exception e) {
			return "";
		}		
	}

	/**
	 * Encode url
	 * 
	 * @param url url to be encoded
	 * @return 
	 */
	public static String urlEncode (String url)
	{
		try {
			URL u = new URL(url);
			String host = u.getHost();
			int indexFile = url.indexOf("/", url.indexOf(host));
			if (indexFile==-1) return url;

			String urlFile = u.getFile();
			urlFile = URLDecoder.decode(urlFile, "UTF-8");

			String protocol = u.getProtocol();
			int port = u.getPort();
			if (port!=-1 && port!=80 && "http".equals(protocol))
				host += ":" .concat(String.valueOf(port));
			if (port!=-1 && port!=443 && "https".equals(protocol))
				host += ":" .concat(String.valueOf(port));

			URI uri = new URI(u.getProtocol(), host, urlFile, null);
			String ret = uri.toASCIIString();
			ret = ret.replaceAll("%3F", "?");
			return ret;		   
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static HashMap<String, String> extractMetas(String rawPage) throws IOException {

		final HashMap<String, String> m = new HashMap<String, String>();

		HtmlCleaner cleaner = new HtmlCleaner();
		//CleanerProperties props = cleaner.getProperties();		 
		//props.setXXX(...);
		TagNode node = cleaner.clean(rawPage);
		TagNode[] myNodes;

		// <meta name="..." content="..." />
		// <meta http-equiv="refresh" content=
		myNodes = node.getElementsByName("meta", true);
		for (int i=0;i<myNodes.length;i++)
		{
			String name = myNodes[i].getAttributeByName("name");
			if (name!=null)
			{
				String scheme = myNodes[i].getAttributeByName("scheme");
				if (scheme!=null)
					name += "_" + scheme;

				String content = myNodes[i].getAttributeByName("content");
				if (content!=null && !"".equals(content))
				{
					m.put("meta_" + name.toLowerCase().replaceAll("\\-", "_"), content);
				}
			}

			String equiv = myNodes[i].getAttributeByName("http-equiv");
			if (equiv!=null)
			{
				String content = myNodes[i].getAttributeByName("content");
				if (content!=null && !"".equals(content))
				{
					m.put("meta_equiv_" + equiv.toLowerCase().replaceAll("\\-", "_"), content);
				}
			}
		}

		// <link ... />
		myNodes = node.getElementsByName("link", true);
		for (int i=0;i<myNodes.length;i++)
		{
			String href = myNodes[i].getAttributeByName("href");
			String rel = myNodes[i].getAttributeByName("rel");
			if (href!=null && rel!=null && "canonical".equals(rel))
			{
				m.put("meta_link_canonical", href);
			}            
		}
		return m;
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static boolean isRelativeURL(String urlHref) {
		if (urlHref.equals("")) return false;
		
		// Case 1 : urlHref starts with "http://"
		if (urlHref.startsWith("http://") || urlHref.startsWith("https://")) {
			return false;
		}
		
		// Case 2 : urlHref looks like "?..."
		if (urlHref.startsWith("?")) {
			return false;
		}

		// Case 3 : urlHref looks like "/path/file.html..."
		if (urlHref.startsWith("/")) {
			return false;
		}

		return true;
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static String urlGetAbsoluteURL(String urlReferer, String urlHref) {
		try {
			if (urlHref.equals(""))
				return "";

			// Case 1 : urlHref starts with "http://"
			if (urlHref.startsWith("http://") || urlHref.startsWith("https://")) {
				return urlHref;
			}

			URL url = new URL(urlReferer);

			// Case 1.1 : urlHref starts with "//"
			if (urlHref.startsWith("//")) {
				return url.getProtocol() + ":" + urlHref;
			}

			String urlRefererHost = url.getProtocol() + "://" + url.getHost();
			if (url.getPort() != -1) {
				urlRefererHost = urlRefererHost + ":" + String.valueOf(url.getPort());
			}

			// Case 2 : urlHref looks like "?..."
			if (urlHref.startsWith("?")) {
				// find "?" in urlReferer
				/*
				if (urlReferer.indexOf("?")!=-1)
					return urlReferer.substring(0,urlReferer.indexOf("?")) + urlHref;
				else
					return urlReferer + urlHref;
				 */  
				return urlRefererHost + "/" + url.getPath() + urlHref;
			}

			// Case 3 : urlHref looks like "/path/file.html..."
			if (urlHref.startsWith("/")) {
				return urlRefererHost + urlHref;
			}

			// Case 4 : urlHref looks like "path/file.html..."
			String urlRefererPath = url.getPath();
			if ("".equals(urlRefererPath)) urlRefererPath = "/";

			//if (urlRefererPath.indexOf(".")==-1 && urlRefererPath.lastIndexOf("/") != urlRefererPath.length()-1)
			//	urlRefererPath = urlRefererPath + "/";

			int offset = urlRefererPath.lastIndexOf("/");
			/*
			if (offset <= 0) {
				urlRefererPath = "";
			} else {
				urlRefererPath = urlRefererPath.substring(0, offset);
			}
			 */
			urlRefererPath = urlRefererPath.substring(0, offset);

			return urlRefererHost + urlRefererPath + "/" + urlHref;

		}
		catch (Exception e) {
			//e.printStackTrace ();
		}
		return "";
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private static String fixUpUrl(String url)
	{
		String ret = url;
		if ("".equals(ret))
			ret = "/";
		else
		{
			if (ret.indexOf(".")>0)
			{
				ret = ret.substring(0,ret.lastIndexOf("/")+1);
			}
			else
			{
				if (!ret.endsWith("/"))
					ret += "/";		
			}				
		}
		return ret;
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static boolean isChildOf(URL urlChild, URL urlFather) 
	{
		String urlChildPath = fixUpUrl(urlChild.getPath().toLowerCase());
		String urlFatherPath = fixUpUrl(urlFather.getPath().toLowerCase());

		return urlChildPath.startsWith(urlFatherPath);
	}

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public static String getHtmlDeclaredLanguage(String rawData)
	{
		if (rawData==null || "".equals(rawData)) return "";

		Hashtable<String,Integer> langFreq = new Hashtable<String,Integer>();
		BufferedReader in = new BufferedReader(new StringReader(rawData));
		String line;
		try {
			while ((line = in.readLine()) != null)
			{
				line = line.toLowerCase();

				//<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="fr-fr">
				if (line.indexOf("<html")>=0 && line.toLowerCase().indexOf(" xml:lang")>=0)
				{
					String lang = parseAttributeValue(line, "xml:lang=");
					if (lang!=null && lang.length()>=2)
					{
						lang = lang.substring(0,2);

						if (langFreq.containsKey(lang))
							langFreq.put(lang, langFreq.get(lang) + 1);
						else
							langFreq.put(lang, 1);
					}
				}

				//<html lang="fr">
				if (line.indexOf("<html")>=0 && line.toLowerCase().indexOf(" lang")>=0)
				{
					String lang = parseAttributeValue(line, "lang=");
					if (lang!=null && lang.length()>=2)
					{
						lang = lang.substring(0,2);

						if (langFreq.containsKey(lang))
							langFreq.put(lang, langFreq.get(lang) + 1);
						else
							langFreq.put(lang, 1);
					}
				}

				//<meta http-equiv="content-language" content="fr-fr" />
				if (line.indexOf("<meta")>=0 && line.toLowerCase().indexOf(" http-equiv")>=0 && line.toLowerCase().indexOf("content-language")>=0 )
				{	
					String lang = parseAttributeValue(line, "content=");
					if (lang!=null && lang.length()>=2)
					{
						lang = lang.substring(0,2);

						if (langFreq.containsKey(lang))
							langFreq.put(lang, langFreq.get(lang) + 1);
						else
							langFreq.put(lang, 1);
					}
				}

				//<meta name="language" content="fr-fr" />
				if (line.indexOf("<meta")>=0 && line.toLowerCase().indexOf(" name")>=0 && line.toLowerCase().indexOf("language")>=0 && line.toLowerCase().indexOf(" content")>=0)
				{	
					String lang = parseAttributeValue(line, "content=");
					if (lang!=null && lang.length()>=2)
					{
						lang = lang.substring(0,2);

						if (langFreq.containsKey(lang))
							langFreq.put(lang, langFreq.get(lang) + 1);
						else
							langFreq.put(lang, 1);
					}
				}

				//<meta name="content-language" content="fr-fr" />
				if (line.indexOf("<meta")>=0 && line.toLowerCase().indexOf(" name")>=0 && line.toLowerCase().indexOf("content-language")>=0 && line.toLowerCase().indexOf(" content")>=0)
				{   
					String lang = parseAttributeValue(line, "content=");
					if (lang!=null && lang.length()>=2)
					{
						lang = lang.substring(0,2);

						if (langFreq.containsKey(lang))
							langFreq.put(lang, langFreq.get(lang) + 1);
						else
							langFreq.put(lang, 1);
					}
				}    
			}

			// Get the best candidate
			Vector<String> v = new Vector<String>(langFreq.keySet());
			Iterator<String> it = v.iterator();
			int max = 0;
			String lang = "";
			while (it.hasNext()) {
				String element =  (String)it.next();
				//System.out.println( element + " " + encodingFreq.get(element));
				if (langFreq.get(element)>max)
				{
					max = langFreq.get(element);
					lang = element;
				}
			}

			return lang;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Parse the character encoding from the specified content type header.
	 * If the content type is null, or there is no explicit character encoding,
	 * <code>null</code> is returned.
	 * <br />
	 * This method was copied from org.apache.catalina.util.RequestUtil,
	 * which is licensed under the Apache License, Version 2.0 (the "License").
	 *
	 * @param contentType a content type header
	 */
	public static String parseCharacterEncoding(String contentType) {

		if (contentType == null)
			return (null);

		String value = "";

		int start = contentType.indexOf("charset='");
		if (start >= 0)
		{
			value = contentType.substring(start + 9);
		}
		else
		{
			start = contentType.indexOf("charset=\"");
			if (start >= 0)
			{
				value = contentType.substring(start + 9);
			}
			else
			{
				start = contentType.indexOf("charset=");
				if (start < 0)
					return (null);
				value = contentType.substring(start + 8);
			}
		}		

		int end = value.indexOf(';');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('"');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('\'');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('/');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('>');
		if (end >= 0)
			value = value.substring(0, end);

		value = value.replaceAll("\"","");
		value = value.replaceAll("'","");

		return (value.trim());
	}

	public static String parseAttributeValue(String line, String attName) {
		if (line == null)
			return (null);
		int start = line.indexOf(attName);
		if (start < 0)
			return (null);
		String value = line.substring(start + attName.length());
		value = value.trim();

		if (value.charAt(0)=='"' || value.charAt(0)=='\'')
			value = value.substring(1);

		int end = value.indexOf(';');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('"');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('\'');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('/');
		if (end >= 0)
			value = value.substring(0, end);

		end = value.indexOf('>');
		if (end >= 0)
			value = value.substring(0, end);

		value = value.replaceAll("\"","");
		value = value.replaceAll("'","");

		return (value.trim());
	}	

	public static String filtreEncoding(String encoding)
	{
		encoding = encoding.toLowerCase();
		if (encoding.startsWith("utf") && !"utf-8".equals(encoding))
			return "";

		return encoding;
	}

	public static List<String> extractLinksFromFeed(String rawPage)
	{
		final ArrayList<String> list = new ArrayList<String>();

		try
		{
			XmlReader xmlReader = new XmlReader(new ByteArrayInputStream(rawPage.getBytes()));
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(xmlReader);

			Object[] arraySyndEntry = feed.getEntries().toArray();
			for (int k=arraySyndEntry.length-1; k>=0; k--)
			{
				SyndEntryImpl syndEntry = (SyndEntryImpl)arraySyndEntry[k];
				String link = strLinkCleanup(syndEntry.getLink());
				if (!list.contains(link))
					list.add(link);
			}
			return list;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	static private String strLinkCleanup(String str) {
		if (str == null) return "";

		// line start and end
		str = str.replaceAll("^[\\n\\t\\s]*", "");
		str = str.replaceAll("[\\n\\t\\s]*$", "");

		// some unicode chars
		str = str.replaceAll("\\u0091", "'");
		str = str.replaceAll("\\u0092", "'");
		str = str.replaceAll("\\u0093", "\"");
		str = str.replaceAll("\\u0094", "\"");

		return str;
	}

	public static List<String> extractAbsoluteLinks(String rawPage, String urlPage, int depth) throws IOException {

		List<String> links = extractLinks(rawPage, depth);
		String baseHref = null;

		for (int i=0; i<links.size(); i++) {
			try {
				String url = null;
				if (baseHref==null && isRelativeURL(links.get(i).trim())) baseHref = getBaseHref(rawPage);
				if (baseHref!=null && isRelativeURL(links.get(i).trim())) {
					url = urlGetAbsoluteURL(baseHref, links.get(i).trim());
				} else {
					url = urlGetAbsoluteURL(urlPage, links.get(i).trim());
				}
				links.set(i, url);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return links;
	}

	public static String getBaseHref(String rawPage) throws IOException {
		if (rawPage==null || !StringUtils.containsIgnoreCase(rawPage, "<base")) return null;

		HtmlCleaner cleaner = new HtmlCleaner();
		//CleanerProperties props = cleaner.getProperties();		 
		//props.setXXX(...);
		TagNode node = cleaner.clean(rawPage);
		TagNode[] myNodes = node.getElementsByName("base", true);
		if (myNodes==null || myNodes.length==0) return null;
		String href = myNodes[0].getAttributeByName("href");
		if (href!=null) return href;
		return null;
	}
	/**
	 * Extract link in html string according to depth parameter
	 * if depth = 0 : extract only redirection or iframe or framset urls links 
	 * if depth = 1 : extract only standard urls links (<a href='..'>
	 * if depth = 2 : extract all links
	 * 
	 * @param rawPage the input html string
	 * @param depth the type of links to be extracted
	 * @return the extracted urls in a String List
	 * @throws IOException
	 */
	public static List<String> extractLinks(String rawPage, int depth) throws IOException {

		final ArrayList<String> list = new ArrayList<String>();

		HtmlCleaner cleaner = new HtmlCleaner();
		//CleanerProperties props = cleaner.getProperties();		 
		//props.setXXX(...);
		TagNode node = cleaner.clean(rawPage);

		TagNode[] myNodes;

		if (depth==1 || depth==2) {
			// <a href=
			myNodes = node.getElementsByName("a", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String link = myNodes[i].getAttributeByName("href");

				if (link!=null) {
					link = link.trim();

					if (link!=null && !"".equals(link))
					{
						if (isValidUrl(link))
							if (!list.contains(link))
								list.add(link);
					}
				}
			}

			// <area href=
			myNodes = node.getElementsByName("area", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String link = myNodes[i].getAttributeByName("href");
				if (link!=null && !"".equals(link))
					if (isValidUrl(link))
						if (!list.contains(link))
							list.add(link);
			}
		}

		if (depth==0 || depth==2) {
			// <frame src=
			myNodes = node.getElementsByName("frame", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String link = myNodes[i].getAttributeByName("src");
				if (link!=null && !"".equals(link))
					if (isValidUrl(link))
						if (!list.contains(link))
							list.add(link);
			}

			// <iframe src=
			myNodes = node.getElementsByName("iframe", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String link = myNodes[i].getAttributeByName("src");
				if (link!=null && !"".equals(link))
					if (isValidUrl(link))
						if (!list.contains(link))
							list.add(link);
			}

			// <meta http-equiv="refresh" content=
			myNodes = node.getElementsByName("meta", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String equiv = myNodes[i].getAttributeByName("http-equiv");
				if ((equiv!=null) && (equiv.equalsIgnoreCase("refresh")))
				{
					String link = myNodes[i].getAttributeByName("content");
					if (link!=null && !"".equals(link))
					{
						if (link.indexOf("=")>0)
						{
							link = link.substring(link.indexOf("=")+1);
							if (!list.contains(link))
								list.add(link);
						}
					}
				}
			}

			// Look for embeded flash 
			// <param name="movie" value="..."
			myNodes = node.getElementsByName("param", true);
			for (int i=0;i<myNodes.length;i++)
			{
				String name = myNodes[i].getAttributeByName("name");
				if ("movie".equals(name))
				{
					String link = myNodes[i].getAttributeByName("value");
					if (!list.contains(link))
						list.add(link);
				}
			}
		}

		// <frame src= (par Jericho parser car HTML Cleaner echoue)
		MicrosoftConditionalCommentTagTypes.register();
		PHPTagTypes.register();
		PHPTagTypes.PHP_SHORT.deregister(); // remove PHP short tags for this example otherwise they override processing instructions
		MasonTagTypes.register();
		Source source=new Source(rawPage);
		source.fullSequentialParse();

		if (depth==0 || depth==2) {
			List<Element> linkElements=source.getAllElements(HTMLElementName.FRAME);
			for (Element linkElement : linkElements) {
				String link=linkElement.getAttributeValue("src");
				if (link!=null && !"".equals(link))
					if (isValidUrl(link))
						if (!list.contains(link))
							list.add(link);
			}
		}
		if (depth==1 || depth==2) {

			List<Element> linkElements=source.getAllElements(HTMLElementName.A);
			for (Element linkElement : linkElements) {
				String link=linkElement.getAttributeValue("href");
				if (link!=null && !"".equals(link))
					if (isValidUrl(link))
						if (!list.contains(link))
							list.add(link);
				/*
			if (href==null) continue;
			// A element can contain other tags so need to extract the text from it:
			String label=linkElement.getContent().getTextExtractor().toString();
			System.out.println(label+" <"+href+'>');
				 */
			}
		}

		String strPattern = "location[.]href=['\"](.*)['\"]";
		Pattern pattern = Pattern.compile(strPattern);
		Matcher matcher = pattern.matcher(rawPage);
		while (matcher.find()) {
			try{
				String url = matcher.group(1);
				if (url.indexOf("'")!=-1)
					url = url.substring(0, url.indexOf("'"));
				if (url.indexOf('"')!=-1)
					url = url.substring(0, url.indexOf('"'));
				if (!list.contains(url))
					list.add(url);        		
			}
			catch (Exception e){}
		}

		// Look for location.href='...'
		//		strPattern = "href=['\"](.*)['\"]";
		//		pattern = Pattern.compile(strPattern);
		//		matcher = pattern.matcher(rawPage);
		//		while (matcher.find()) {
		//			try{
		//				String url = matcher.group(1);
		//				if (url.indexOf("'")!=-1)
		//					url = url.substring(0, url.indexOf("'"));
		//				if (url.indexOf('"')!=-1)
		//					url = url.substring(0, url.indexOf('"'));
		//				if (!list.contains(url))
		//					list.add(url);        		
		//			}
		//			catch (Exception e){}
		//		}

		if (depth==0 || depth==2) {
			// Look for location.replace("...")
			strPattern = "location[.]replace\\(['\"](.*)['\"]\\)";
			pattern = Pattern.compile(strPattern);
			matcher = pattern.matcher(rawPage);
			while (matcher.find()) {
				try{
					String url = matcher.group(1);
					if (url.indexOf("'")!=-1)
						url = url.substring(0, url.indexOf("'"));
					if (url.indexOf('"')!=-1)
						url = url.substring(0, url.indexOf('"'));
					if (!list.contains(url))
						list.add(url);        		
				}
				catch (Exception e){}
			}

			// Look for window.location='...'
			strPattern = "window[.]location=['\"](.*)['\"]";
			pattern = Pattern.compile(strPattern);
			matcher = pattern.matcher(rawPage);
			while (matcher.find()) {
				try{
					String url = matcher.group(1);
					if (url.indexOf("'")!=-1)
						url = url.substring(0, url.indexOf("'"));
					if (url.indexOf('"')!=-1)
						url = url.substring(0, url.indexOf('"'));
					if (!list.contains(url))
						list.add(url);        		
				}
				catch (Exception e){}
			}
		}

		return list;
	}

	private static boolean isValidUrl(String url) {
		String temp = url.toLowerCase();
		if (!temp.startsWith("mailto:") && !temp.startsWith("javascript:") && !temp.startsWith("#") && !temp.startsWith("\\") && !temp.startsWith("'") && !temp.startsWith("\"")) {
			if (temp.startsWith("http")) {
				try {
					if (temp.startsWith("http:/") && !temp.startsWith("http://")) return false;
					@SuppressWarnings("unused")
					URL u = new URL(temp);
					return true;
				}
				catch (Exception e){
					return false;
				}
			}
			return true;
		}
		else return false;
	}
	
    public static String urlGetFileName(String url) {
        try
        {		
            URL u = new URL(url);
            String name = u.getPath();
            if (name.lastIndexOf("/")!=-1 && name.lastIndexOf("/") < name.length())
                name = name.substring(name.lastIndexOf("/")+1);
            return name;
        }
        catch (Exception e) {}
        return "";
    }
    
    public static String urlAddBasicAuthentication(String url, String login, String password) {
		url = url.replace("http://", "http://"+login+":"+password+"@");
		url = url.replace("https://", "https://"+login+":"+password+"@");
		return url;
    }

}
