package fr.eolya.simplepipeline;

import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;

public class SimplePipelineUtils {

	public static String getTransformedPath(String path, Doc d) {
		if (d==null) return path;
		try
		{
			Pattern p = Pattern.compile("\\{([a-zA-Z0-9\\/\\-_:]*)\\}");
			Matcher m = p.matcher(path);
			StringBuffer sb = new StringBuffer();
			if (!m.find())
				return path;
			m.reset();
			while(m.find()) {
				String group = m.group().substring(1, m.group().length()-1);
				String replacement = "";
				if (d!=null)
					replacement = d.getElementText(group);
				if (replacement!=null)
					replacement = replacement.replaceAll("[\\/\\\\\\?%\\*:\\|\"<>\\.\\+\\[\\]]", "_");
				m.appendReplacement(sb,replacement);
			}
			return sb.toString().replaceAll("\\/{2,}", "/");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "";
		}
	}
	public static void fileDone(File f, Doc d, boolean success, Properties props, Logger logger, String id) {
		String tgtPath;
		if (success) {
			tgtPath = props.getProperty("onsuccessmoveto");
		} else {
			tgtPath = props.getProperty("onerrormoveto");
		}
		tgtPath = Utils.getValidPropertyPath(tgtPath, null, "HOME");

		if (tgtPath!=null && !"".equals(tgtPath)) {
			tgtPath = SimplePipelineUtils.getTransformedPath(tgtPath, d);
			File tgtDir = new File(tgtPath);
			tgtDir.mkdirs();
			File f2 = new File(tgtPath + "/" + f.getName());
			if (f2.exists()) f2.delete();
			try {
				String msg = "";
				if (StringUtils.isNotBlank(id)) msg += "[" + id + "] ";
				msg += "File done - move : " + f.getAbsolutePath() + " -> " + f2.getAbsolutePath();
				logger.log(msg);
				FileUtils.moveFile(f, f2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (f.exists())
			f.delete();	
	}
}
