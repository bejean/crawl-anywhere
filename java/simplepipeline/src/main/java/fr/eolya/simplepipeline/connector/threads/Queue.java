package fr.eolya.simplepipeline.connector.threads;

import java.util.*;

/**
 * Class Queue
 *
 * This class has a queue and a set of Sources. Synchronized methods are
 * provided for inserting in both the set and the queue, retrieving (and
 * removing) an element from the queue only and for clearing the queue.
 *
 * Adapted from a code in the public domain by Andreas Hess <andreas.hess@ucd.ie>, 01/02/2003
 */

public class Queue {

	LinkedList<QueueItem> queueElements;

	/**
	 * Maximum number of elements allowed in the gatheredLinks set
	 */
	protected int maxElements;

	public Queue() {
		queueElements = new LinkedList<QueueItem>();
		maxElements = -1;
	}

	public Queue(int maxElements) {
		queueElements = new LinkedList<QueueItem>();
		this.maxElements = maxElements;
	}

	/**
	 * Set the maximum number of allowed elements
	 */
	public void setMaxElements(int maxElements) {
		this.maxElements = maxElements;
	}

	/**
	 * Return how many elements are in the queue
	 */
	public int getQueueSize() {
		return queueElements.size();
	}

	/**
	 * Return the first element from the queue
	 */
	protected QueueItem getFirst() {
		synchronized (queueElements) {
			// try to get element from the queue
			// is the queue is empty, return null
			if (queueElements.size() == 0) {
				return null;
			} else {
				return (QueueItem) queueElements.getFirst();
			}
		}
	}

	/**
	 * Return and remove the first element from the queue
	 */
	public QueueItem pop() {
		synchronized (queueElements) {
			// try to get element from the queue
			// is the queue is empty, return null
			if (queueElements.size() == 0) {
				return null;
			} else {
				return (QueueItem) queueElements.removeFirst();
			}
		}
	}

	/**
	 * Add an element at the end of the queue
	 */
	public boolean push(QueueItem o) {
		synchronized (queueElements) {

			// don't allow more than maxElements links in the queue
			if (maxElements != -1 && maxElements <= queueElements.size())
				return false;

			//if (queueElements.contains(o))
			queueElements.addLast(o);
		}
		return true;
	}

	/**
	 * Clear queue.
	 */
	public void clear() {
		synchronized (queueElements) {
			queueElements.clear();
		}
	}

	public boolean contains(QueueItem o) {
		synchronized (queueElements) {
			return queueElements.contains(o);
		}
	}

}
