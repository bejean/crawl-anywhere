/*
 * Licensed to Eolya and Dominique Bejean under one
 * or more contributor license agreements. 
 * Eolya licenses this file to you under the 
 * Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fr.eolya.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A generic utilities class
 */
public class Utils {

	/**
	 * Build absolute path
	 * 
	 * @param path Relative or absolute path for searched file
	 * @param homePath Home path
	 * @param homeEnvVarName Environment variable name for homePath
	 * @return
	 */
	static public String getValidPropertyPath(String path, String homePath, String homeEnvVarName) {

		if (path==null || "".equals(path)) return "";
		File f = new File(path);
		if (f.isAbsolute()) return path;

		if (homePath!=null && !"".equals(homePath)) {
			String tempPath = homePath;
			tempPath = tempPath + "/" + path;
			f = new File(tempPath);
			if (f.isAbsolute()) return tempPath;
		}
		if (homeEnvVarName!=null && !"".equals(homeEnvVarName)) {
			String homeEnvEnvValue = System.getenv(homeEnvVarName);
			if (homeEnvEnvValue!=null && !"".equals(homeEnvEnvValue)) {
				String tempPath = homeEnvEnvValue;
				tempPath = tempPath + "/" + path;
				f = new File(tempPath);
				if (f.isAbsolute()) return tempPath;
			}
		}
		return "";
	}

	/**
	 * Read properties
	 * 
	 * @param propertiesFileName Properties file name
	 * @return
	 */	
	static public Properties loadPropertiesByFileName(String propertiesFileName) {
		Properties props = new Properties();
		try {
			InputStream inStream = new FileInputStream(propertiesFileName);
			props.load(inStream);
			inStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return props;
	}

	/**
	 * Read properties by searching the properties file in the classpath
	 * 
	 * @param propertiesFileName Properties file name
	 * @return
	 */	
	static public Properties loadPropertiesInClassPath(String propertiesFileName) {
		Properties props = new Properties();
		try {
			InputStream input = ClassLoader.getSystemResourceAsStream((propertiesFileName));
			props.load(input);
		} catch (Exception e) {
			return null;
		}
		return props;
	}

	/**
	 * Get current day name (sunday, ... , saturday)
	 * 
	 * @param 
	 * @return
	 */	
	public static String getCurrentDayName()
	{
		Calendar cal = new GregorianCalendar();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

		switch (dayOfWeek)
		{
		case 1:
			return "sunday";
		case 2:
			return "monday";
		case 3:
			return "thursday";
		case 4:
			return "wednesday";
		case 5:
			return "tuesday";
		case 6:
			return "friday";
		case 7:
			return "saturday";
		}
		return "";
	}

	/**
	 * Get current hour of day
	 * 
	 * @param 
	 * @return
	 */	
	public static int getCurrentHour()
	{
		Calendar cal = new GregorianCalendar();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		return hour;
	}

	/**
	 * Get current process PID
	 * 
	 * @return
	 */
	public static String getProcessId() {
		String pid="1";
		try {
			pid = ManagementFactory.getRuntimeMXBean().getName();
			String[] aItems = pid.split("@");
			if (aItems!=null && aItems.length>0)
				return aItems[0];
		} catch (Exception e) {}
		return pid;
	}

	
	/**
	 * Return true if the string is a numeric (-1.0 return true)
	 * 
	 * @return
	 */
	public static boolean isStringNumeric( String str )
	{
	    DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
	    char localeMinusSign = currentLocaleSymbols.getMinusSign();

	    if ( !Character.isDigit( str.charAt( 0 ) ) && str.charAt( 0 ) != localeMinusSign ) return false;

	    boolean isDecimalSeparatorFound = false;
	    char localeDecimalSeparator = currentLocaleSymbols.getDecimalSeparator();

	    for ( char c : str.substring( 1 ).toCharArray() )
	    {
	        if ( !Character.isDigit( c ) )
	        {
	            if ( c == localeDecimalSeparator && !isDecimalSeparatorFound )
	            {
	                isDecimalSeparatorFound = true;
	                continue;
	            }
	            return false;
	        }
	    }
	    return true;
	}

	
	/**
	 * Sleep current thread
	 * 
	 * @param ms sleep duration in ms
	 * @return
	 */
	static public void sleep(int ms) {
		try {
			Thread.sleep(ms); // ms
		} catch (InterruptedException e) {}
	}
	
	/**
	 * This String utility or util method can be used to merge 2 arrays of
	 * string values. If the input arrays are like this array1 = {"a", "b" ,
	 * "c"} array2 = {"c", "d", "e"} Then the output array will have {"a", "b" ,
	 * "c", "d", "e"}
	 * 
	 * This takes care of eliminating duplicates and checks null values.
	 * 
	 * @param values
	 * @return
	 */
	public static String[] mergeStringArrays(String array1[], String array2[]) {
	    
	    if (array1 == null || array1.length == 0)
	        return array2;
	    if (array2 == null || array2.length == 0)
	        return array1;
	    List<String> array1List = Arrays.asList(array1);
	    List<String> array2List = Arrays.asList(array2);
	    List<String> result = new ArrayList<String>(array1List);  
	    List<String> tmp = new ArrayList<String>(array1List);
	    tmp.retainAll(array2List);
	    result.removeAll(tmp);
	    result.addAll(array2List);  
	    return ((String[]) result.toArray(new String[result.size()]));
	}
	
	public static File createTempFile(String prefix, String suffix, String directory) throws IOException {
		File tmpFile = null;
		if (directory == null)
			directory = "";
		if (!"".equals(directory))
			tmpFile = new File(directory);
		if (tmpFile == null || !tmpFile.exists() || !tmpFile.isDirectory())
			return File.createTempFile(prefix, suffix);
		else
			return File.createTempFile(prefix, suffix, tmpFile);
	}

	public static void dumpToFile(String fileName, String buffer, boolean append) {
		dumpToFile(fileName, buffer, append, "");
	}

	public static void dumpToFile(String fileName, String buffer, boolean append, String encoding) {
		try {
			if (!"".equals(encoding)) {
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(fileName)), encoding));
				out.write(buffer);
				out.close();
			} else {
				BufferedWriter writer = null;
				writer = new BufferedWriter(new FileWriter(fileName, append));
				writer.write(buffer);
				writer.close();
			}
		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println(e);
		}
	}

	static public String getJobUid() {
		Date date = new Date();
		String time = String.valueOf(date.getTime());
		return time + "-" + UUID.randomUUID().toString();
	}
	
	static public String padZero(int value, int digit) {
		return padZero(String.valueOf(value), digit);
	}
	
	static public String padZero(String value, int digit) {
		if (value.length()<digit)
			return "0000000000".substring(0, digit-value.length()) + value;
		else
			return value;
	}
	
	public static File[] getListFileAlphaOrder(File dir) {
		File files[] = dir.listFiles();
		Arrays.sort(files, new DirAlphaComparator());
		return files;
	}

	public static String loadFileInString(String url) {
		BufferedReader in = null;
		String s = "";
		try {
			in = new BufferedReader(new FileReader(new File(url)));
			String line = "";
			while ((line = in.readLine()) != null) {
				s += line;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException ex) {
			}
		}
		return s;
	}
	
	public static String regExpExtract(String source, String pattern, int group) {
		// Compile the patten.
		Pattern p = Pattern.compile(pattern);

		// Match it.
		Matcher m = p.matcher(source);
		if (m.find()) return m.group(group);
		return null;
	}
	
	static public String strGetStartingText(String str, int len) {

		String temp = str.substring(0, Math.min(len, str.length()));
		int offset = temp.lastIndexOf(" ");
		if (offset!=-1)
			temp = temp.substring(0,offset);

		return temp;
	}
}
