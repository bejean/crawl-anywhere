package fr.eolya.crawler.connectors.web;

import java.util.Arrays;
import java.util.List;

import fr.eolya.extraction.ScriptsWrapper;
import fr.eolya.utils.http.HttpUtils;

class Links {
	public List<String> links0 = null;
	public List<String> links1 = null;

	/** Default instantiation. */
	public Links(String rawPage, String url, boolean isFeed, String scriptName) {
		try {
			// TODO: V4
			if (isFeed) {
				links1 = HttpUtils.extractLinksFromFeed(rawPage);
			}
			else {                                                                                                                        
				links0 = HttpUtils.extractAbsoluteLinks(rawPage, url, 0);                       
				links1 = HttpUtils.extractAbsoluteLinks(rawPage, url, 1);

				// input links can be updated (replace, add or remove links) by the scripts
				String [] aLinks = ScriptsWrapper.htmlLinks(url, rawPage, links1.toArray(new String[]{}), null, scriptName);
				links1 = Arrays.asList(aLinks); 
			}
		} catch(Exception e) {
			links0 = null;
			links1 = null;
		}
	}

	public String[] getLinks0() {
		if (links0==null) return null;
		String[] strResult=new String[links0.size()];  
		links0.toArray(strResult);
		return strResult;
	}
	public String[] getLinks1() {
		if (links1==null) return null;
		String[] strResult=new String[links1.size()];  
		links1.toArray(strResult);
		return strResult;
	}  
}