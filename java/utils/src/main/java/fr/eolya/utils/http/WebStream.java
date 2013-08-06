package fr.eolya.utils.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import fr.eolya.extraction.CharsetRecognizer;

public class WebStream {

	private InputStream input = null;
	private String rawData = null;
	private String charSet = null;
	private String declaredLanguage = null;
	private String declaredEncoding = null;
	private String contentType = null;
	private File tempFile = null;
	private String metaEncodingBalise = null;

	public WebStream (InputStream input, String declaredEncoding, String contentType, String contentEncoding)
	{
		this.input = input;

		if (contentEncoding!=null && "gzip".equals(contentEncoding)) {
			try {
				this.input = new GZIPInputStream(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.declaredEncoding = declaredEncoding;
		this.contentType = contentType;
	}

	public void clear()
	{
		if (tempFile!=null)
			tempFile.delete();
	}

	public String getCharSet()
	{
		if (charSet!=null)
			return charSet;

		String tempExt = ".txt";
		Hashtable<String,Integer> encodingFreq = new Hashtable<String,Integer>();

		String encodingInContentType = "";

		try {		
			if (contentType == null) contentType = "";
			contentType = contentType.toLowerCase();

			if (contentType.startsWith("text/html"))
			{
				encodingInContentType = HttpUtils.parseCharacterEncoding(contentType);
				if (encodingInContentType!=null && !"".equals(encodingInContentType))
					encodingFreq.put(encodingInContentType, 1);
				contentType = "text/html";
				tempExt = ".html";
			}	

			// On sauvegarde le flux dans un fichier temporaire
			tempFile = File.createTempFile("tmp", tempExt);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
			int c;
			while((c = input.read()) != -1) {
				out.writeByte(c);
			}
			input.close();
			out.close();

			// charset detection with jchardet
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFile));			
			String[] aCharSet = CharsetRecognizer.detect(0, bis);
			bis.close();
			String encoding = HttpUtils.filtreEncoding(aCharSet[0].toLowerCase());
			if (encoding!=null && !"".equals(encoding) && !"nomatch".equals(encoding)) {
				if (encodingFreq.containsKey(encoding))
					encodingFreq.put(encoding, encodingFreq.get(encoding) + 2);
				else
					encodingFreq.put(encoding, 2);
			}

			// charset detection with icu
			bis = new BufferedInputStream(new FileInputStream(tempFile));
			CharsetDetector detector;
			detector = new CharsetDetector();
			detector.enableInputFilter(true);
			detector.setText(bis);
			if (declaredEncoding!=null && !"".equals(declaredEncoding))
				detector.setDeclaredEncoding(declaredEncoding);
			CharsetMatch[] matches = detector.detectAll();
			bis.close();
			encoding = HttpUtils.filtreEncoding(matches[0].getName().toLowerCase());
			if (encoding!=null && !"".equals(encoding)) {
				if (encodingFreq.containsKey(encoding))
					encodingFreq.put(encoding, encodingFreq.get(encoding) + 2);
				else
					encodingFreq.put(encoding, 2);
			}

			// lecture du charset dans la balise contentType dans le fichier			
			InputStreamReader fr = null;
			if (encodingInContentType!=null && !"".equals(encodingInContentType)) {
				fr = new InputStreamReader(new FileInputStream(tempFile), encodingInContentType);
			}
			else {
				fr = new InputStreamReader(new FileInputStream(tempFile), "UTF-8");				
			}

			BufferedReader br = new BufferedReader(fr);

			boolean find = false;
			while(br.ready() && !find){
				String line = br.readLine();
				String lineLowerCase = line.toLowerCase();
				if (lineLowerCase.indexOf("charset")>=0 && lineLowerCase.indexOf("http-equiv")>=0 && lineLowerCase.indexOf("content-type")>=0 ) {

					int off = Math.max(lineLowerCase.indexOf("charset"), lineLowerCase.indexOf("http-equiv"));
					off = Math.max(off, lineLowerCase.indexOf("content-type"));
					if (lineLowerCase.indexOf(">", off)!=-1) {
						encoding = HttpUtils.parseCharacterEncoding(lineLowerCase);
						if (encoding!=null && !"".equals(encoding))
						{
							if (encodingFreq.containsKey(encoding))
								encodingFreq.put(encoding, encodingFreq.get(encoding) + 3);
							else
								encodingFreq.put(encoding, 3);
						}

						find = true;
					}
				}
			}
			br.close() ;

			// Get the best candidate
			Vector<String> v = new Vector<String>(encodingFreq.keySet());
			Iterator<String> it = v.iterator();
			int max = 0;
			encoding = "";
			while (it.hasNext()) {
				String element =  (String)it.next();
				//System.out.println( element + " " + encodingFreq.get(element));
				if (encodingFreq.get(element)>max)
				{
					max = encodingFreq.get(element);
					encoding = element;
				}
			}
			this.charSet = encoding;
			return charSet;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}	

		return "";
	}

	public String getString()
	{
		if (rawData!=null)
			return rawData;

		try{	
			if (contentType == null) contentType = "";
			contentType = contentType.toLowerCase();

			if (charSet==null){
				getCharSet();	
			}

			StringBuffer buffer = new StringBuffer();
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFile));	

			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(bis, charSet);
			}
			catch (Exception e) {
				isr = new InputStreamReader(bis, "UTF-8");
			}
			if (isr==null) return null;

			int ch;
			while ((ch=isr.read())>-1)
			{
				buffer.append((char)ch);
			}
			isr.close();
			String ret = buffer.toString();

			if (contentType.startsWith("text/html") && metaEncodingBalise!=null && !"".equals(metaEncodingBalise))
			{
				// on met le bon charset dans la balise contenttype
				ret = ret.replace(metaEncodingBalise, "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + charSet + "\">" );				
			}
			rawData = ret;
			return ret;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}	
		return "";
	}


	public String getDeclaredLanguage()
	{
		if (declaredLanguage!=null)
			return declaredLanguage;

		if (rawData==null) getString();
		return HttpUtils.getHtmlDeclaredLanguage(rawData);
	}

//	public static void main(String[] args) throws IOException {
//
//		InputStreamReader fr = null;
//		fr = new InputStreamReader(new FileInputStream("/tmp/tmp251061790944559968.html"), "ISO-8859-1");
//
//		BufferedReader br = new BufferedReader(fr);
//
//		boolean find = false;
//		while(br.ready() && !find){
//			String line = br.readLine();
//			String lineLowerCase = line.toLowerCase();
//		}
//		br.close() ;
//	}
}
