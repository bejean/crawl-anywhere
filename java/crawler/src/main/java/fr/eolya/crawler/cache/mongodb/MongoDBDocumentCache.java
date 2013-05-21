package fr.eolya.crawler.cache.mongodb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import fr.eolya.crawler.cache.DocumentCacheItem;
import fr.eolya.crawler.cache.IDocumentCache;
import fr.eolya.utils.Base64;
import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;
import fr.eolya.utils.nosql.mongodb.MongoDBCollection;
import fr.eolya.utils.nosql.mongodb.MongoDBConnection;
import fr.eolya.utils.nosql.mongodb.MongoDBDatabase;

public class MongoDBDocumentCache implements IDocumentCache {

	private MongoDBConnection con = null;
	private MongoDBDatabase db = null;
	private String collName;	
	private MongoDBCollection coll = null;
	private final Object collMonitor = new Object();
	private long size;
	private long maxDocSize;

	public MongoDBDocumentCache(String sourceId, MongoDBConnection con, String dbName, String collName) {
		this.con = con;
		this.db = new MongoDBDatabase(this.con, dbName);
		this.collName = collName + "_" + sourceId;
		this.coll = new MongoDBCollection(db,this.collName);
		this.size = this.coll.size();
		this.maxDocSize = 0;
	}

	@Override
	public String put(String itemId, InputStream dataStream, long dataSize, 
			HashMap<String, String> params, HashMap<String, String> metas, XMLConfig extra) {

		if (coll == null) return null;
		if (maxDocSize > 0 && dataSize > 0 && dataSize > maxDocSize) return null;

		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("item_id", itemId);

		BasicDBObject doc = new BasicDBObject();
		doc.put("item_id", itemId);
		doc.put("item_extra", extra.asXml());

		Document xml_params = DocumentFactory.getInstance().createDocument("utf-8");
		xml_params.setXMLEncoding("utf-8");
		Element xml_params_items = xml_params.addElement("params");

		for (Map.Entry<String, String> item : params.entrySet()) {
			String key = item.getKey();
			if (item.getValue()!=null) xml_params_items.addElement(key).addText(item.getValue());
		}
		doc.put("item_params", xml_params.asXML());

		Document xml_metas = DocumentFactory.getInstance().createDocument("utf-8");
		xml_metas.setXMLEncoding("utf-8");
		Element xml_metas_items = xml_metas.addElement("metas");

		for (Map.Entry<String, String> item : metas.entrySet()) {
			String key = item.getKey();
			if (item.getKey().startsWith("meta_")) {
				key = key.replace(':', '_').replace('-', '_').replace('.', '_').replace('/', '_');
			}
			if (item.getValue()!=null) xml_metas_items.addElement(key).addText(item.getValue());
		}	
		doc.put("item_metas", xml_metas.asXML());

		if (dataStream!=null) {
			String contentBase64 = "";
			try {
				//dataStream.reset();
				contentBase64 = Base64.inputStreamToStringBase64(dataStream);
				doc.put("content_base64", contentBase64);

			} catch (IOException e) {
				System.out.println(itemId);
				e.printStackTrace();
			}
		}
		remove(itemId);
		return coll.add(doc);
	}

	@Override
	public void remove(String itemId) {
		if (coll == null) return;
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("item_id", itemId);
		coll.remove(docsearch);        
	}

	@Override
	public DocumentCacheItem get(String itemId) {
		if (coll == null) return null;
		BasicDBObject docsearch = new BasicDBObject();
		docsearch.put("item_id", itemId);
		BasicDBObject doc = coll.get(docsearch);
		return doc2DocumentCacheItem(doc);
	}

	public static DocumentCacheItem doc2DocumentCacheItem(BasicDBObject doc) {
		try {
			String itemId = doc.get("item_id").toString();

			XMLConfig extra = null;
			if (doc.get("item_extra")!=null) {
				extra = new XMLConfig();
				extra.loadString(doc.get("item_extra").toString());
			}

			HashMap<String, String> params = new HashMap<String, String>();
			HashMap<String, String> metas = new HashMap<String, String>();

			Document document = null;
			SAXReader reader = new SAXReader();
			reader.setValidation(false);
			try {
				document = reader.read(new StringReader(doc.get("item_params").toString()));

				Element elmParams = (Element) document.selectSingleNode("params");
				if (elmParams!=null) {
					List<Element> items = elmParams.elements();
					Iterator<Element> iterItems = items.iterator();
					while (iterItems.hasNext()) {
						Element item = (Element) iterItems.next();
						params.put(item.getName(), item.getText());
					}
				}
			} catch (DocumentException de) {
				IOException ioe = new IOException();
				ioe.initCause(de);
				throw ioe;
			}

			reader = new SAXReader();
			reader.setValidation(false);
			try {
				document = reader.read(new StringReader(doc.get("item_metas").toString()));

				Element elmMetas = (Element) document.selectSingleNode("metas");
				if (elmMetas!=null) {
					List<Element> items = elmMetas.elements();
					Iterator<Element> iterItems = items.iterator();
					while (iterItems.hasNext()) {
						Element item = (Element) iterItems.next();
						metas.put(item.getName().replace(':', '_').replace('-', '_').replace('.', '_').replace('/', '_'), item.getText());
					}
				}
			} catch (DocumentException de) {
				IOException ioe = new IOException();
				ioe.initCause(de);
				throw ioe;
			}

			InputStream inputSource = new ByteArrayInputStream(doc.get("content_base64").toString().getBytes());
			InputStream streamData = new Base64.InputStream(inputSource, Base64.DECODE);

			return new DocumentCacheItem( itemId, streamData, params, metas, extra);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public void reset() {
		synchronized (collMonitor) {
			coll.drop();
			coll = new MongoDBCollection(db,this.collName);
		}		
		size = 0; 
	}

	public long getMaxDocSize() {
		return maxDocSize;
	}

	public void setMaxDocSize(long maxDocSize) {
		this.maxDocSize = maxDocSize;
	}

	@Override
	public Iterator<DocumentCacheItem> getIterator() {
		if (coll == null) return null;
		DBCursor cur = coll.getCursor(new BasicDBObject());
		return new  MongoDBDocumentCacheIterator(cur);
	}

}
