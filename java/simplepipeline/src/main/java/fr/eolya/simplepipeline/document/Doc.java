package fr.eolya.simplepipeline.document;

import java.io.File;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Doc {

	public String getFileName() {
		return fileName;
	}

	Document document;
	String fileName;

	public Doc(File file) throws IOException {
		this.fileName = file.getAbsolutePath();
		load(file);
	}

	public Doc(String fileName)  throws IOException {
		this.fileName = fileName;
		load(new File(fileName));
	}

	private void load(File file) throws IOException {
		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		try {
			document = reader.read(file);
		} catch (DocumentException de) {
			IOException ioe = new IOException();
			ioe.initCause(de);
			throw ioe;
		}		
	}

	public Document getDocument() {
		return document;
	}

	public boolean existElement(String xpath) {
		Element e = (Element) document.selectSingleNode(xpath);
		if (e == null)
			return false;
		else
			return true;
	}

	public String getElementText(String xpath) {
		Element e = (Element) document.selectSingleNode(xpath);
		if (e == null)
			return "";
		else
			return e.getText();
	}

	public String getElementAttribute(String xpath, String attribute) {
		Element e = (Element) document.selectSingleNode(xpath);
		if (e == null) {
			return "";
		} else {
			String value = e.attributeValue(attribute);
			if (value == null) {
				return "";
			}
			else {
				return value;
			}
		}
	}

	public void addElement(String xpath, String elementName, String text) {
		if (text==null) return;
		Element e = (Element) document.selectSingleNode(xpath);
		if (e != null) e.addElement(elementName).setText(text);
	}

	public void setElementText(String xpath, String text) {
		Element e = (Element) document.selectSingleNode(xpath);
		if (e != null) e.setText(text);
	}

	public String asXML() {
		if (document==null)
			throw new IllegalStateException("Document not initialized");
		return document.asXML();
	}
}
