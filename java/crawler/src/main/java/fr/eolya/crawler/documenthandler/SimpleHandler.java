package fr.eolya.crawler.documenthandler;

import java.io.InputStream;
import java.util.HashMap;

import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.utils.XMLConfig;

public class SimpleHandler extends BaseHandler implements IDocumentHandler {

	public SimpleHandler() {}

	public int status() { return 0; }
	
	public void sendDoc(String sourceId, String itemId, String accountId, String rawData, HashMap<String,String> params, HashMap<String,String> metas, XMLConfig extra, IConnector cnx) {
		//System.out.println("Simple document handler - send document (string) : " + id);
	}

	public void sendDoc(String sourceId, String itemId, String accountId, InputStream streamData, HashMap<String,String> params, HashMap<String,String> metas, XMLConfig extra, IConnector cnx) {
		//System.out.println("Simple document handler - send document (stream) : " + id);
	}

	public void removeDoc(String sourceId, String itemId, String accountId) {
		//System.out.println("Simple document handler - remove document : " + id);
	}
	
	public void resetSource(String sourceId) {
		//System.out.println("Simple document handler - reset source : " + sourceId);
	}

}
