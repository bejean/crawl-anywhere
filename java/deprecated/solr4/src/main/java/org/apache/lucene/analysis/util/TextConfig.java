package org.apache.lucene.analysis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class TextConfig {

	public TextConfig() {
	}

	public static String [] readFile(String filename) {

		if (filename==null || "".equals(filename)) return null;
		try {
			ArrayList<String> list = getFileLines(filename);
			String [] lines = list.toArray(new String[list.size()]);
			return lines;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets the content from a File as StringArray List.
	 *
	 * @param fileName A file to read from.
	 * @return List of individual line of the specified file. List may be empty but not
	 *         null.
	 * @throws IOException
	 */
	public static ArrayList<String> getFileLines(String fileName) throws IOException {

		ArrayList<String> result = new ArrayList<String>();

		File aFile = new File(fileName);

		if (!aFile.isFile()) {
			//throw new IOException( fileName + " is not a regular File" );
			return result; // None
		}

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(aFile));
		}
		catch (FileNotFoundException e1) {
			// TODO handle Exception
			e1.printStackTrace();
			return result;
		}

		String aLine = null;
		while ((aLine = reader.readLine()) != null) {
			aLine = aLine.trim();
			if ("".equals(aLine)) continue;
			if ('#'==aLine.charAt(0)) continue;

			result.add(aLine.trim());
		}
		reader.close();

		return result;
	}

	/***
	 * Fusionne les éléments d'un tableau en une chaîne
	 * @param delim : la chaîne de séparation
	 * @param args : la tableau
	 * @return la chaîne fusionnée
	 */
	private static String implode(String delim, String[] args){
		StringBuffer sb = new StringBuffer();

		for(int i =0; i < args.length; i++){
			if (i > 0)
				sb.append(delim);

			sb.append(args[i]);
		}

		return sb.toString();
	}
}
