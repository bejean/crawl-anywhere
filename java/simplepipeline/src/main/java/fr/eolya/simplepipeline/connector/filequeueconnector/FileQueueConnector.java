package fr.eolya.simplepipeline.connector.filequeueconnector;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import fr.eolya.simplepipeline.IStateController;
import fr.eolya.simplepipeline.SimplePipelineUtils;
import fr.eolya.simplepipeline.connector.Connector;
import fr.eolya.simplepipeline.connector.threads.IMessageReceiver;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 * 	<connector classname="fr.eolya.simplepipeline.connector.FileQueueConnector">
 * 		<param name="rootdir">/tmp/in</param>
 *		<param name="filepattern">^j[.].*[.]xml$</param>
 * 		<param name="onsuccessmoveto">/tmp/in/_success</param>
 *		<param name="onerrormoveto">/tmp/in/_error</param>
 * 	</connector>
 */

public class FileQueueConnector implements Connector, IMessageReceiver {

	protected long totalItemCount = 0;
	protected Properties props = null;
	protected IStateController sc;

	protected int maxThreads = 0;
	boolean stopRequested = false;
	private File rootDir = null;

	public boolean start() throws Exception {
		if (sc==null || sc.getConfig()==null) {
			throw new IllegalStateException("setConfig() was not called");
		}		
		maxThreads = Integer.parseInt(props.getProperty("threads", "2"));
		String rootDirPath = props.getProperty("rootdir");
		rootDirPath = Utils.getValidPropertyPath(rootDirPath, null, "HOME");
				
		rootDir = new File(rootDirPath);
		if (!rootDir.exists() || !rootDir.isDirectory()) return false;
		return true;
	}


	/**
	 * Run this Connector.
	 */
	public void execute() throws Exception {

		FileQueueConnectorThreadController ptc = new FileQueueConnectorThreadController(FileQueueConnectorThread.class, props, sc.getLogger(), sc, this);
		//ptc.setMaxThreads(maxThreads);
		FileQueueConnectorQueue pq = (FileQueueConnectorQueue) ptc.getQueue();

		ArrayList<String> previousQueued = null;

		ptc.start();

		java.util.Date dstartloop = new java.util.Date();
		long countLoop = 0;
		long lastDocCountLoop = 0;

		stopRequested = false;
		boolean bFinished = false;
		boolean firstLoop = true;
		while (!stopRequested && !bFinished){
			try {
				if (pq.getQueueSize()==0) {		
					File files[] = Utils.getListFileAlphaOrder(rootDir);

					if (getFilesCount(files, props.getProperty("filepattern"))>0) {
						if (!ptc.isProcessing() && previousQueued!=null)
							previousQueued.clear();
						ArrayList<String> previousQueuedTemp = new ArrayList<String>();

						for (int i=0; i<Math.min(files.length, 1000); i++) {
							if (files[i].isFile() && files[i].getName().matches(props.getProperty("filepattern"))) {
								if (previousQueued==null || !previousQueued.contains(files[i].getName())) {
									Doc d = null;
									try {
										d = new Doc(files[i]);
									} catch (Exception e) {
										SimplePipelineUtils.fileDone(files[i], null, false, props, sc.getLogger(), null);
									}
									if (d != null) {
										pq.push(new FileQueueConnectorQueueItem(rootDir + "/" + files[i].getName()));
									}
								}
								previousQueuedTemp.add(files[i].getName());
							}
						}
						previousQueued = previousQueuedTemp;
						
						if (firstLoop) {
						    ptc.setMaxThreads(maxThreads);
						    ptc.startThreads();
						    firstLoop = false;
						}
					}
					else {
						if (sc.getOnce()) {
							// try waiting 5 minutes
							sc.getLogger().log("    No more file to process : start waiting 5 minutes");
							int waitingSince = 0;
							stopRequested = sc.stopRequested();
							while (waitingSince<300*1000 && getFilesCount(files, props.getProperty("filepattern"))==0 && !stopRequested) {
								Utils.sleep(5000);
								waitingSince += 5000;
								stopRequested = sc.stopRequested();
								files = Utils.getListFileAlphaOrder(rootDir);
							}
							if (getFilesCount(files, props.getProperty("filepattern"))==0) {
								// No file to process after waiting 5 minutes => stop 
								if (!stopRequested) sc.getLogger().log("    No more file to process after waiting 5 minutes and mode once : stop pipeline");
								bFinished = true;
							}
						}
					}
				}

				stopRequested = sc.stopRequested();
				if (!stopRequested) {
					Utils.sleep(2000);

					countLoop++;
					if ((countLoop % 30)==0) {

						java.util.Date dendloop = new java.util.Date();
						long timeloop = (dendloop.getTime() - dstartloop.getTime());
						long countDoc = getProcessedItemsCount() - lastDocCountLoop;
						lastDocCountLoop += countDoc;

						if (countDoc>0) {
							String msg = "Loop (" + String.valueOf(countLoop) + ") : \n";
							msg += "    time (sec)                  = " + String.valueOf(timeloop / 1000) + "\n";
							msg += "    doc (total)                 = " + String.valueOf(lastDocCountLoop) + "\n";
							msg += "    doc                         = " + String.valueOf(countDoc) + "\n";
							msg += "    time per doc (ms)           = " + String.valueOf(timeloop / countDoc) + "\n";
							msg += "    docs per minute             = " + String.valueOf((countDoc * 60 * 1000) / timeloop) + "\n";
							msg += "    memory (free / max / total) = " + String.valueOf(Runtime.getRuntime().freeMemory()) + " / " + String.valueOf(Runtime.getRuntime().maxMemory()) + " / " + String.valueOf(Runtime.getRuntime().totalMemory());
							sc.getLogger().log (msg);				
						}
						dstartloop = new java.util.Date();
					}
				}
			}
			catch (Exception e) {
				System.err.println("An error occured: ");
				e.printStackTrace();
			}
		}
		pq.clear();
		ptc.stopThreads();

		// Wait for threads end
		int loopCount = 0;
		while (ptc.getRunningThreads()>0) {
			if (loopCount==6) {
				sc.getLogger().log("Waiting all threads terminate !");
				loopCount=0;
			}
			loopCount++;
			Utils.sleep(5000);
		}

		//				java.util.Date dstartloop = new java.util.Date();
		//				java.util.Date dendloop = new java.util.Date();
		//				long timeloop = (dendloop.getTime() - dstartloop.getTime());
		//
		//				if (timeloop>0) {
		//					String msg = "Loop : \n";
		//					msg += "    time (sec)                  = " + String.valueOf(timeloop / 1000) + "\n";
		//					msg += "    doc                         = " + String.valueOf(Math.min(files.length, 1000)) + "\n";
		//					msg += "    time per doc (ms)           = " + String.valueOf(timeloop / Math.min(files.length, 1000)) + "\n";
		//					msg += "    docs per minute             = " + String.valueOf((Math.min(files.length, 1000) * 60 * 1000) / timeloop) + "\n";
		//					msg += "    memory (free / max / total) = " + String.valueOf(Runtime.getRuntime().freeMemory()) + " / " + String.valueOf(Runtime.getRuntime().maxMemory()) + " / " + String.valueOf(Runtime.getRuntime().totalMemory());
		//					logger.log (msg);
		//				}

	}

	/**
	 * Configure this connector's state controller
	 */
	public void setStateController(IStateController sc) {
		this.sc = sc;
		props = sc.getConfig().getConnectorProperties();
	}

	public long getProcessedItemsCount() {
		return totalItemCount;
	}

	public synchronized void incrProcessedItemsCount() {
		if (totalItemCount==Long.MAX_VALUE)
			totalItemCount=0;
		totalItemCount++;
	}

	public boolean stopRequested() {
		return stopRequested;
	}

	public void receiveMessage(Object o, long threadId) {
		// In our case, the object is already string, but that doesn't matter
		sc.getLogger().log("[" + threadId + "] " + o.toString());
	}

	public void finished(long threadId) {
		sc.getLogger().log("[" + threadId + "] finished");
	}

	public void finishedAll() {
		// ignore
	}

	private static int getFilesCount(File files[], String pattern) {
		int count = 0;
		if (files.length>0) {
			for (int i=0; i<files.length; i++) {
				if (files[i].isFile() && files[i].getName().matches(pattern)) {
					count++;
				}
			}
		}
		return count;
	}


}
