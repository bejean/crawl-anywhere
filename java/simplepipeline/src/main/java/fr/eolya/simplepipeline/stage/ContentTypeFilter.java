package fr.eolya.simplepipeline.stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.eolya.simplepipeline.document.Doc;


/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.ContentTypeFilter">
 *      <param name="onaction">add</param>
 *      <param name="contentTypeElement">item_contenttype</param>
 *		<param name="acceptedContentType">application/x-shockwave-flash,text/html,text/plain,application/pdf,application/xhtml+xml,application/msword,application/xml,application/rtf,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.presentationml.presentation</param>
 *	</stage>
 */
public class ContentTypeFilter extends Stage {

	private String sourceElement = null;
	private String[] aContentType = null;
	
	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
		sourceElement = props.getProperty("contentTypeElement");
		String acceptedContentType = props.getProperty("acceptedContentType");
		if (acceptedContentType!=null && !"".equals(acceptedContentType)) {
			aContentType = acceptedContentType.trim().split("\\s*,\\s*");
		}
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

		if (logger!=null) logger.log("    content type filtering");

		// Input
		String sourceValue = "";

		//String sourceElement = props.getProperty("contentTypeElement");
		if (sourceElement != null && !"".equals(sourceElement)) {
			sourceValue = doc.getElementText("//" + sourceElement);
			
			// Compile the patten.
			Pattern p = Pattern.compile("^([a-zA-Z_\\/\\-\\.\\+]*).*");

			// Match it.
			Matcher m = p.matcher(sourceValue);
			if (m.find()) sourceValue =  m.group(1);
			
		}

		if (logger!=null) logger.log("    filtering contentype : " + sourceValue);

		if (sourceValue!=null && !"".equals(sourceValue)) {
			if (aContentType!=null) {
				boolean accepted = false;
				for (int i = 0; i < aContentType.length && !accepted; i++) {
					if (aContentType[i].equals(sourceValue)) 
						accepted = true;
				}
				if (!accepted) {
					stageList.setStagesStatus(StageList.STATUS_OK);
					if (logger!=null) logger.log("        rejected");
					java.util.Date endTime = new java.util.Date();
					processingTime += (endTime.getTime() - startTime.getTime());
					return;							
				}
			}
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
