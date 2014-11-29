package fr.eolya.crawlerws;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.annotation.Timed;

import fr.eolya.utils.Base64;
import fr.eolya.utils.http.HttpLoader;
import fr.eolya.utils.http.HttpStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


@Path("/testauthentication")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlerWsResourcesTestAuthentication extends CrawlerWsResources {
	
    public CrawlerWsResourcesTestAuthentication(CrawlerWsConfiguration conf) {
		super(conf);
	}

	@GET
    @Timed
    public CrawlerWsSayingTestAuthentication doGet(@QueryParam("page") String page, 
    		@QueryParam("auth_mode") String authMode, 
    		@QueryParam("auth_login") String authLogin, 
    		@QueryParam("auth_passwd") String authPasswd, 
    		@QueryParam("auth_param") String authParam) {
		
        Map<String, String> authCookies = null;
        Map<String, String> authBasicLogin = null;
        
        String userAgent = conf.getUserAgent();
        
        try {
        	HttpLoader urlLoader = new HttpLoader();
            urlLoader.setUserAgent(userAgent);
            
            if (!"0".equals(authMode)) {
                if ("3".equals(authMode)) {
					authBasicLogin = new HashMap<String, String>();
					authBasicLogin.put("login",authLogin);
					authBasicLogin.put("password",authPasswd);		
                    urlLoader.setBasicLogin(authBasicLogin);
                } else {
                    authCookies = HttpLoader.getAuthCookies(Integer.parseInt(authMode), authLogin, authPasswd, authParam,
                            conf.getProxyHost(), conf.getProxyPort(), conf.getProxyExclude(), conf.getProxyUserName(), conf.getProxyPassword());                	
                    if (authCookies!=null)
                        urlLoader.setCookies(authCookies);
                    else
                    	return new CrawlerWsSayingTestAuthentication(0, "", 10, "Unable to authentify");
                }
            }
            
            if (urlLoader.open(page) == HttpLoader.LOAD_SUCCESS) {
                String contentType = urlLoader.getContentType();
                String contentEncoding = urlLoader.getContentEncoding();
                if ((contentType!=null) && (contentType.toLowerCase().startsWith("text/html"))) {
					HttpStream ws = new HttpStream(urlLoader.getStream(), "", contentType, contentEncoding);
                    String rawPage = ws.getString();
                    ws.clear();
                    try {
                    	return new CrawlerWsSayingTestAuthentication(1, Base64.stringToStringBase64(rawPage), 0, "");
                    }
                    catch(Exception e) {}
                }
                else {
                    if (contentType!=null)
                    	return new CrawlerWsSayingTestAuthentication(0, "", 10, "Failed load page (content-type = " + contentType + ")");
                    else
                    	return new CrawlerWsSayingTestAuthentication(0, "", 10, "Failed load page (no content-type)");
                }
            }
            else {
            	return new CrawlerWsSayingTestAuthentication(0, "", 10, "Failed load page (response code = " + String.valueOf(urlLoader.errorCode));
            }
            urlLoader.close();
        } catch (Exception e) {
            e.printStackTrace();
        	return new CrawlerWsSayingTestAuthentication(0, "", 10, e.getMessage());
        }
    	return new CrawlerWsSayingTestAuthentication(0, "", 10, "Unknown error");
    }
}