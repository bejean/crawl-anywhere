package fr.eolya.simplepipeline.stage;

import java.util.Arrays;

import fr.eolya.simplepipeline.document.Doc;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.OnFieldFilter">
 *		<param name="source">item_contenttype</param>
 *		<param name="include"></param>
 *		<param name="exclude">image/jpeg,video/quicktime,application/zip,application/x-zip-compressed,application/octet-stream</param>
 *	</stage>
 */

public class OnFieldFilter extends Stage {

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
	}
	
	@Override
	public void processDoc(Doc doc) throws Exception {

		// Check onaction
		if (!doProcess(doc)) {
			if (nextStage != null)
				nextStage.processDoc(doc);	
			return;
		}

		String source = "";
		
		String sourceElement = props.getProperty("source");
		if (sourceElement != null && !"".equals(sourceElement)) {
			source = doc.getElementText("//" + sourceElement);
		}

		if (logger!=null) logger.log("    filter on " + sourceElement);

		String include = props.getProperty("include");
		String exclude = props.getProperty("exclude");

		if (!isAccepted(source, include, exclude)) {
			if (logger!=null) logger.log("        reject : " + source);
			return;
		}

		if (nextStage != null) {
			nextStage.processDoc(doc);
		}	
	}
	
	private boolean isAccepted(String value, String include, String exclude) {
		if (value == null || "".equals(value))
			return true;

		value = value.toLowerCase();

		if (include != null && !"".equals(include)) {
			// si value est dans include, on retourne true
			String[] aInclude = include.split(",");
			for (int i = 0; i < aInclude.length; i++) {
				if (value.startsWith(aInclude[i].trim()))
					return true;
			}
			return false;
		}

		if (exclude != null && !"".equals(exclude)) {
			// si value est dans exclude, on retourne false
			String[] aExclude = exclude.split(",");
			for (int i = 0; i < aExclude.length; i++) {
				if (value.startsWith(aExclude[i].trim()))
					return false;
			}
			return true;
		}
		return true;
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
