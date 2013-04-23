package fr.eolya.simplepipeline.stage;

import fr.eolya.simplepipeline.document.Doc;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.BpiCleanUp">
 *		<param name="source">source_tags</param>
 *		<param name="cleanuppattern">(.*):"([^"]*)"</param>
 *	</stage>
 */

public class BpiCleanUp extends Stage {

	private String sourceElement = null;
	private String cleanupPatternCapture = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();

		sourceElement = props.getProperty("source", "");
		cleanupPatternCapture = props.getProperty("cleanuppattern", "");
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

		if (logger!=null) logger.log("    BpiCleanUp");

		// Input
		String sourceValue = "";
		String targetValue = "";

		if (sourceElement != null && !"".equals(sourceElement))
			sourceValue = doc.getElementText("//" + sourceElement);

		String[] aValues = sourceValue.split(",");
		
		for (int i=0; i<aValues.length; i++) {
			if (!aValues[i].matches(cleanupPatternCapture)) {
				if (!"".equals(targetValue)) targetValue += ",";
				targetValue += aValues[i];
			} 
			else {
				if (logger!=null) logger.log("    BpiCleanUp remove - " + aValues[i]);
			}
				
		}
		doc.setElementText("//" + sourceElement, targetValue);

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
