package org.apache.lucene.analysis.nterm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.analysis.util.TextConfig;
import org.apache.lucene.util.Version;

public final class NTermStopFilter extends FilteringTokenFilter {

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private List<String> junkWords;
	private List<List<String>> mutualExclusion;
	private boolean option_skip_three = false;
	private boolean option_skip_numbers = false;
	private boolean option_dashes = false;
	private boolean option_period = false;
	private boolean option_include = false;


	public NTermStopFilter(Version matchVersion, TokenStream in, String ntermStopFilterRules) {
		super(true, in);
		readJunkFile(ntermStopFilterRules);
	}

	public NTermStopFilter(Version matchVersion, TokenStream in, List<String> ntermStopFilterRulesLines) {
		super(true, in);
		readJunkFile(ntermStopFilterRulesLines);
	}

	/**
	 * Returns the next input Token whose term() is not a stop word.
	 */
	@Override
	protected boolean accept() throws IOException {

		String text = new String(termAtt.buffer()).substring(0, termAtt.length());

		if (option_include && junkWords != null) {
			if (!junkWords.contains(text)) {
				return false;
			}
		}
		else {
			String aText[] = text.split(" ");
			
			if (junkWords != null) {
				// If the n-gram is a junkword => skip
				if (junkWords.contains(text)) {
					return false;
				}

				// If first or last term of the n-gram are junkwords => skip
				if (junkWords.contains(aText[0]) || junkWords.contains(aText[aText.length-1])) {
					return false;
				}

				// If two successive terms of the n-gram are junkwords => skip
				for (int i=0; i<aText.length-2; i++) {
					if (junkWords.contains(aText[i]) && junkWords.contains(aText[i+1])) {
						return false;
					}
				}
			}

			// If first or last term dot not contains digit or alphabetic letter => skip
			boolean accept = false;
			for (int i = 0; i<aText[0].length() && !accept; i++) {
				if (Character.isLetter(aText[0].charAt(i)) || Character.isDigit(aText[0].charAt(i))) accept = true;
			}
			if (!accept) return false;
			accept = false;
			for (int i = 0; i<aText[aText.length-1].length() && !accept; i++) {
				if (Character.isLetter(aText[aText.length-1].charAt(i)) || Character.isDigit(aText[aText.length-1].charAt(i))) accept = true;
			}
			if (!accept) return false;

			if (mutualExclusion != null) {

				for (int i=0; i<=mutualExclusion.size()-1; i++) {

					int count = 0;
					for (int j=0; j<=aText.length-1 && count<2; j++) {
						if (mutualExclusion.get(i).contains(aText[j])) {
							count++;
						}
					}
					if (count>=2) {
						return false;
					}
				}
			}
			if (option_skip_three && text.length() <= 3) {
				return false;
			}
			if (option_skip_numbers && isNumber(text)) {
				return false;
			}
			if (option_skip_numbers){
				String tmp = text.replace(" ", "");
				if (isNumber(tmp)) {
					return false;
				}
			}
			if (option_dashes && text.indexOf("-") != -1) {
				return false;
			}
			if (option_period && text.indexOf(".") != -1) {
				return false;
			}

			if (text.indexOf("|") != -1) {
				return false;
			}

			//			if (option_period && text.indexOf(".") != -1) {
			//				return false;
			//			}
			//			if (text.indexOf("_stopme_") != -1) {
			//				return false;
			//			}
		}
		return true;
	}


	/**
	 * Junk file is a comma sep list of junk terms and deonminators.
	 * These terms will be broken down into a list and used to reject items within a cloud.
	 * Following options are supported, and must be specified on a individual line
	 * -numbers - ignore numbers
	 * -smallwords - skips words with three or less chars
	 * -dashes - ignore terms with dashes
	 * # - lines startinig with # are ignored.
	 */
	private void readJunkFile(List<String> lines) {

		//System.out.println("readJunkFile 1");

		if (lines==null || lines.size()==0) return;

		//System.out.println("readJunkFile 1.2");

		try {
			List<String> junkList = new ArrayList<String>();

			boolean junkwords = false;
			boolean mutual_exclusion = false;

			for (String line : lines) {

				//System.out.println("line : " + line);

				//read each line of text file
				//while ((line = bufRdr.readLine()) != null) {
				if (line.startsWith("#") || line.trim().length() == 0) {
					continue;
				} else if (line.startsWith("-numbers")) {
					option_skip_numbers = true;
					continue;
				} else if (line.startsWith("-smallwords")) {
					option_skip_three = true;
					continue;
				} else if (line.startsWith("-dashes")) {
					option_dashes = true;
					continue;
				} else if (line.startsWith("-period")) {
					option_period = true;
					continue;
				} else if (line.startsWith("-include")) {
					option_include = true;
					continue;
				} else if (line.startsWith("-junkwords")) {
					junkwords = true;
					mutual_exclusion = false;
					this.junkWords = new ArrayList<String>();
					continue;
				} else if (line.startsWith("-exclusion")) {
					junkwords = false;
					mutual_exclusion = true;
					this.mutualExclusion = new ArrayList<List<String>>();
					continue;
				}
				if (junkwords) {
					StringTokenizer st = new StringTokenizer(line, ",");
					while (st.hasMoreTokens()) {
						this.junkWords.add(st.nextToken().trim().replace("\\s+", " ").toLowerCase());
					}
				}
				if (mutual_exclusion) {

					StringTokenizer st = new StringTokenizer(line, ",");
					List<String> l = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						l.add(st.nextToken().trim().replace("\\s+", " ").toLowerCase());
					}
					this.mutualExclusion.add(l);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return;
	}

	private void readJunkFile(String filename) {

		//System.out.println("readJunkFile 2");
		if (filename==null || "".equals(filename)) return;
		//System.out.println("readJunkFile 2.2 - " + filename);
		try {
			//ArrayList<String> list = getFileContent(filename);
			//String [] lines = list.toArray(new String[list.size()]);
			String [] lines = TextConfig.readFile( filename);
			readJunkFile(Arrays.asList(lines));
			return;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private static boolean isNumber(final String s) {
		for (char c : s.toCharArray())
			if (!Character.isDigit(c) && c!='.' && c != ',')
				return false;
		return true;
	}
}
