package fr.eolya.simplepipeline.connector;

public class ConnectorFactory {
	public static Connector getConnector(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if ("fr.eolya.simplepipeline.connector.FileQueueConnector".equals(className)) className = "fr.eolya.simplepipeline.connector.filequeueconnector.FileQueueConnector";
		Class classRef = Class.forName(className);
		return (Connector) classRef.newInstance();
	}
}