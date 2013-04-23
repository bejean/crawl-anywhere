package fr.eolya.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class XMLConfig {

	private Document document = null;

	public XMLConfig() {}

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

	public String getProperty(String xpath) {
		if (document==null) return null;
		Element child = (Element) document.selectSingleNode(xpath);
		if (child == null)
			return null;
		return child.getText();
	}

	public String getProperty(String xpath, String defaultValue) {
		String value = getProperty(xpath);
		if (value==null || "".equals(value)) return defaultValue;
		return value;
	}

	public String getPropertyAsXml(String xpath) {
		if (document==null) return null;
		Element child = (Element) document.selectSingleNode(xpath);
		if (child == null)
			return null;
		return child.asXML();
	}

	public String getPropertyAsXml(String xpath, String defaultValue) {
		String value = getProperty(xpath);
		if (value==null || "".equals(value)) return defaultValue;
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
			if (child == null) return false;	
			Element newElement = child.element(name);
			if (newElement==null) newElement = child.addElement(name);
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
		@SuppressWarnings("unchecked")
		List<Node> nodes = document.selectNodes(xpath);
		if (nodes == null)
			return 0;
		return nodes.size();
	}

	public Properties getProperties(String xpath) {
		if (document==null) return null;
		Properties props = null;	
		Element element = (Element) document.selectSingleNode(xpath);
		if (element == null)
			return null;

		@SuppressWarnings("unchecked")
		List<Element> childs = element.elements("param");
		if (childs!=null && childs.size()>0) {
			props = new Properties();
			for (int i=0; i<childs.size(); i++) {
				props.put(childs.get(i).attributeValue("name"), childs.get(i).getText());
			}
		}
		return props;
	}

	public String asXml() {
		if (document==null) return null;
		return document.asXML();
	}
	
	public Document getDocument() {
		return document;
	}
}
