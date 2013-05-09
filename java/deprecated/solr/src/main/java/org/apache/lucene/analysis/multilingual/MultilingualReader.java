package org.apache.lucene.analysis.multilingual;

import java.io.Reader;
import java.io.StringReader;

public class MultilingualReader {

	public String langCode = "en";
	StringBuffer buffer = null;
	private Reader reader = null;

	public MultilingualReader() {}

	public Reader analyze(Reader reader)
	{
		try {
			langCode = "en";
			this.reader = null;
			
			// Get language code
			buffer = new StringBuffer();	

			int ch = reader.read();
			boolean bOk = true;
			for (int i = 0; i<4 && ch!=-1 && bOk; i++)
			{	
				buffer.append((char)ch);

				if (i==0 && ch != '[')
					bOk = false;
				
				if (i==3)
				{
					if (ch == ']')
						langCode = buffer.toString().toLowerCase().substring(1, 3); 
					else
						bOk = false;
				}
				
				if (i==1 || i==2)
					if (ch < 'a' || ch > 'z')
						bOk = false;
				
				if (bOk) ch = reader.read();
			}
			if (!"".equals(langCode))
			{
				buffer = new StringBuffer();	
				buffer.append("    ");
				if (ch!=-1) buffer.append((char)ch);
			}
			ch = reader.read();
			while (ch!=-1)
			{
				buffer.append((char)ch);
				ch = reader.read();
			}
			this.reader = new StringReader(buffer.toString());
			MultilingualUtils.trace (buffer.toString());			
			return this.reader;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		MultilingualReader mr = new MultilingualReader();
		mr.analyze(new StringReader("[xx]abcd"));
		mr.analyze(new StringReader("abcd"));
		mr.analyze(new StringReader("[xxabcd"));
	}

}
