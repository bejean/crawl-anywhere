package fr.eolya.utils.http;

import java.net.*;

/**
 * An url utilities class
 */
public class URLUtils {

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

}
