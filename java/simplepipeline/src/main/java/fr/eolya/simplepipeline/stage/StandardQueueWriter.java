package fr.eolya.simplepipeline.stage;

import java.io.File;
import java.io.IOException;

import fr.eolya.simplepipeline.SimplePipelineUtils;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;
import fr.eolya.utils.XMLUtils;

/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.StandardQueueWriter">
 *		<param name="queuedir">/tmp/out</param>
 *		<param name="tempfilenameprefix">t.</param>
 *		<param name="filenameprefix">j.</param>
 *	</stage>
 */

public class StandardQueueWriter extends Stage {

	private String queueDir = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
		queueDir = props.getProperty("queuedir");
		queueDir = Utils.getValidPropertyPath(queueDir, null, "HOME");
	}
	
	@Override
	public void processDoc(Doc doc) throws Exception {

		// Check onaction
		if (!doProcess(doc)) {
			if (nextStage != null)
				nextStage.processDoc(doc);	
			return;
		}

		java.util.Date startTime = new java.util.Date();
		
		if (logger!=null) logger.log("    output document creation");

		File queue = new File(queueDir);
		if (queue==null || (queue.exists() && !queue.isDirectory())) {
			if (logger!=null) logger.log("        error with target queue : " + queueDir);	
			throw new IOException("error with target queue : " + queueDir);
		}

		String tempQueueDir = SimplePipelineUtils.getTransformedPath(queueDir, doc);
		File tgtDir = new File(tempQueueDir);
		tgtDir.mkdirs();

		XMLUtils.writeToQueue(Utils.getJobUid(), doc.getDocument(), tempQueueDir, props.getProperty("tempfilenameprefix"), props.getProperty("filenameprefix"), true, false);

		java.util.Date endTime = new java.util.Date();
		processingTime += (endTime.getTime() - startTime.getTime());

		if (nextStage != null) {
			nextStage.processDoc(doc);
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
