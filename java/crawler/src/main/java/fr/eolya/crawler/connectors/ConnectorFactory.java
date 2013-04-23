package fr.eolya.crawler.connectors;

import java.lang.reflect.*;
import java.util.Map;

import fr.eolya.crawler.queue.ISourceItemsQueue;
import fr.eolya.utils.nosql.IDBConnection;

public class ConnectorFactory {
	public static IConnector getConnector(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Class classRef = Class.forName(className);
		return (IConnector) classRef.newInstance();
	}

	public static ISource getSourceInstance(String className, String id, String crawlMode, Map<String,Object> srcData) throws InstantiationException, IllegalAccessException, SecurityException, IllegalArgumentException, InvocationTargetException {

		// TODO: V4 - class.forname ne passe pas par le catch. On ne détecte pas l'erreur et le pid file n'est pas supprimé.

		try {
			@SuppressWarnings("rawtypes")
			Class c = Class.forName(className);
			@SuppressWarnings("unchecked")
			Method m = c.getDeclaredMethod("createSourceInstance", Integer.class, String.class, String.class, Map.class);
			Object o = m.invoke(null, new Integer(id), className, crawlMode, srcData);
			return (ISource) o;		
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e2) {
			e2.printStackTrace();
		}
		return null;
	}

	public static ISourceItemsQueue getSourceItemsQueueInstance(ISource src, String type, IDBConnection con, String dbName, String dbCollName) throws InstantiationException, IllegalAccessException, SecurityException, IllegalArgumentException, InvocationTargetException {

		// TODO class.forname ne passe pas par le catch. On ne détecte pas l'erreur et le pid file n'est pas supprimé.
		
		try {
			@SuppressWarnings("rawtypes")
			Class c = Class.forName(src.getClassName());
			@SuppressWarnings("unchecked")
			Method m = c.getDeclaredMethod("createSourceItemsQueueInstance", String.class, IDBConnection.class, String.class, String.class, ISource.class);
			Object o = m.invoke(null, type, con, dbName, dbCollName, src);
			return (ISourceItemsQueue) o;		
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
}