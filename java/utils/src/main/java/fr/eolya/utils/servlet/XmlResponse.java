package fr.eolya.utils.servlet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;

import fr.eolya.utils.*;


public class XmlResponse {

	private static Document buildErrorXmlDocument(int errno, String errmsg)
	{
		Document resDocument = null;

		try {
			resDocument = DocumentHelper.parseText("<?xml version=\"1.0\" encoding=\"utf-8\"?><error/>");
			Element error = resDocument.getRootElement();
			Element no = error.addElement("errno");
			no.addText(StringUtils.trimToEmpty(Integer.toString(errno)));
			Element msg = error.addElement("errmsg");
			msg.addText(StringUtils.trimToEmpty(errmsg));
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
}
