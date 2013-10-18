package fr.eolya.simplepipeline.stage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.FieldMapping">
 *      <param name="onaction">add</param>
 *		<param name="source">item_contenttype</param>
 *		<param name="sourcecapture">^([a-zA-Z_\/\-\.\+]*).*</param>
 *		<param name="groupcapture">1</param>
 *		<param name="target">item_contenttyperoot</param>
 *		<param name="overwritetarget">ifempty | always</param>
 *		<param name="mappingdefinitionfile">config/pipeline/contenttypemapping.txt</param>
 *		<param name="lowercasenormalization">no</param>
 *      <param name="sourcecaptureisdefault">yes</param>
 *	</stage>
 */

public class FieldMapping extends Stage {

	private HashMap<String, String> mappings = null;
	
	private String sourceElement = null;
	private String targetElement = null;
	private String sourceCapture = null;
	private String groupCapture = null;
	private String sourceCaptureIsDefault = null;
	private String lowerCaseNormalization = null;
	private String overwritetarget = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
		String mappingDefinitionFile = props.getProperty("mappingdefinitionfile");
		mappingDefinitionFile = Utils.getValidPropertyPath(mappingDefinitionFile, null, "HOME");
		if (mappingDefinitionFile==null) mappingDefinitionFile = "";

		if (logger!=null) logger.log("    FieldMapping - mappingdefinitionfile = " + mappingDefinitionFile);

		lowerCaseNormalization = props.getProperty("lowercasenormalization");
		if (lowerCaseNormalization==null) lowerCaseNormalization = "";

		if (!"".equals(mappingDefinitionFile)) {
			try {
				mappings = loadFieldMapping(mappingDefinitionFile, PipelineConfig.isEnabled(lowerCaseNormalization));
				if (logger!=null && verbose) logger.log("    FieldMapping - mappingdefinitionfile size = " + String.valueOf(mappings.size()));
			} catch (java.io.FileNotFoundException e) {
				mappings = null;
				if (logger!=null && verbose) logger.log("    FieldMapping - mappingdefinitionfile not found");
			} catch (IOException e) {
				e.printStackTrace();
				mappings = null;
				if (logger!=null && verbose) logger.log("    FieldMapping - mappingdefinitionfile error");
			}
		}
		
		sourceElement = props.getProperty("source");
		targetElement = props.getProperty("target");

		sourceCapture = props.getProperty("sourcecapture");
		if (sourceCapture==null) sourceCapture = "";

		sourceCaptureIsDefault = props.getProperty("sourcecaptureisdefault");
		if (sourceCaptureIsDefault==null) sourceCaptureIsDefault = "";

		overwritetarget = props.getProperty("overwritetarget");
		if (overwritetarget==null) overwritetarget = "always";

		groupCapture = props.getProperty("groupcapture");
		if (groupCapture==null) groupCapture = "";

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
		
		if (logger!=null) logger.log("    field mapping");

		// Input
		String sourceValue = "";
		String targetValue = "";

		if (sourceElement != null && !"".equals(sourceElement))
			sourceValue = doc.getElementText("//" + sourceElement);

		if (targetElement != null && !"".equals(targetElement))
			targetValue = doc.getElementText("//" + targetElement);
		
		if (!"".equals(targetValue) && "ifempty".equals(overwritetarget)) {
			if (nextStage != null)
				nextStage.processDoc(doc);	
			return;
		}
			
		if (logger!=null) logger.log("    field mapping " + sourceElement + " to " + targetElement);
		if (logger!=null) logger.log("    source value before capture = " + sourceValue);

		//if (PipelineConfig.isEnabled(lowerCaseNormalization)) sourceValue = sourceValue.toLowerCase();
		if (!"".equals(sourceCapture)) {
			if (logger!=null) logger.log("    source capture = " + sourceCapture);

			int group = 1;

			if (groupCapture!=null && !"".equals(groupCapture))
				group = Integer.parseInt(groupCapture);

			String capturedValue = Utils.regExpExtract(sourceValue, sourceCapture, group);
			if (capturedValue!=null) {
				sourceValue = capturedValue;
				if (logger!=null) logger.log("    match");
			}
			else {
				sourceValue = "";
				if (logger!=null) logger.log("    no match");
			}
			
			/*
			// Compile the patten.
			Pattern p = Pattern.compile(sourceCapture);

			// Match it.
			Matcher m = p.matcher(sourceValue);
			if (m.find()) sourceValue = m.group(group);
			*/
			if (logger!=null) logger.log("    source value after capture = " + sourceValue);
		}

		//targetValue = sourceValue;
		if (mappings!=null) {
			String key = sourceValue.trim();
			if (PipelineConfig.isEnabled(lowerCaseNormalization)) key = key.toLowerCase();
			String mappedValue = mappings.get(key);
			if (logger!=null) logger.log("    mapped value = " + mappedValue);
			if (mappedValue!=null)
				targetValue = mappedValue;
			else {
				//if (!"".equals(sourceCapture) && PipelineConfig.isEnabled(sourceCaptureIsDefault)) {
				if (PipelineConfig.isEnabled(sourceCaptureIsDefault)) {
					if (logger!=null) logger.log("    no mapping found -> use capture as default");
					targetValue = sourceValue;
				} 
				else {
					if (logger!=null) logger.log("    no mapping found -> do not use capture as default");					
				}
			}
			//doc.addElement("/job", targetElement, targetValue);			
		}
		else {
			if (PipelineConfig.isEnabled(sourceCaptureIsDefault)) {
				if (logger!=null) logger.log("    no mapping not defined -> use capture as default");
				targetValue = sourceValue;
			} 
			else {
				if (logger!=null) logger.log("    no mapping not defined -> do not use capture as default");					
			}
			//if (logger!=null) logger.log("    no mapping defined");
		}
		if (targetValue!=null && !"".equals(targetValue)) {
			if (doc.existElement("//" + targetElement))
				doc.setElementText("//" + targetElement, targetValue);	
			else
				doc.addElement("/job", targetElement, targetValue);	
		}

		java.util.Date endTime = new java.util.Date();
		processingTime += (endTime.getTime() - startTime.getTime());

		if (nextStage != null) {
			nextStage.processDoc(doc);
		}		
	}

	private HashMap<String, String> loadFieldMapping(String fileName, boolean lowerCaseNormalisation) throws IOException {
		HashMap<String, String> map = new HashMap<String, String>();

		InputStream stream = new FileInputStream(fileName);
		InputStreamReader streamReader = new InputStreamReader(stream);
		BufferedReader reader = new BufferedReader(streamReader);

		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!"".equals(line) && !line.startsWith("#")) {
				//line = line.replaceAll("\\s+", "");
				String[] aItems = line.split("=");
				String key = aItems[0].trim();
				if (lowerCaseNormalisation) key = key.toLowerCase();
				if (aItems.length==2)
					map.put(key, aItems[1].trim());
				else
					map.put(key, "");
			}
		}
		return map;
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
