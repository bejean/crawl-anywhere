package fr.eolya.simplepipeline.connector.filequeueconnector;

import fr.eolya.simplepipeline.connector.threads.Queue;

/**
 * Class SourceQueue
 * 
 * This class has a queue and a set of Sources. Synchronized methods are
 * provided for inserting in both the set and the queue, retrieving (and
 * removing) an element from the queue only and for clearing the queue.
 * 
 * Adapted from a code in the public domain by Andreas Hess <andreas.hess@ucd.ie>, 01/02/2003
 */

public class FileQueueConnectorQueue extends Queue {

	public FileQueueConnectorQueue() {
		super();
	}

	public FileQueueConnectorQueue(int maxElements) {
		super(maxElements);
	}

	/**
	 * Return and remove the first element from the queue
	 */
	public synchronized FileQueueConnectorQueueItem pop() {
		return (FileQueueConnectorQueueItem) super.pop();
	}

	/**
	 * Add an element at the end of the queue
	 */
	public synchronized boolean push(FileQueueConnectorQueueItem o) {
		System.out.println("Pushing : " + o.getFileName());
		return super.push(o);
	}

}
