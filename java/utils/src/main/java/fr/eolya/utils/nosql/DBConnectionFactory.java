package fr.eolya.utils.nosql;

import java.net.UnknownHostException;

import fr.eolya.utils.nosql.mongodb.MongoDBConnection;


public class DBConnectionFactory {

	public static IDBConnection getDBConnectionInstance(String type, String dbHost, int dbPort, String userName, String userPassword) {
		if ("mongodb".equals(type)) {
			try {
				return  new MongoDBConnection(dbHost, dbPort, userName, userPassword);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

}
