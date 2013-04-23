package fr.eolya.simplepipeline.stage;

import java.io.File;

import org.apache.commons.io.FileUtils;

import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 * 	<connector classname="fr.eolya.simplepipeline.connector.FileQueueCleanUp">
 *		<param name="source_file">content_file</param>
 * 		<param name="onsuccessmoveto">/tmp/in/_success</param>
 *		<param name="onerrormoveto">/tmp/in/_error</param>
 * 	</connector>
 */
public class FileQueueCleanUp extends Stage {

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();
	}
	
	@Override
	public void processDoc(Doc doc) throws Exception {

		boolean success = true;
		try {
			if (nextStage != null) {
				nextStage.processDoc(doc);
			}
		}
		catch (Exception e) {
			success = false;
			throw e;
		}
		finally {

			String source_file = "";

			String sourceFileElement = props.getProperty("source_file");
			if (sourceFileElement != null && !"".equals(sourceFileElement)) {
				source_file = doc.getElementText("//" + sourceFileElement);
				if (source_file != null || !"".equals(source_file)) {
					if (logger!=null) logger.log("    cleaning file : " + source_file);
					fileDone(new File (source_file), success);
				}
			}
		}
	}

	private void fileDone(File f, boolean success) {
		String tgtPath;
		if (success) {
			tgtPath = props.getProperty("onsuccessmoveto");
		} else {
			tgtPath = props.getProperty("onerrormoveto");
		}
		tgtPath = Utils.getValidPropertyPath(tgtPath, null, "HOME");

		if (tgtPath!=null && !"".equals(tgtPath)) {
			File tgtDir = new File(tgtPath);
			tgtDir.mkdirs();
			File f2 = new File(tgtPath + "/" + f.getName());
			try {
				logger.log("Move : " + f.getAbsolutePath() + "/" + f.getName() + " -> " +  f2.getAbsolutePath() + "/" + f2.getName());
				FileUtils.moveFile(f, f2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (f.exists())
			f.delete();	
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
