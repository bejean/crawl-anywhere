package fr.eolya.simplepipeline.stage;

import java.util.Properties;

import fr.eolya.simplepipeline.IStateController;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Logger;

public abstract class Stage {

	protected Properties props = null;
	protected Stage nextStage = null;
	protected Logger logger = null;;
	protected IStateController sc;
	protected boolean verbose;
	protected String aOnAction[] = null;
	protected StageList stageList = null;
	protected long processingTime = 0;

	public Stage() {
	}

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		String onAction = props.getProperty("onaction");
		if (onAction != null && "".equals(onAction.trim())) onAction = null;
		if (onAction != null) {
			aOnAction = onAction.trim().split("\\s*,\\s*");
		}
	}

	/**
	 * Perform some operation on an document. 
	 * @param doc the document to process
	 */
	public abstract void processDoc(Doc doc) throws Exception;

	/**
	 * Closes the stage and releases any resources.
	 */
	public void close() {
	}

	public void setProperties (Properties props) {
		this.props = props;
	}

	public void setLogger (Logger logger) {
		this.logger = logger;
	}

	public void setVerbose (boolean verbose) {
		this.verbose = verbose;
	}

	public void setStateController(IStateController sc) {
		this.sc = sc;
	}

	public void setNextStage(Stage stage) {
		nextStage = stage;
	}

	public Stage getNextStage() {
		return nextStage;
	}

	/**
	 * Returns a brief description of this class
	 */
	public abstract String getName();

	/**
	 * Returns a brief description of this class
	 */
	public abstract String getDescription();

	public void setStageList(StageList stageList) {
		this.stageList = stageList;
	}

	protected boolean doProcess(Doc doc) {
		// Check onaction
		boolean doProcess = true;
		String action = doc.getElementAttribute("//job", "action");
		if (aOnAction!=null && action!=null && !"".equals(action.trim())) {
			doProcess = false;
			for (int i=0; i<aOnAction.length; i++) {
				if (action.equals(aOnAction[i]))
					return true;
			}
		}
		return doProcess;
	}
	
	public long getProcessingTime() {
		return processingTime;
	}

}
