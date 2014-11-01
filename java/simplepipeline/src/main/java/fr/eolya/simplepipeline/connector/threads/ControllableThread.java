package fr.eolya.simplepipeline.connector.threads;

import java.util.Date;
import java.util.Properties;

import fr.eolya.simplepipeline.IStateController;

/**
 * Abstract class that denotes a thread that can cooperate with a
 * ThreadController and has a Queue, a depth level and a MessageReceiver.
 *
 * Adapted from a code in the public domain by Andreas Hess <andreas.hess@ucd.ie>, 01/02/2003
 */

abstract public class ControllableThread extends Thread {

	protected int id;
	protected IStateController sc;
	protected IMessageReceiver mr;
	protected Properties props;
	protected Queue queue;
	protected ThreadController tc;
	protected boolean stopThread;
	protected long lastActivityTimeStamp;

	public synchronized void stopThread() {
        this.stopThread = true;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public void setThreadController(ThreadController tc) {
		this.tc = tc;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setMessageReceiver(IMessageReceiver mr) {
		this.mr = mr;
	}

	public void setStateControler(IStateController sc) {
		this.sc = sc;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public ControllableThread() {
		stopThread = false;
		setLastActivityTimeStamp();
	}
	
	public long getQueueSize() {
		return queue.getQueueSize();
	}

	/**
	 * The thread invokes the process method for each object in the queue
	 */
	public abstract void process(QueueItem o);

	public abstract void run();
	
	protected synchronized void setLastActivityTimeStamp() {
		this.lastActivityTimeStamp = new Date().getTime();
	}
	
	public long getLastActivityTimeStamp() {
		return this.lastActivityTimeStamp;
	}
}
