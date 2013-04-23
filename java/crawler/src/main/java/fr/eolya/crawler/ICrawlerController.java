package fr.eolya.crawler;

import fr.eolya.crawler.database.ICrawlerDB;
import fr.eolya.utils.nosql.IDBConnection;

public interface ICrawlerController {
	public boolean stopRequested();
	public IDBConnection getDBConnection(boolean forceReconnect);
	public ICrawlerDB getCrawlerDB();
}
