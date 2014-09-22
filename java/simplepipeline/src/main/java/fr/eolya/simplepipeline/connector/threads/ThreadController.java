package fr.eolya.simplepipeline.connector.threads;

import java.util.ArrayList;
import java.util.Properties;

import fr.eolya.simplepipeline.IStateController;

/**
 * A simple controller class for a multithreaded environment, where threads may
 * insert and process 'tasks' from/into a queue.
 *
 * Adapted from a code in the public domain by Andreas Hess <andreas.hess@ucd.ie>, 01/02/2003
 */

abstract public class ThreadController {

	/**
	 * the task queue
	 */
	protected Queue tasks;

	/**
	 * maximum number of parallel threads -1 if unlimited
	 */
	protected int maxThreads;

	/**
	 * An object that is notified about what a thread does See comments for
	 * interface MessageReceiver for details.
	 */
	protected IMessageReceiver receiver;

	/**
	 * The class of the threads created by this ThreadController This class is
	 * expected to be a subtype of ControllableThread.
	 */
	@SuppressWarnings("unchecked")
	protected Class threadClass;

	/**
	 * A unique synchronized counter
	 */
	protected int counter;

	/**
	 * Number of currently running threads This value is handed to the threads
	 * as an id, so note that the thread id is not unique, but is always in the
	 * range 0...maxThreads-1
	 */
	protected int nThreads;

	protected ArrayList<ControllableThread> threads = new ArrayList<ControllableThread>();

	protected Properties props;
	protected IStateController sc;

	protected boolean isStarted;

	/**
	 * Get a unique number from a counter
	 */
	public synchronized int getUniqueNumber() {
		return counter++;
	}

	public boolean isStarted () {
		return isStarted;
	}

	/**
	 * Get queue
	 */
	public Queue getQueue() {
		return tasks;
	}

	/**
	 * Adjust number of allowed threads and start new threads if possible
	 */
	public synchronized void setMaxThreads(int maxThreads)
	throws InstantiationException, IllegalAccessException {
		this.maxThreads = maxThreads;
		//startThreads();
	}

	/**
	 * Get number of maximum allowed threads
	 */
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * Get number of currently running threads
	 */
	public int getRunningThreads() {
		return nThreads;
	}

	/**
	 * Called by a thread to tell the controller that it is about to stop. The
	 * threadId is handed over to the MessageReceiver.
	 */
	public abstract void finished(int threadId);
	public abstract boolean start();

	/**
	 * Start the maximum number of allowed threads
	 */
	public abstract void startThreads() throws InstantiationException, IllegalAccessException;

	public void stopThreads() {
		for (int n = 0; n < nThreads; n++) {
			ControllableThread thread = threads.get(n);
			if (thread!=null) {
				try {
					thread.stopThread();
				}
				catch(Exception e) {}
			}
		}
	}

}
