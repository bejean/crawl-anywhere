package fr.eolya.utils;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class CrawlerUtilsCommon {
	public static String getUrlMode(String url, String filteringRules, String defaultMode) {

		/*
		 * a : push to index + extract links (default)
		 * s : skip
		 * g : push to index only
		 * l : extract links only
		 * o : once (skip if already crawled, else a)
		 */

		if (defaultMode==null || "".equals(defaultMode))
			defaultMode = "a";

		if (url == null || "".equals(url))
			return defaultMode;

		if (filteringRules == null || "".equals(filteringRules))
			return defaultMode;

		String[] aRules = null;
		if (filteringRules.startsWith("<")) {
			SAXReader reader = new SAXReader();
			reader.setValidation(false);
			try {
				Document document = reader.read(new StringReader(filteringRules));
				@SuppressWarnings("unchecked")
				List<Node> nodes = document.selectNodes("//rules/rule");
				if (nodes!=null && nodes.size()>0) {
					aRules = new String[nodes.size()];
					for (int i = 0; i<nodes.size(); i++) {
						Element rule = (Element) nodes.get(i);
						@SuppressWarnings("unchecked")
						List<Element> ruleItems = rule.elements();
						String ope = null;
						String mode = null;
						String pat = null;
						for (int j = 0; j<ruleItems.size(); j++) {
							Element item = ruleItems.get(j);
							if ("ope".equals(item.getName())) ope = item.getText();
							if ("mode".equals(item.getName())) mode = item.getText();
							if ("pat".equals(item.getName())) pat = item.getText();
						}
						if ("all".equals(mode)) mode = "a";
						if ("skip".equals(mode)) mode = "s";
						if ("once".equals(mode)) mode = "o";
						if ("links".equals(mode)) mode = "l";
						if ("get".equals(mode)) mode = "g";
						aRules[i] = ope + ":" + mode + ":" + pat;
					}
				}
				else
					return defaultMode;				
			} 
			catch (Exception e) {
				return defaultMode;
			}
		}
		else {
			aRules = filteringRules.split("\n");
		}

		try {	

			String path = "";
			try {
				URL u = new URL(url);
				path = u.getFile();
			}
			catch (MalformedURLException e) {
				path = url;
			}


			// dés qu'une url correspond à une regle "+", on retourne true
			// dés qu'une url correspond à une regle "-", on retourne false
			for (int i = 0; i < aRules.length; i++) {
				String rule = aRules[i].trim();
				if (rule.charAt(0) != '#') {

					if (rule.startsWith("+path:")) {
						String r = rule.substring(6);
						if (path.startsWith(r))
							return "a";
					}

					if (rule.startsWith("+match:")) {
						String r = rule.substring(7);
						Pattern p = Pattern.compile(r);
						Matcher m = p.matcher(path);
						if (m.find())
							return "a";
					}

					if (rule.startsWith("-path:")) {
						String r = rule.substring(6);
						if (path.startsWith(r))
							return "s";
					}

					if (rule.startsWith("-match:")) {
						String r = rule.substring(7);
						Pattern p = Pattern.compile(r);
						Matcher m = p.matcher(path);
						if (m.find())
							return "s";
					}

					// (match|path):(a|s|l|o):...
					Pattern p0 = Pattern.compile("^(match|path):(a|s|l|g|o):");
					Matcher m0 = p0.matcher(rule);
					if (m0.find()) {

						if (rule.startsWith("path:")) {
							String mode = rule.substring(5,6);
							String r = rule.substring(7);
							if (path.startsWith(r))
								return mode;
						}

						if (rule.startsWith("match:")) {
							String mode = rule.substring(6,7);
							String r = rule.substring(8);
							Pattern p = Pattern.compile(r);
							Matcher m = p.matcher(path);
							if (m.find())
								return mode;
						}

					}
				}
			}
			return defaultMode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultMode;
	}
}
