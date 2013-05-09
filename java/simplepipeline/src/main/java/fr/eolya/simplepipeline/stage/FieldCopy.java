package fr.eolya.simplepipeline.stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.document.Doc;

/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.FieldCopy">
 *		<param name="onaction">add</param>
 *      <param name="source">source_element</param>
 *		<param name="sourcecapture">^([a-z\/]*);.*</param>
 *		<param name="groupcapture">1</param>
 *		<param name="target">target_element</param>
 *		<param name="lowercasenormalization">no</param>
 *      <param name="overwrite">yes</param>
 *	</stage>
 */

public class FieldCopy extends Stage {
    
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
        
        java.util.Date startTime = new java.util.Date();
        
        if (logger!=null) logger.log("    field copy");
        
        // Input
        String sourceValue = "";
        String sourceCapture = "";
        String targetValue = "";
        String lowerCaseNormalization = "";
        String overwrite = "";
        
        String sourceElement = props.getProperty("source");
        if (sourceElement != null && !"".equals(sourceElement)) {
            sourceValue = doc.getElementText("//" + sourceElement);
        }
        
        String targetElement = props.getProperty("target");
        
        if (logger!=null) logger.log("    field copy " + sourceElement + " to " + targetElement);
        
        sourceCapture = props.getProperty("sourcecapture");
        if (sourceCapture==null) sourceCapture = "";
        
        lowerCaseNormalization = props.getProperty("lowercasenormalization");
        if (lowerCaseNormalization==null) lowerCaseNormalization = "";
        
        overwrite = props.getProperty("overwrite");
        if (overwrite==null) 
            overwrite = "";
        else
            overwrite = overwrite.toLowerCase();
        
        if (PipelineConfig.isEnabled(lowerCaseNormalization)) sourceValue = sourceValue.toLowerCase();
        if (!"".equals(sourceCapture)) {
            
            int group = 1;
            
            String groupCapture = props.getProperty("groupcapture");
            if (groupCapture!=null && !"".equals(groupCapture))
                group = Integer.parseInt(groupCapture);
            
            // Compile the patten.
            Pattern p = Pattern.compile(sourceCapture);
            
            // Match it.
            Matcher m = p.matcher(sourceValue);
            if (m.find()) targetValue =  m.group(group);
        }
        else {
            targetValue = sourceValue;
        }
        if (targetValue!=null && !"".equals(targetValue)) {
            if (PipelineConfig.isEnabled(overwrite) && doc.getElementText("/job/" + targetElement)!=null && !"".equals(doc.getElementText("/job/" + targetElement))) {
                doc.setElementText("/job/" + targetElement, targetValue);
            } else {
                doc.addElement("/job", targetElement, targetValue);
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
