package org.apache.lucene.analysis.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class XMLConfig {

	protected Document document = null;

	public XMLConfig() {
	}

	public void loadFile(String fileName) throws IOException {
		if (fileName==null || "".equals(fileName)) return;
		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		try {
			document = reader.read(new File(fileName));
		} catch (DocumentException de) {
			IOException ioe = new IOException();
			ioe.initCause(de);
			throw ioe;
		}
	}

	public void loadString(String xmlFragment) throws IOException {
		if (xmlFragment==null || "".equals(xmlFragment)) return;
		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		try {
			document = reader.read(new StringReader(xmlFragment));
		} catch (DocumentException de) {
			IOException ioe = new IOException();
			ioe.initCause(de);
			throw ioe;
		}
	}

	public void loadString(String[] xmlFragmentLines) throws IOException {
		loadString(XMLConfig.implode("\n", xmlFragmentLines));
	}

	public boolean pathExists(String xpath) {
		if (document==null) return false;
		Element child = (Element) document.selectSingleNode(xpath);
		if (child == null) return false;
		return true;
	}
	
	public String getProperty(String xpath) {
		if (document==null) return null;
		Element child = (Element) document.selectSingleNode(xpath);
		if (child == null)
			return null;
		return child.getText();
	}

	public String getProperty(String xpath, String defaultValue) {
		String value = getProperty(xpath);
		if (value==null) return defaultValue;
		return value;
	}

	public boolean setProperty(String xpath, String name, String value) {
		if (document==null) {
			document = DocumentFactory.getInstance().createDocument("utf-8");
			document.setXMLEncoding("utf-8");
		}

		if ("".equals(xpath) || "/".equals(xpath)) {
			Element newElement = (Element) document.selectSingleNode(name);
			if (newElement == null)
				newElement = document.addElement(name);
			newElement.setText(value);
		}
		else {
			Element child = (Element) document.selectSingleNode(xpath);
			if (child == null)
				return false;	
			Element newElement = child.element(name);
			if (newElement==null)
				newElement = child.addElement(name);
			if (newElement==null) return false;
			newElement.setText(value);
		}
		return true;
	}

	public String getPropertyAttribute(String xpath, String attribute) {
		if (document==null) return null;
		Element child = (Element) document.selectSingleNode(xpath);
		if (child == null)
			return null;
		return child.attributeValue(attribute);
	}

	public int getElementCount(String xpath) {
		if (document==null) return 0;
		List<Node> nodes = document.selectNodes(xpath);
		if (nodes == null)
			return 0;
		return nodes.size();
	}

	public Properties getAttributesAsProperties(String xpath) {
		if (document==null) return null;
		Properties props = null;	
		Element element = (Element) document.selectSingleNode(xpath);
		if (element == null)
			return null;
		
		Iterator itr =element.attributeIterator();
		while(itr.hasNext()) {
			if (props==null) props = new Properties();
		    Attribute attr = (Attribute) itr.next(); 
			props.put(attr.getName(), attr.getValue());
		} 
		return props;
	}
	
	public Properties getProperties(String xpath) {
		if (document==null) return null;
		Properties props = null;	
		Element element = (Element) document.selectSingleNode(xpath);
		if (element == null)
			return null;

		List<Element> childs = element.elements("param");
		if (childs!=null && childs.size()>0) {
			props = new Properties();
			for (int i=0; i<childs.size(); i++) {
				props.put(childs.get(i).attributeValue("name"), childs.get(i).getText());
			}
		}
		return props;
	}

	public String nodeAsXml(String xpath) {
		if (document==null) return null;
		Element element = (Element) document.selectSingleNode(xpath);
		if (element == null) return null;
		return element.asXML();
	}
	
	public String asXml() {
		if (document==null) return null;
		return document.asXML();
	}

	public Document getDocument() {
		return document;
	}

	/***
	 * Fusionne les éléments d'un tableau en une chaîne
	 * @param delim : la chaîne de séparation
	 * @param args : la tableau
	 * @return la chaîne fusionnée
	 */
	private static String implode(String delim, String[] args){
		StringBuffer sb = new StringBuffer();

		for(int i =0; i < args.length; i++){
			if (i > 0)
				sb.append(delim);

			sb.append(args[i]);
		}

		return sb.toString();
	}
}
