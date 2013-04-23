package fr.eolya.simplepipeline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.eolya.simplepipeline.document.Doc;

public class SimplePipelineUtils {

	public static String getTransformedPath(String path, Doc d) {
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
}
