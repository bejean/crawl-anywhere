package fr.eolya.simplepipeline.stage;

import fr.eolya.simplepipeline.IStateController;
import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Logger;

public class StageList {

	public static final int STATUS_OK = 0; 
	public static final int STATUS_ERROR = -1; 

	// the first stage in the list
	private Stage first;
	private Logger logger;
	private int statusStages;
	private int threadId;


	public void createPipeline(int threadId, PipelineConfig config, Logger logger, boolean verbose, IStateController sc) {

		first = null;
		Stage current = null;
		this.logger = logger;
		this.threadId = threadId;

		try {
			if (logger!=null) logger.log("	Initiating stage list");
			for (int i=0; i<config.getStageCount(); i++) {
				if (config.isStageEnabled(i)) {
					String className = config.getStageClassName(i);
					if (logger!=null) logger.log("	Adding stage : " + className);
					if (className==null || "".equals(className))
						throw new IllegalArgumentException("Connector classname is missing or empty");

					Stage stage = StageFactory.getStage(className);
					stage.setProperties(config.getStageProperties(i));
					stage.setStateController(sc);
					stage.setLogger(logger);
					stage.setVerbose(verbose);
					stage.setStageList(this);

					if (first == null) {
						first = stage;
						current = stage;
					} else {
						current.setNextStage(stage);
						current = stage;
					}
				}
			}		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Pipeline creation error");
		}
	}
	
	public void logProcessingTime() {
		Stage current = first;
		String msg = "[" + String.valueOf(threadId) + "] Processing time";
		while (current!=null) {
			msg += "\n    " + current.getClass().getName() + " : " +  String.valueOf(current.getProcessingTime());
			current = current.getNextStage();
		}
		logger.log(msg);
	}

	public void initialize() {
		if (first == null) {
			// Debug - throw new IllegalStateException("Pipeline was not created");
			if (logger!=null) logger.log("	Pipeline was not created (no stages)");
			return;
		}

		first.initialize();
		Stage next = first.getNextStage();
		while (next != null) {
			next.initialize();
			next = next.getNextStage();
		}
	}

	public void processDoc(Doc doc) throws Exception {
		if (first == null) {
			// Debug - throw new IllegalStateException("Pipeline was not created");
			if (logger!=null) logger.log("	Pipeline was not created (no stages)");
			return;
		}

		statusStages = STATUS_OK;
		if (first != null) {
			first.processDoc(doc);
		}
	}

	public void close() {
		if (first == null) {
			// Debug - throw new IllegalStateException("Pipeline was not created");
			if (logger!=null) logger.log("	Pipeline was not created (no stages)");
			return;
		}

		first.close();
		Stage next = first.getNextStage();
		while (next != null) {
			next.close();
			next = next.getNextStage();
		}		
	}

	public void setStagesStatus (int status) {
		statusStages = status;
	}
	public int getStagesStatus() {
		return statusStages;
	}
	public int getThreadId() {
		return threadId;
	}
}
