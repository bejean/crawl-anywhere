package fr.eolya.simplepipeline.connector.filequeueconnector;

import java.util.Date;
import java.util.Properties;

import fr.eolya.simplepipeline.IStateController;
import fr.eolya.simplepipeline.connector.threads.IMessageReceiver;
import fr.eolya.simplepipeline.connector.threads.ThreadController;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;

/**
 * A simple controller class for a multithreaded environment, where threads
 * process 'tasks' from a queue.
 *
 * Adapted from a code in the public domain by Andreas Hess <andreas.hess@ucd.ie>, 01/02/2003
 */

public class FileQueueConnectorThreadController extends ThreadController {

	/**
	 * Constructor that intializes the instance variables
	 * The queue may already contain some tasks.
	 */
	@SuppressWarnings("unchecked")
	public FileQueueConnectorThreadController(Class threadClass,
			Properties props,
			Logger logger,
			IStateController sc,
			IMessageReceiver receiver)
	throws InstantiationException, IllegalAccessException {

		this.threadClass = threadClass;
		this.props = props;
		this.sc = sc;
		this.receiver = receiver;

		counter = 0;
		nThreads = 0;
		maxThreads = 1;
		
		tasks = new FileQueueConnectorQueue();
	}

	public boolean start() {
		try {			
			startThreads();
			isStarted = true;
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Called by a thread to tell the controller that it is about to stop.
	 * The threadId is handed over to the MessageReceiver.
	 */
	public synchronized void finished(int threadId) {
		receiver.finished(threadId);
		nThreads--;
		if (nThreads == 0) {
			Utils.sleep(5000);
			// if no tasks in queue we're don
			if ((tasks.getQueueSize() == 0) || receiver.stopRequested()) {
				receiver.finishedAll();
				return;
			}
		}
	}

	/**
	 * Start the maximum number of allowed threads
	 */
	public synchronized void startThreads()
	throws InstantiationException, IllegalAccessException {
		// Start m threads
		// For more information on where m comes from see comment on the constructor.

		int m = maxThreads - nThreads;

		// Create threads
		for (int n = 0; n < m; n++) {
			int id = nThreads++;

			FileQueueConnectorThread thread = (FileQueueConnectorThread) threadClass.newInstance();
			thread.setThreadController(this);
			thread.setMessageReceiver(receiver);
			thread.setStateControler(sc);
			thread.setQueue(tasks);
			thread.setId(id);
			thread.setProps(props);
			thread.start();

			threads.add(thread);
			if (n==0 && m>1) Utils.sleep(60000);
		}
	}

	public boolean isProcessing()
	{
		for (int n = 0; n < nThreads; n++) {
			FileQueueConnectorThread thread = (FileQueueConnectorThread) threads.get(n);
			if (thread!=null) {
				if (thread.isProcessing()) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	public boolean isActive()
	{
		long nowTimeStamp = new Date().getTime();

		for (int n = 0; n < nThreads; n++) {
			FileQueueConnectorThread thread = (FileQueueConnectorThread) threads.get(n);
			if (thread!=null) {
				if (thread.getLastActivityTimeStamp() < (nowTimeStamp-(300*1000))) {
					// at least one thread didn't signal activity recently
					return false;
				}
			}
		}
		return true;
	}

}
