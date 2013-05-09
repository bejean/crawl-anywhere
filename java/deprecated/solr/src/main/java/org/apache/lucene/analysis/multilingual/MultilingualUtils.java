package org.apache.lucene.analysis.multilingual;

public class MultilingualUtils {
	
	public static void trace (String str) {
		//System.out.println("mltrace - " + str);			
	}

/*	static public String getLangCode(Reader reader) {
		try {
			// Get language code
			StringBuffer langBuffer = new StringBuffer();		
			for (int i = 0; i<4; i++)
			{	
				int ch = reader.read();
				if (i==0 && ch != '[')
					return "";
				if (i==3 && ch != ']')
					return "";
				if (i==1 || i==2)
				{
					if (ch < 'a' || ch > 'z')
						return "";
					langBuffer.append((char)ch);
				}
			}
			
			for (int i = 0; i<12; i++)
			{	
				int ch = reader.read();
				ch = ch;
			}
			
			return langBuffer.toString().toLowerCase();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "";
		}
	}*/

	/*
	static public Stream loadPropertiesInClassPath(String propertiesFileName)
	{
		Properties props = new Properties();
		try
		{
			props.load(ClassLoader.getSystemResourceAsStream(propertiesFileName));
		}
		catch (Exception e)
		{
			return null;
		}
		return props;
	}	
*/
}
