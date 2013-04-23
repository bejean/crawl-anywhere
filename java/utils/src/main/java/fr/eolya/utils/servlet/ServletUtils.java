package fr.eolya.utils.servlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.naming.*;
import javax.servlet.http.HttpServlet;

import org.dom4j.*;

import fr.eolya.utils.XMLConfig;


public class ServletUtils {

	private static Document buildErrorXmlDocument(int errno, String errmsg)
	{
		Document resDocument = null;

		try {
			resDocument = DocumentHelper.parseText("<?xml version=\"1.0\" encoding=\"utf-8\"?><error/>");
			Element error = resDocument.getRootElement();
			Element no = error.addElement("errno");
			no.addText(strEmptyIfNull(Integer.toString(errno)));
			Element msg = error.addElement("errmsg");
			msg.addText(strEmptyIfNull(errmsg));
			return resDocument;
		}
		catch(Exception e) {}
		return null;
	}

	public static String buildErrorXml(int errno, String errmsg)
	{
		Document doc = buildErrorXmlDocument(errno, errmsg);
		if (doc==null)
			return "";
		else
		{
			SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar calendar=new GregorianCalendar();
			dateFormat.format(calendar.getTime());
			System.out.println(dateFormat.format(calendar.getTime()) + " - " + doc.asXML());
			return doc.asXML();
		}
	}

	public static String strEmptyIfNull(String str) {
		if (str == null)
			return "";
		return str;
	}

	public static String locateHome() {
		String home = null;
		
		// Try JNDI
		try {
			Context c = new InitialContext();
			home = (String)c.lookup("java:comp/env/home");
			if (home!=null && !"".equals(home)) return normalizeDir(home);
		} catch (Exception e) {
			//e.printStackTrace();
		} 
		
		// Try env (for debugging)
		home = System.getenv("home");
		if (home!=null && !"".equals(home)) return normalizeDir(home);

		return "";
	}

	/** Ensures a directory name always ends with a '/'. */
	public static String normalizeDir(String path) {
		return ( path != null && (!(path.endsWith("/") || path.endsWith("\\"))) )? path + File.separator : path;
	}
	
	public static String getSetting(HttpServlet httpServlet, XMLConfig xmlConfig, String key) {		
		if (xmlConfig!=null) return xmlConfig.getProperty("/servlet/param[@name='" + key + "']");
		return httpServlet.getServletConfig().getInitParameter(key);
	}
	public static String getSetting(HttpServlet httpServlet, XMLConfig xmlConfig, String key, String defaultValue) {
		String value = getSetting(httpServlet, xmlConfig, key);
		if (value==null || "".equals(value)) return defaultValue;
		return value;
	}

	
	public static XMLConfig loadXmlConfig(String relativePath) {
		String home = ServletUtils.locateHome();	
		String xmlConfigPath = home + relativePath;
		File xmlConfigFile = new File(xmlConfigPath);
		XMLConfig xmlConfig = null;
		if (xmlConfigFile!=null && xmlConfigFile.exists()) {
			xmlConfig = new XMLConfig();
			try {
				xmlConfig.loadFile(xmlConfigPath);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return xmlConfig;
		}
		return null;
	}
}
