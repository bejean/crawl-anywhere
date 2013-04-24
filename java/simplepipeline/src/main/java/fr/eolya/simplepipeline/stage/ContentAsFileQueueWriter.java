package fr.eolya.simplepipeline.stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import fr.eolya.simplepipeline.SimplePipelineUtils;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Base64;
import fr.eolya.utils.MimeTypes;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 * <stage classname="fr.eolya.simplepipeline.stage.ContentAsFileQueueWriter">
 *     <param name="source">content</param>
 *     <param name="contenttype">item_contenttype</param>
 *     <param name="queuedir">/tmp/out</param>
 *     <param name="tempfilenameprefix">t.</param>
 *     <param name="acceptedContentType">application/pdf,application/xhtml+xml,text/html</param>
 * </stage>
 */

public class ContentAsFileQueueWriter extends Stage {

	private String queueDir = null;
	private String sourceElement = null;
	private String[] aContentType = null;
	private String contentTypeElement = null;
	private String tempFileNamePrefix = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
		queueDir = props.getProperty("queuedir");
		queueDir = Utils.getValidPropertyPath(queueDir, null, "HOME");
		sourceElement = props.getProperty("source");
		String acceptedContentType = props.getProperty("acceptedContentType");
		if (acceptedContentType!=null && !"".equals(acceptedContentType)) {
			aContentType = acceptedContentType.trim().split("\\s*,\\s*");
		}
		contentTypeElement = props.getProperty("contenttype");
		tempFileNamePrefix = props.getProperty("tempfilenameprefix");
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
		//if (queue==null || !queue.exists() || !queue.isDirectory()) {
		if (queue==null || (queue.exists() && !queue.isDirectory())) {
			if (logger!=null) logger.log("        error with target queue : " + queueDir);	
			throw new IOException("error with target queue : " + queueDir);
		}

		String contentType = "";
		if (contentTypeElement != null && !"".equals(contentTypeElement)) {
			contentType = doc.getElementText("//" + contentTypeElement);
			
			if (contentType==null || "".equals(contentType)) {
				if (nextStage != null) {
					nextStage.processDoc(doc);
				}
				return;
			}
			
			// Compile the patten.
			Pattern p = Pattern.compile("^([a-zA-Z_\\/\\-\\.\\+]*).*");

			// Match it.
			Matcher m = p.matcher(contentType);
			if (m.find()) contentType = m.group(1);
			
			if (aContentType!=null) {
				boolean accepted = false;
				for (int i = 0; i < aContentType.length && !accepted; i++) {
					if (aContentType[i].equals(contentType)) 
						accepted = true;
				}
				if (!accepted) {
					if (nextStage != null) {
						nextStage.processDoc(doc);
					}
					return;
				}
			}
		}

		String source = "";
		if (sourceElement != null && !"".equals(sourceElement)) {
			source = doc.getElementText("//" + sourceElement);
			if (source == null)
				source = "";
		}

		if (!"".equals(source)) {
			String sourceEncoding = doc.getElementAttribute("//" + sourceElement, "encoding");

			InputStream inputSource = null;
			String rawData = null;

			if (!"base64".equals(sourceEncoding) && contentType.startsWith("text/")) {  // or "application/x-shockwave-flash" ???
				rawData = StringEscapeUtils.unescapeHtml4(source);
			}
			else {
				inputSource = new ByteArrayInputStream(source.getBytes());

				if ("base64".equals(sourceEncoding))
					inputSource = new Base64.InputStream(inputSource, Base64.DECODE);
			}

			// extention a partir du type mime
			String ext = MimeTypes.lookupExtension(contentType);
			if (ext==null) ext="unknown";
			// nom du fichier cible
			
			String fileName = doc.getFileName();
			File f = new File(fileName);
			fileName = f.getName().substring(2);

			String tempQueueDir = SimplePipelineUtils.getTransformedPath(queueDir, doc);
			File tgtDir = new File(tempQueueDir);
			tgtDir.mkdirs();
			
			String tempFileName = tempQueueDir + "/" + tempFileNamePrefix + fileName.substring(0, fileName.length()-3) + ext;
			fileName = tempQueueDir + "/" + fileName.substring(0, fileName.length()-3) + ext;

			if (inputSource != null || rawData != null) {
				if (inputSource == null) {
					inputSource = new ByteArrayInputStream(rawData.getBytes());
				}
				OutputStream out=new FileOutputStream(tempFileName);
				byte buf[]=new byte[1024];
				int len;
				while((len=inputSource.read(buf))>0)
					out.write(buf,0,len);
				out.close();
				inputSource.close();				
			}
			File f2 = new File(tempFileName);
			f2.renameTo(new File(fileName));
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
