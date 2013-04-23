package fr.eolya.simplepipeline.connector.filequeueconnector;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import fr.eolya.simplepipeline.connector.threads.ControllableThread;
import fr.eolya.simplepipeline.connector.threads.QueueItem;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.simplepipeline.stage.StageList;
import fr.eolya.simplepipeline.SimplePipelineUtils;
import fr.eolya.utils.Utils;

public class FileQueueConnectorThread extends ControllableThread {

	private StageList stageList = null;
	private boolean processing = false;

	public boolean isProcessing() {
		return processing;
	}
	public FileQueueConnectorThread() {
		super();
	}

	public void process(QueueItem o) {
		String fileName = ((FileQueueConnectorQueueItem) o).getFileName();
		sc.getLogger().log("[" + id + "] processing : " + fileName);
		System.out.println("[" + id + "] processing : " + fileName);

		File f = new File(fileName);
		try {
			Doc d = new Doc(f);
			if (d != null) {
				processing = true;
				stageList.processDoc(d);
				if (stageList.getStagesStatus() == StageList.STATUS_ERROR) {
					//Utils.sleep(10+(id*3));
					//if (false) {
					fileDone(f, d, false, props);
				}
				else {
					fileDone(f, d, true, props);								
				}
				processing = false;
			}
		} catch (IOException e) {
			fileDone(f, null, false, props);
			e.printStackTrace();
		} catch (Exception e) {
			fileDone(f, null, false, props);
			e.printStackTrace();
			//throw new RuntimeException("pipeline failed (" + e.getMessage() + ")");
		} catch (OutOfMemoryError e) {
			fileDone(f, null, false, props);
			e.printStackTrace();
		}
		mr.incrProcessedItemsCount();
	}

	public void run() {	

		if (stageList==null) {
			stageList = new StageList();
			stageList.createPipeline(id, sc.getConfig(), sc.getLogger(), sc.getVerbose(), sc);
			stageList.initialize();
		}

		while (!mr.stopRequested() && !stopThread) {
			// pop new urls from the queue until queue is empty
			for (QueueItem newTask = queue.pop(); newTask!=null && !stopThread; newTask = queue.pop()) {

				// Process the newTask
				process(newTask);

				//synchronized(this) {
				//	Thread.yield();
				if (mr.stopRequested() || stopThread) {
					sc.getLogger().log("[" + id + "] Stop crawling due to stop request !");
					break;
				}
				//}

				super.setLastActivityTimeStamp();
			}
			Utils.sleep(1000);
		}

		stageList.logProcessingTime();

		// Notify the ThreadController that we're done
		tc.finished(id);
	}

	private void fileDone(File f, Doc d, boolean success, Properties props) {
		String tgtPath;
		if (success) {
			tgtPath = props.getProperty("onsuccessmoveto");
		} else {
			tgtPath = props.getProperty("onerrormoveto");
		}
		tgtPath = Utils.getValidPropertyPath(tgtPath, null, "HOME");

		if (tgtPath!=null && !"".equals(tgtPath)) {
			tgtPath = SimplePipelineUtils.getTransformedPath(tgtPath, d);
			File tgtDir = new File(tgtPath);
			tgtDir.mkdirs();
			File f2 = new File(tgtPath + "/" + f.getName());
			if (f2.exists()) f2.delete();
			try {
				sc.getLogger().log("[" + id + "] File done - move : " + f.getAbsolutePath() + " -> " + f2.getAbsolutePath());
				FileUtils.moveFile(f, f2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (f.exists())
			f.delete();	
	}

}
