package fr.eolya.crawler.connectors.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class StartingUrls {

	private ArrayList<StartingUrl> urls = null;
	private String urlHome = null;
	private boolean isOptimized = false;

	public StartingUrls(String urlXml) throws DocumentException, MalformedURLException {

		urlXml = urlXml.trim();

		if (urlXml.startsWith("<")) {

			isOptimized = true;

			Document document;
			document = DocumentHelper.parseText( urlXml );

			@SuppressWarnings("unchecked")
			Iterator<Element> iterUrls = document.selectNodes("//urls/url").iterator();
			while (iterUrls.hasNext()) {
				Element startingUrlElement = iterUrls.next();	
				Element url = (Element) startingUrlElement.selectSingleNode("url");
				Element mode = (Element) startingUrlElement.selectSingleNode("mode");
				Element onlyFirstCrawl = (Element) startingUrlElement.selectSingleNode("onlyfirstcrawl");
				Element allowOtherDomain = (Element) startingUrlElement.selectSingleNode("allowotherdomain");

				if ("s".equals(mode.getText()) && "0".equals(onlyFirstCrawl.getText()))
					isOptimized = false;

				String itemUrl = url.getText(); 
				String itemHome = ""; 
				String itemMode = mode.getText(); 

				if (urlHome==null) {
					URL tmpUrl = null;
					if ("r".equals(itemMode) && !"".equals(itemHome)) {
						tmpUrl = new URL(itemHome);
					} else {
						tmpUrl = new URL(itemUrl);
					}
					urlHome = tmpUrl.getProtocol() + "://" + tmpUrl.getAuthority() + "/";
				}

				//boolean bAllowOtherDomain = false;
				//if ("l".equals(mode.getText()) && "1".equals(allowOtherDomain.getText()))
				//	bAllowOtherDomain = true;
				boolean bAllowOtherDomain = "1".equals(allowOtherDomain.getText());

				StartingUrl startingUrl = new StartingUrl(itemUrl, itemMode, "1".equals(onlyFirstCrawl.getText()), bAllowOtherDomain);

				if (urls==null)
					urls = new ArrayList<StartingUrl>();
				urls.add(startingUrl);
			}
		}
		else {
			StartingUrl startingUrl = new StartingUrl(urlXml, "s", false, false);
			urls = new ArrayList<StartingUrl>();
			urls.add(startingUrl);
			URL tmpUrl = new URL(urlXml);
			urlHome = tmpUrl.getProtocol() + "://" + tmpUrl.getAuthority() + "/";
		}	
	}

	public boolean isOptimized() {
		return isOptimized;
	}

	public String getUrlHome() {
		return urlHome;
	}

	public void setUrlHome(String urlHome) {
		this.urlHome = urlHome;
	}

	public int size() {
		if (urls!=null)
			return urls.size();
		return 0;
	}

	public StartingUrl get(int index) {
		if (urls!=null)
			return urls.get(index);
		return null;
	}

	public boolean isNotIndexableStartingUrl(String url) {
		for (int i=0; i<urls.size(); i++) {
			String u = urls.get(i).url;
			String m = urls.get(i).mode;
			if (u.equals(url) && ("r".equals(m) ||"l".equals(m)))
				return true;
		}
		return false;
	}

	//	public static void main(String[] args) {
	//		String xml = "<urls><url><url>http://feeds.feedburner.com/everythingicafe</url><home>http://www.everythingicafe.com/</home><mode>r</mode><allowotherdomain>0</allowotherdomain><onlyfirstcrawl>0</onlyfirstcrawl></url></urls>";
	//		StartingUrls u;
	//		try {
	//			u = new StartingUrls(xml);
	//			for (int i=0; i<u.size(); i++) {
	//				System.out.println(u.get(i).url + " - " + u.get(i).mode + " - " + u.get(i).allowOtherDomain + " - " + u.get(i).onlyFirstCrawl);
	//			}
	//			System.out.println(u.getUrlHome());
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}
}
