package fr.eolya.crawler.ws;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.WebAuthSession;

import fr.eolya.extraction.tika.TikaWrapper;
import fr.eolya.utils.*;
import fr.eolya.utils.http.HttpLoader;
import fr.eolya.utils.http.HttpStream;
import fr.eolya.utils.http.HttpUtils;
import fr.eolya.utils.servlet.*;


public class CrawlerWS extends HttpServlet {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private static final String MIME_TYPE = "text/xml; charset=utf-8";
    
    //	public Properties props = null;
    
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }
    
    public void init (ServletConfig config) throws ServletException {
        super.init (config);
        
        ServletContext context= config.getServletContext();
        if (context.getAttribute("DropboxContext") == null) {
            //HashMap<String,DropboxContext> dropboxContext = new HashMap<String,DropboxContext>();
            ArrayList<DropboxContext> dropboxContext = new ArrayList<DropboxContext>();
            context.setAttribute("DropboxContext", dropboxContext);
        }
    }
    
    public void destroy() {}    
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        
        XMLConfig xmlConfig = ServletUtils.loadXmlConfig("webapps/crawlerws/crawlerws.xml");
        
        String action = "";
        try {
            action = StringUtils.trimToEmpty(req.getParameter("action")).trim();
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (action.equals("")) {
            sendRes(res, XmlResponse.buildErrorXml(1, "Missing action"));
            return;
        }
        if (!action.equals("testauthentication") && !action.equals("testfilteringrules") && !action.equals("testcleaning") && !action.equals("dropboxlinkstep1") && !action.equals("dropboxlinkstep2") && !action.equals("dropboxgetfile")){
            sendRes(res, XmlResponse.buildErrorXml(2, "Wrong action"));
            return;
        }
        
        String xml = "";
        if (action.equals("testauthentication"))
            xml = doTestAuthentication(req, res, xmlConfig);
        
        if (action.equals("testfilteringrules"))
            xml = doTestFilteringRules(req, res, xmlConfig);
        
        if (action.equals("testcleaning"))
            xml = doTestCleaning(req, res, xmlConfig);
        
        if (action.equals("dropboxlinkstep1"))
            xml = doDropboxLinkStep1(req, res, xmlConfig);
        
        if (action.equals("dropboxlinkstep2"))
            xml = doDropboxLinkStep2(req, res, xmlConfig);
        
        if (action.equals("dropboxgetfile"))
            xml = doDropboxGetFile(req, res, xmlConfig);
        
        if (xml!=null) sendRes(res, xml);
    }
    
    ////////////////////////////////////////////////////////////////////:
    // 
    ////////////////////////////////////////////////////////////////////:
    protected String doTestAuthentication (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        String page = StringUtils.trimToEmpty(req.getParameter("page")).trim();
        String authMode = StringUtils.trimToEmpty(req.getParameter("auth_mode")).trim();
        String authLogin = StringUtils.trimToEmpty(req.getParameter("auth_login")).trim();
        String authPasswd = StringUtils.trimToEmpty(req.getParameter("auth_passwd")).trim();
        String authParam = StringUtils.trimToEmpty(req.getParameter("auth_param")).trim();
        Map<String, String> authCookies;
        
        try {
        	HttpLoader urlLoader;
			//try {
				urlLoader = new HttpLoader();
			//} catch (URISyntaxException e1) {
			//	e1.printStackTrace();
            //    return XmlResponse.buildErrorXml(10, "Failed load page (bad url : " + page + ")");
			//}
            
            String userAgent = ServletUtils.getSetting(this, xmlConfig, "crawler_user_agent", "CaBot");
            urlLoader.setUserAgent(userAgent);
            
            authCookies = HttpUtils.getAuthCookies(Integer.parseInt(authMode), authLogin, authPasswd, authParam,
                            ServletUtils.getSetting(this, xmlConfig, "proxy_host", ""), ServletUtils.getSetting(this, xmlConfig, "proxy_port", ""), ServletUtils.getSetting(this, xmlConfig, "proxy_exclude", ""), ServletUtils.getSetting(this, xmlConfig, "proxy_username", ""), ServletUtils.getSetting(this, xmlConfig, "proxy_password", ""));
            
            if (authCookies!=null)
                urlLoader.setCookies(authCookies);
            else
                return XmlResponse.buildErrorXml(10, "Failed get authentication cookie");
            
            if (urlLoader.open(page) == HttpLoader.LOAD_SUCCESS) {
                String contentType = urlLoader.getContentType();
                String contentEncoding = urlLoader.getContentEncoding();
                if ((contentType!=null) && (contentType.toLowerCase().startsWith("text/html"))) {
					HttpStream ws = new HttpStream(urlLoader.getStream(), "", contentType, contentEncoding);
                    String rawPage = ws.getString();
                    ws.clear();
                    try {
                        String ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
                        ret += "<page><![CDATA[" + rawPage + "]]>" + "</page></result>";
                        urlLoader.close();
                        return ret;
                    }
                    catch(Exception e) {}
                }
                else {
                    if (contentType!=null)
                        return XmlResponse.buildErrorXml(10, "Failed load page (content-type = " + contentType + ")");
                    else
                        return XmlResponse.buildErrorXml(10, "Failed load page (no content-type)");
                    
                }
            }
            else {
                return XmlResponse.buildErrorXml(10, "Failed load page (response code = " + String.valueOf(urlLoader.errorCode));
            }
            urlLoader.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return XmlResponse.buildErrorXml(10, "Failed");
    }
    
    ////////////////////////////////////////////////////////////////////:
    // 
    ////////////////////////////////////////////////////////////////////:
    protected String doTestFilteringRules (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        String page = StringUtils.trimToEmpty(req.getParameter("page")).trim();
        String rules = StringUtils.trimToEmpty(req.getParameter("rules")).trim();
        
        if (!"".equals(page)) {
            try {
                String ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
                ret += "<status>";
                ret += CrawlerUtilsCommon.getUrlMode(page, rules, "a");
                ret += "</status></result>";
                return ret;
            }
            catch(Exception e) {}
        }
        return XmlResponse.buildErrorXml(10, "Missing page");
    }
    
    ////////////////////////////////////////////////////////////////////:
    // 
    ////////////////////////////////////////////////////////////////////:
    protected String doTestCleaning (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        String page = StringUtils.trimToEmpty(req.getParameter("page")).trim();
        java.net.URL url = null;
        
        try {
            url = new java.net.URL(page);
        }
        catch (Exception e) {
            return XmlResponse.buildErrorXml(-1, "Invalid URL");
        }
        
        try {
            //MultiFormatTextExtractor extractor = new MultiFormatTextExtractor();
        	HttpLoader urlLoader;
			//try {
				urlLoader = new HttpLoader();
			//} catch (URISyntaxException e1) {
			//	e1.printStackTrace();
            //    return XmlResponse.buildErrorXml(10, "Failed load page (bad url : " + page + ")");
			//}
 
            if (urlLoader.open(url.toExternalForm()) == HttpLoader.LOAD_SUCCESS) {
                String ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
                
                //String contentType = urlLoader.getContentType();
                //String contentEncoding = urlLoader.getContentEncoding();
                
                //HttpStream ws = new HttpStream(urlLoader.getStream(), "", contentType, contentEncoding);
                //String data = ws.getString();
                //ws.clear();
                
                //String rawPage = extractor.htmlPageToText(data, page, "");
                //String title = extractor.getTitle();
				TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_HTML);
				tikaWrapper.process(urlLoader.getStream());
				String rawPage = tikaWrapper.getText();
				String title = tikaWrapper.getMetaTitle();

                ret += "<page_0><![CDATA[" + rawPage + "]]>" + "</page_0>";
                ret += "<title_0><![CDATA[" + title + "]]>" + "</title_0>";
                
                //rawPage = extractor.htmlPageToText(data, page, "boilerpipe_article");
                //title = extractor.getTitle();
				tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_ARTICLE, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream());
				rawPage = tikaWrapper.getText();
				title = tikaWrapper.getMetaTitle();
                ret += "<page_1><![CDATA[" + rawPage + "]]>" + "</page_1>";
                ret += "<title_1><![CDATA[" + title + "]]>" + "</title_1>";
 
                //rawPage = extractor.htmlPageToText(data, page, "boilerpipe_default");
                //title = extractor.getTitle();
				tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_DEFAULT, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream());
				rawPage = tikaWrapper.getText();
				title = tikaWrapper.getMetaTitle();
                ret += "<page_2><![CDATA[" + rawPage + "]]>" + "</page_2>";
                ret += "<title_2><![CDATA[" + title + "]]>" + "</title_2>";
 
                //rawPage = extractor.htmlPageToText(data, page, "boilerpipe_canola");
                //title = extractor.getTitle();
				tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_CANOLA, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream());
				rawPage = tikaWrapper.getText();
				title = tikaWrapper.getMetaTitle();
                ret += "<page_3><![CDATA[" + rawPage + "]]>" + "</page_3>";
                ret += "<title_3><![CDATA[" + title + "]]>" + "</title_3>";

                //rawPage = extractor.htmlPageToText(data, page, "snacktory");
                //title = extractor.getTitle();
				tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_SNACKTORY, TikaWrapper.CONTENT_TYPE_HTML);
				tikaWrapper.process(urlLoader.getStream());
				rawPage = tikaWrapper.getText();
				title = tikaWrapper.getMetaTitle();
                ret += "<page_4><![CDATA[" + rawPage + "]]>" + "</page_4>";
                ret += "<title_4><![CDATA[" + title + "]]>" + "</title_4>";
                
                ret += "</result>";
                urlLoader.close();
                return ret;
            } else {
                return XmlResponse.buildErrorXml(-1, "Error loading page");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return XmlResponse.buildErrorXml(-1, "Cleaning error");
    }
    
    protected String doDropboxLinkStep1 (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        
        String appKey = ServletUtils.getSetting(this, xmlConfig, "dropbox_appkey", "");
        String appSecret = ServletUtils.getSetting(this, xmlConfig, "dropbox_appsecret", "");
        String callbackUrl =  StringUtils.trimToEmpty(req.getParameter("callback")).trim();
        
        if ("".equals(appKey) || "".equals(appSecret)) return XmlResponse.buildErrorXml(-1, "Missing parameters (Dropbox appKey and appSecret)");
        if ("".equals(callbackUrl)) return XmlResponse.buildErrorXml(-1, "Missing parameters (Dropbox call back url)");
        
        ServletContext context=getServletContext();
        
        long tsNow = new Date().getTime();
        
        synchronized(this) {
            @SuppressWarnings("unchecked")
            //HashMap<String,DropboxContext> dropboxContextList = (HashMap<String, DropboxContext>) context.getAttribute("DropboxContext");
            ArrayList<DropboxContext> dropboxContextList = (ArrayList<DropboxContext>) context.getAttribute("DropboxContext");
            if (dropboxContextList == null) return XmlResponse.buildErrorXml(-1, "No Dropbox context");
            
            //dropboxContextGet(dropboxContextList, tsNow, null);
            
            //		Iterator<String> iterator = dropboxContextList.keySet().iterator();
            //		while(iterator.hasNext()){   
            //			String key = (String) iterator.next();
            //			long ts = Long.parseLong(key.substring(key.indexOf("-")+1));
            //			if ((tsNow-ts) > 15*60*1000) dropboxContextList.remove(key);
            //		}
            
            AppKeyPair appKeyPair;
            appKeyPair = new AppKeyPair(appKey, appSecret);
            WebAuthSession was = new WebAuthSession(appKeyPair, Session.AccessType.DROPBOX);
            try {
                WebAuthSession.WebAuthInfo info = was.getAuthInfo(callbackUrl);
                
                DropboxContext dropboxContext = new DropboxContext(appKey + "-" + String.valueOf(tsNow), was, info);
                //dropboxContextList.put(appKey + "-" + String.valueOf(tsNow), dropboxContext);
                dropboxContextList.add(dropboxContext);
                
                String ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
                ret += "<errno>0</errno>";
                ret += "<info_url><![CDATA[" + info.url + "]]>" + "</info_url>";
                ret += "<timestamp><![CDATA[" +String.valueOf(tsNow) + "]]>" + "</timestamp>";
                ret += "</result>";
                return ret;			
                
            } catch (DropboxException e) {
                e.printStackTrace();
                return XmlResponse.buildErrorXml(-1, "Dropbox error");
            }
        }
    }
    
    protected String doDropboxLinkStep2 (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        
        String timestamp = StringUtils.trimToEmpty(req.getParameter("timestamp")).trim();
        
        String appKey = ServletUtils.getSetting(this, xmlConfig, "dropbox_appkey", "");
        if ("".equals(appKey)) return XmlResponse.buildErrorXml(-1, "Missing parameters");
        
        WebAuthSession was = null;
        WebAuthSession.WebAuthInfo info = null;
        
        ServletContext context=getServletContext();
        DropboxContext dropboxContext = null;
        synchronized(this) {
            @SuppressWarnings("unchecked")
            //HashMap<String,DropboxContext> dropboxContextList = (HashMap<String, DropboxContext>) context.getAttribute("DropboxContext");
            ArrayList<DropboxContext> dropboxContextList = (ArrayList<DropboxContext>) context.getAttribute("DropboxContext");
            if (dropboxContextList == null) return XmlResponse.buildErrorXml(-1, "No Dropbox context");
            dropboxContext = dropboxContextGet(dropboxContextList, 0, appKey+"-"+timestamp);
            if (dropboxContext == null) return XmlResponse.buildErrorXml(-1, "No Dropbox context");
        }
        
        
        //		Iterator<String> iterator = dropboxContextList.keySet().iterator();
        //
        //		while(iterator.hasNext()){   
        //			String key = (String) iterator.next();
        //			if (key.equals(appKey+"-"+timestamp)) {
        //				was = dropboxContextList.get(key).was;
        //				info = dropboxContextList.get(key).info;
        //				break;
        //			}
        //		}
        
        
        was = dropboxContext.was;
        info = dropboxContext.info;
        if (was == null || info == null) return XmlResponse.buildErrorXml(-1, "No Dropbox context");
        
        try {
            String userId = was.retrieveWebAccessToken(info.requestTokenPair);
            System.out.println("User ID: " + userId);
            System.out.println("Access Key: " + was.getAccessTokenPair().key);
            System.out.println("Access Secret " + was.getAccessTokenPair().secret);
            //DropboxAPI<WebAuthSession> api = new DropboxAPI<WebAuthSession>(was);
            //DeltaPage<Entry> deltaPage = api.delta("");
        } catch (DropboxException e) {
            e.printStackTrace();
            return XmlResponse.buildErrorXml(-1, "Dropbox unlinked");
        }
        
        String ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
        ret += "<errno>0</errno>";
        ret += "<token_key>" + was.getAccessTokenPair().key + "</token_key>";
        ret += "<token_secret>" + was.getAccessTokenPair().secret + "</token_secret></result>";
        return ret;			
    }
    
    private void sendRes(HttpServletResponse res, String xml) throws IOException {
        res.setContentType(MIME_TYPE);
        PrintWriter out = res.getWriter();
        out.println(xml);
        out.close();
    }
    
    protected String doDropboxGetFile (HttpServletRequest req, HttpServletResponse res, XMLConfig xmlConfig) {
        
        String appKey = ServletUtils.getSetting(this, xmlConfig, "dropbox_appkey");
        String appSecret = ServletUtils.getSetting(this, xmlConfig, "dropbox_appsecret");
        if ("".equals(appKey) || "".equals(appSecret)) return XmlResponse.buildErrorXml(-1, "Missing parameters");
        
        String tokenKey = ServletUtils.getSetting(this, xmlConfig, "dropbox_tokenkey", "");
        String tokenSecret = ServletUtils.getSetting(this, xmlConfig, "dropbox_tokensecret", "");
        
        if ("".equals(tokenKey) || "".equals(tokenSecret)) {
            tokenKey = StringUtils.trimToEmpty(req.getParameter("tokenkey")).trim();
            tokenSecret = StringUtils.trimToEmpty(req.getParameter("tokensecret")).trim();
        }
        
        String filePath = StringUtils.trimToEmpty(req.getParameter("filepath")).trim();
        
        AppKeyPair appKeyPair = new AppKeyPair(appKey, appSecret);
        WebAuthSession was = new WebAuthSession(appKeyPair, Session.AccessType.DROPBOX);
        AccessTokenPair accessToken = new AccessTokenPair(tokenKey, tokenSecret);
        was.setAccessTokenPair(accessToken);
        DropboxAPI<WebAuthSession> api = new DropboxAPI<WebAuthSession>(was);
        
        try {
            Entry meta = api.metadata(filePath, 1, null, false, null);
            DropboxInputStream streamData = api.getFileStream(filePath, null);
            
            try
            {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);		    	  
                int bytesread = 0;
                while(true){
                    bytesread = streamData.read(buffer);
                    if (bytesread == -1) break;
                    baos.write(buffer,0,bytesread);
                }
                
                String name = "";
                int offset = filePath.lastIndexOf("/");
                if (offset!=-1) name = filePath.substring(offset+1);
                
                res.setContentType(meta.mimeType);     
                if (!"".equals((name)))
                    res.setHeader("Content-Disposition", "attachment; filename=" + name);
                res.setHeader("Cache-Control", "no-cache");    
                res.setContentLength(baos.size());
                ServletOutputStream sos = res.getOutputStream();
                baos.writeTo(sos); 
                sos.flush();
                streamData.close();
                return null;
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                return XmlResponse.buildErrorXml(-1, "Dropbox error");
            }
        } catch (DropboxException e) {
            e.printStackTrace();
            return XmlResponse.buildErrorXml(-1, "Dropbox error");
        }
    }
    
    private class DropboxContext {
        public String key = null;
        public long ts = 0;
        public WebAuthSession was = null;
        public WebAuthSession.WebAuthInfo info = null;
        public DropboxContext(String key, WebAuthSession was, WebAuthSession.WebAuthInfo info) {
            this.key = key;
            this.was = was;
            this.info = info;
            this.ts = new Date().getTime();
        }
    }
    //ArrayList<DropboxContext>
    //private synchronized DropboxContext dropboxContextGet(HashMap<String,DropboxContext> dropboxContextList, long tsNow, String userKey) {
    private synchronized DropboxContext dropboxContextGet(ArrayList<DropboxContext> dropboxContextList, long tsNow, String userKey) {
        if (dropboxContextList.size()==0) return null;        
        if (tsNow>0) {
            for (int i=dropboxContextList.size()-1; i>=0; i--) {
                DropboxContext dc = dropboxContextList.get(i);
                if ((tsNow-dc.ts) > 15*60*1000) dropboxContextList.remove(i);
            }
            if (userKey==null) return null;
        }
        for (int i=0; i<dropboxContextList.size(); i++) {
            DropboxContext dc = dropboxContextList.get(i);
            if (dc.key.equals(userKey)) {
                return dc;
            }
        }
        //        Iterator<String> iterator = dropboxContextList.keySet().iterator();
        //        if (tsNow>0) {
        //            while(iterator.hasNext()) {   
        //                String key = (String) iterator.next();
        //                long ts = Long.parseLong(key.substring(key.indexOf("-")+1));
        //                if ((tsNow-ts) > 15*60*1000) dropboxContextList.remove(key);
        //            }
        //            if (userKey==null) return null;
        //            iterator = dropboxContextList.keySet().iterator();
        //        }
        //        
        //        while(iterator.hasNext()){   
        //            String key = (String) iterator.next();
        //            if (key.equals(userKey)) {
        //                return dropboxContextList.get(key);
        //            }
        //        }
        return null;
    }
}
