package fr.eolya.simplepipeline.stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.eolya.simplepipeline.document.Doc;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.TagFilter">
 *		<param name="tagfield">source_tags</param>
 *		<param name="textfield">text</param>
 *	</stage>
 */

public class TagFilter extends Stage {

	private String sourceElement = null;
	private String textElement = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();

		sourceElement = props.getProperty("tagfield", "");
		textElement = props.getProperty("textfield", "");
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

		if (logger!=null) logger.log("    Tag Filter");

		// Input
		String sourceValue = "";
		String targetValue = "";
		String textValue = "";

		if (sourceElement != null && !"".equals(sourceElement))
			sourceValue = doc.getElementText("//" + sourceElement);

		if (!"".equals(sourceValue)) {
			if (textElement != null && !"".equals(textElement))
				textValue = doc.getElementText("//" + textElement);

			String[] aValues = sourceValue.split(",");

			for (int i=0; i<aValues.length; i++) {	
				aValues[i] = aValues[i].trim();
				Pattern p = Pattern.compile("\\b" + aValues[i].replaceAll(" +", "\\\\s+") + "\\s?\\b", Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(textValue);
				if (m.find()) {
					if (!"".equals(targetValue)) targetValue += ",";
					targetValue += aValues[i];
				}
			}
			doc.setElementText("//" + sourceElement, targetValue);
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
