package fr.eolya.crawler.documenthandler;

import java.io.InputStream;
import java.util.HashMap;

import fr.eolya.crawler.connectors.IConnector;
import fr.eolya.utils.*;

public interface IDocumentHandler {
	
	public void init(String outputType, String targetName, String targetType, String targetParameters, String targetQueueDir, XMLConfig config); 
	public int status();
	public void sendDoc(String sourceId, String itemId, String accountId, String rawData, HashMap<String,String> params, HashMap<String,String> metas, XMLConfig extra, IConnector cnx);
	public void sendDoc(String sourceId, String itemId, String accountId, InputStream streamData, HashMap<String,String> params, HashMap<String,String> metas, XMLConfig extra, IConnector cnx);
	public void removeDoc(String sourceId, String itemId, String accountId);
	public void resetSource(String sourceId);
	public void setLogger(Logger logger);
}
