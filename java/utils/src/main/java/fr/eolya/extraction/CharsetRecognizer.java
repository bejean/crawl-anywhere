package fr.eolya.extraction;

import java.io.BufferedInputStream;
import java.io.IOException;
import org.mozilla.intl.chardet.* ;

public class CharsetRecognizer {

	public static String[] detect(int lang, BufferedInputStream imp)
	{
		if (lang==0) lang = 6;

		// Initalize the nsDetector() ;
		//	1 => Japanese 
		//  2 => Chinese
		//	3 => Simplified Chinese
		//	4 => Traditional Chinese
		//	5 => Korean
		//	6 => Dont know (default)	
		nsDetector det = new nsDetector(lang) ;

		// Set an observer...
		// The Notify() will be called when a matching charset is found.
		det.Init(new nsICharsetDetectionObserver() {
			public void Notify(String charset) {
				HtmlCharsetDetector.found = true ;
				//System.out.println("CHARSET = " + charset);
			}
		});

		byte[] buf = new byte[1024] ;
		int len;
		boolean done = false ;
		boolean isAscii = true ;

		try {
			while( (len=imp.read(buf,0,buf.length)) != -1) {

				// Check if the stream is only ascii.
				if (isAscii)
					isAscii = det.isAscii(buf,len);

				// DoIt if non-ascii and not done yet.
				if (!isAscii && !done)
					done = det.DoIt(buf,len, false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		det.DataEnd();

		if (isAscii) {
			return "ASCII".split(" ");
		}

		return det.getProbableCharsets() ;
	}

}
