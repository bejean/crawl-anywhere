package fr.eolya.simplepipeline.connector.filequeueconnector;

import fr.eolya.simplepipeline.connector.threads.QueueItem;

public class FileQueueConnectorQueueItem extends QueueItem {
	
	private String fileName;

	public FileQueueConnectorQueueItem(String fileName) {
		super();
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return fileName;
	}
}
