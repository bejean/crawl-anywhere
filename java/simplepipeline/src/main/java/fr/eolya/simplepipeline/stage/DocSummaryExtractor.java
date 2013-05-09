package fr.eolya.simplepipeline.stage;

import fr.eolya.simplepipeline.document.Doc;

/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.DocSummaryExtractor">
 *		<param name="source">text</param>
 *		<param name="target">summary</param>
 *		<param name="maxsize">500</param>
 *	</stage>
 */

public class DocSummaryExtractor extends Stage {

	private String sourceElement = null;
	private String targetElement = null;
	private int maxsize = 0;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
		sourceElement = props.getProperty("source");
	    targetElement = props.getProperty("target");
		maxsize = Integer.parseInt(props.getProperty("maxsize"));
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

		if (logger!=null) logger.log("    summary extraction");

		String source = "";

		if (sourceElement != null && !"".equals(sourceElement))
			source = doc.getElementText("//" + sourceElement);
	
		if (targetElement != null && !"".equals(targetElement)) {
			String summary = source.replaceAll("[\\s]+", " ").trim();
			summary = summary.substring(0, Math.min(summary.length(), maxsize));
			int index = summary.lastIndexOf(" ");
			if (index>0) summary = summary.substring(0, index);
			doc.addElement("/job", targetElement, summary);
		}

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
