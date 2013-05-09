package org.apache.lucene.analysis.multilingual;

import java.util.Properties;

import org.apache.lucene.analysis.util.XMLConfig;

public class MultilingualConfig extends XMLConfig {

	public MultilingualConfig() {}
	private String configHome = null;
	
	public String getConfigHome() {
		if (configHome==null) configHome = getProperty("/multilingual/configHome", "");
		return configHome;
	}
	
	private String getPath(String lang, String mode) {
		if (!pathExists("/multilingual/" + lang)) lang = "default";
		if (pathExists("/multilingual/" + lang + "/" + mode)) return "/multilingual/" + lang + "/" + mode;
		return "/multilingual/" + lang;
	}
	
	public Properties getCharFilterProperties(String lang, String mode) {
		return getAttributesAsProperties(getPath(lang, mode) + "/charFilter" );
	}

	public Properties getTokenizerProperties(String lang, String mode) {
		return getAttributesAsProperties(getPath(lang, mode) + "/tokenizer" );

	}

	public Properties getFilterProperties(int index, String lang, String mode) {
		return getAttributesAsProperties(getPath(lang, mode) + "/filter[" + Integer.toString(index+1) +"]" );
	}

	public int getFilterCount(String lang, String mode) {
		return getElementCount(getPath(lang, mode) + "/filter");
	}
	
	public String getXml(String lang, String mode) {
		return nodeAsXml(getPath(lang, mode));
	}
}
