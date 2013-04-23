package fr.eolya.simplepipeline.stage;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import fr.eolya.extraction.MultiFormatTextExtractor;
import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.ActoniaMeta">
 *      <param name="onaction">add</param>
 *      <param name="contenttype">item_contenttype</param>
 *      <param name="contentcharset">item_charset</param>
 *      <param name="source">content</param>
 *	</stage>
 */

public class ActoniaMeta extends Stage {

    private boolean stopPipelineOnError = false;
    private String contentTypeElement = null;
    private String contentCharsetElement = null;
    private String sourceElement = null;

	/**
	 * Perform initialization.
	 */
    public void initialize() {
        super.initialize();
        
        stopPipelineOnError = PipelineConfig.isEnabled(props.getProperty("stoppipelineonerror"));
        contentTypeElement = props.getProperty("contenttype");
        contentCharsetElement = props.getProperty("contentcharset");
        sourceElement = props.getProperty("source");
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

        String contentType = "";
        String contentCharset = "";
        String source = "";

        if (contentTypeElement != null && !"".equals(contentTypeElement)) {
		    contentType = doc.getElementText("//" + contentTypeElement);
		}
		
		if (contentCharsetElement != null && !"".equals(contentCharsetElement)) {
		    contentCharset = doc.getElementText("//" + contentCharsetElement);
		}

        if (sourceElement != null && !"".equals(sourceElement)) {
            source = doc.getElementText("//" + sourceElement);
            if (source == null)
                source = "";
        }
        String rawData = StringEscapeUtils.unescapeHtml(source);
		        
        HtmlCleaner cleaner = new HtmlCleaner();
        //CleanerProperties props = cleaner.getProperties();         
        //props.setXXX(...);
        TagNode node = cleaner.clean(rawData);
        TagNode[] myNodes;
        
        myNodes = node.getElementsByName("a", true);
        for (int i=0;i<myNodes.length;i++)
        {
            String href = myNodes[i].getAttributeByName("href");
            String anchorText = myNodes[i].getText().toString();
            doc.addElement("/job", "actonia_link", href + "|" + anchorText);
        }
        doc.addElement("/job", "actonia_link_count", String.valueOf(myNodes.length));

        myNodes = node.getElementsByName("h1", true);
        for (int i=0;i<myNodes.length;i++)
        {
            doc.addElement("/job", "actonia_h1", myNodes[i].getText().toString());
        }

        myNodes = node.getElementsByName("h2", true);
        for (int i=0;i<myNodes.length;i++)
        {
            doc.addElement("/job", "actonia_h2", myNodes[i].getText().toString());
        }

        myNodes = node.getElementsByName("h3", true);
        for (int i=0;i<myNodes.length;i++)
        {
            doc.addElement("/job", "actonia_h3", myNodes[i].getText().toString());
        }

        myNodes = node.getElementsByName("h4", true);
        for (int i=0;i<myNodes.length;i++)
        {
            doc.addElement("/job", "actonia_h4", myNodes[i].getText().toString());
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
