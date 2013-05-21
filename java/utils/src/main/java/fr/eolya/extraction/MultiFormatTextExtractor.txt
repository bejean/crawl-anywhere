package fr.eolya.extraction;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import org.jsoup.Jsoup;

import de.jetwick.snacktory.ArticleTextExtractor;
import de.jetwick.snacktory.JResult;
import de.jetwick.snacktory.SHelper;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.CanolaExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import fr.eolya.utils.Utils;
import fr.eolya.utils.http.HttpUtils;

import com.developpez.adiguba.shell.Shell;

public class MultiFormatTextExtractor {

	private String title;
	private String date;
	private String author;
	private String imageUrl;
	private long contentSize;
	private String contentType;

	private String pdfToTextPath = null;
	private String catdocPath = null;
	private String catpptPath = null;
	private String swfToHtmlPath = null;
	private String tmpPath = null;

	private HTMLExtractorJericho jerichoExtractor = null;

	private Parser tikaParser = null;   

	public MultiFormatTextExtractor() {}

	//	public String getPdfToTextPath() {
	//		return pdfToTextPath;
	//	}
	//
	//	public String getCatdocPath() {
	//		return catdocPath;
	//	}
	//
	//	public String getCatpptPath() {
	//		return catpptPath;
	//	}
	//
	//	public String getSwfToHtmlPath() {
	//		return swfToHtmlPath;
	//	}
	//
	//	public String getTempPath() {
	//		return tmpPath;
	//	}

	public void setPdfToTextPath(String pdfToTextPath) {
		this.pdfToTextPath = pdfToTextPath;	
	}

	public void setCatdocPath(String catdocPath) {
		this.catdocPath = catdocPath;	
	}

	public void setCatpptPath(String catpptPath) {
		this.catpptPath = catpptPath;	
	}

	public void setSwfToHtmlPath(String swfToHtmlPath) {
		this.swfToHtmlPath = swfToHtmlPath;
	}

	public void setTempPath(String tempPath) {
		this.tmpPath = tempPath;	
	}

	public String htmlPageToText(String rawData) {
		return htmlPageToText(rawData, "", "");
	}

	public String htmlPageToText(String rawData, String url, String cleanMethod) {

		try {
			String text = "";
			title = "";
			date = "";
			imageUrl = "";

			if (!"".equals(cleanMethod)) {
				if ("boilerpipe_article".equals(cleanMethod)) 
					text = ArticleExtractor.INSTANCE.getText(rawData);
				if ("boilerpipe_default".equals(cleanMethod)) 
					text = DefaultExtractor.INSTANCE.getText(rawData);
				if ("boilerpipe_canola".equals(cleanMethod)) 
					text = CanolaExtractor.INSTANCE.getText(rawData);
				if ("snacktory".equals(cleanMethod)) {
					ArticleTextExtractor extractor = new ArticleTextExtractor();
					JResult res = extractor.extractContent(rawData);
					text = res.getText();
					title = res.getTitle();

					//date = res.getDate(); //  yyyy/mm/dd

					date = SHelper.completeDate(SHelper.estimateDate(url));

					if (date!=null) {
						Pattern p = Pattern.compile("^([0-9]{4})\\/([0-9]{2})\\/([0-9]{2})");
						Matcher m = p.matcher(date);
						if (m.find()) {
							date = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + " 00:00:00";
						}
						else {
							date = "";
						}
					} else {
						date = "";
					}

					//imageUrl = res.getImageUrl();
					imageUrl = HttpUtils.urlGetAbsoluteURL(url, res.getImageUrlBestMatch());
				}
			} 
			contentSize = rawData.length();
			contentType ="text/html";
			if (jerichoExtractor==null) jerichoExtractor = new HTMLExtractorJericho();
			if (!jerichoExtractor.parse(rawData)) return text;
			if ("".equals(title)) title = jerichoExtractor.getTitle();

			String creationDate = jerichoExtractor.getMeta("CreationDate");
			if (creationDate!=null && !"".equals(creationDate)) {

				// Compile the patten.
				Pattern p = Pattern.compile("^([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})");

				// Match it.
				Matcher m = p.matcher(creationDate);
				if (m.find()) {
					date = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + " " + m.group(4) + ":" + m.group(5) + ":" + m.group(6);
				}
				else {
					date = creationDate;
				}
			}

			if (!"".equals(text) || "snacktory".equals(cleanMethod)) return text;

			return jsoupParse(rawData);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String jsoupParse(String html) {
		String html2 = html.replaceAll("(?i)<br[^>]*>", "br2n");
		html2 = html2.replaceAll("(?i)<p[^>]*>", "br2n<p>");
		html2 = html2.replaceAll("(?i)<div[^>]*>", "br2n<div>");
		html2 = html2.replaceAll("(?i)<li[^>]*>", "br2n<li>");
		String text = Jsoup.parse(html2).text();
		text = text.replaceAll("(br2n)+\\s*", "\n");
		return text;
	}

	public String htmlStringToText(String rawData) {

		try {
			contentSize = rawData.length();
			contentType ="text/html";
			if (jerichoExtractor==null) jerichoExtractor = new HTMLExtractorJericho();
			if (!jerichoExtractor.parse(rawData)) return "";
			if ("".equals(title)) title = jerichoExtractor.getTitle();
			return jerichoExtractor.getText();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String pdfInputStreamToHtml(InputStream input) {
		File tempFile = null;
		File tempFile2 = null;
		try {
			// Get a local copy of the file
			tempFile = Utils.createTempFile("tmp", ".pdf", tmpPath);

			OutputStream out=new FileOutputStream(tempFile);
			byte buf[]=new byte[1024];
			int len;
			while((len=input.read(buf))>0)
				out.write(buf,0,len);
			out.close();
			input.close();				

			contentSize = tempFile.length();

			// Convert with PDFTOTEXT
			tempFile2 = Utils.createTempFile("tmp", ".html", tmpPath);

			Shell sh = new Shell(); 

			// pdftotext -enc UTF-8 -raw -q -htmlmeta -eol unix in.pdf out.html
			sh.exec(pdfToTextPath, "-enc", "UTF-8", "-raw", "-q", "-htmlmeta", "-eol", "unix", tempFile.getAbsolutePath(), tempFile2.getAbsolutePath()).consumeAsString();
			tempFile.delete();

			// Load in string and add the <meta http-equiv='Content-Type' content='text/html; charset=utf-8'> line
			InputStreamReader fr1 =  new InputStreamReader(new FileInputStream(tempFile2), "UTF-8");
			BufferedReader br1 = new BufferedReader(fr1);
			StringBuilder sb = new StringBuilder();

			while(br1.ready()){
				String line = br1.readLine();
				sb.append(line).append("\n");
				if ("</head>".equals(line))
				{
					sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>").append("\n");
				}
			}
			br1.close() ;
			tempFile2.delete();

			contentType ="application/pdf";

			return sb.toString();
		} 
		catch (Exception e) {
			if (tempFile!=null && tempFile.exists()) tempFile.delete();
			if (tempFile2!=null && tempFile2.exists()) tempFile2.delete();
			e.printStackTrace();
		}
		return "";
	}

	public String docInputStreamToHtml(InputStream input) {
		contentType ="application/msword";
		return docpptInputStreamToHtml(input, catdocPath, "doc");
	}

	public String pptInputStreamToHtml(InputStream input) {
		contentType ="application/mspowerpoint";
		return docpptInputStreamToHtml(input, catpptPath, "ppt");
	}

	private String docpptInputStreamToHtml(InputStream input, String binPath, String ext) {

		File tempFile = null;
		File tempFile2 = null;
		try {
			// Get a local copy of the file
			tempFile = Utils.createTempFile("tmp", "." + ext, tmpPath);

			OutputStream out=new FileOutputStream(tempFile);
			byte buf[]=new byte[1024];
			int len;
			while((len=input.read(buf))>0)
				out.write(buf,0,len);
			out.close();
			input.close();				

			contentSize = tempFile.length();

			tempFile2 = Utils.createTempFile("tmp", ".html", tmpPath);

			Shell sh = new Shell(); 

			// catdoc -d utf-8 in.pdf > out.html
			String cmd = binPath + " -d" + " utf-8" + " " + tempFile.getAbsolutePath() +  " > " + tempFile2.getAbsolutePath();
			System.out.println("docpptInputStreamToText: " + cmd);

			//sh.exec(binPath, "-d", "utf-8", tempFile.getAbsolutePath(), ">", tempFile2.getAbsolutePath()).consumeAsString();
			@SuppressWarnings("unused")
			String result = sh.command(cmd).consumeAsString();
			tempFile.delete();

			// Load in string and add the <meta http-equiv='Content-Type' content='text/html; charset=utf-8'> line
			InputStreamReader fr1 =  new InputStreamReader(new FileInputStream(tempFile2), "UTF-8");
			BufferedReader br1 = new BufferedReader(fr1);
			StringBuilder sb = new StringBuilder();

			while(br1.ready()){
				String line = br1.readLine();
				sb.append(line).append("\n");
			}
			br1.close() ;
			tempFile2.delete();

			//return htmlStringToText(sb.toString());
			return sb.toString();
		} 
		catch (Exception e) {
			if (tempFile!=null && tempFile.exists()) tempFile.delete();
			if (tempFile2!=null && tempFile2.exists()) tempFile2.delete();
			e.printStackTrace();
		}
		return "";
	}

	public String swfInputStreamToHtml(InputStream input)
	{
		File tempFile = null;
		File tempFile2 = null;

		try {
			if (input!=null && swfToHtmlPath!=null && !"".equals(swfToHtmlPath)) {
				// Get a local copy of the file
				tempFile = File.createTempFile("tmp", ".swf");

				OutputStream out=new FileOutputStream(tempFile);
				byte buf[]=new byte[1024];
				int len;
				while((len=input.read(buf))>0)
					out.write(buf,0,len);
				out.close();
				input.close();					

				// Convert with SWF2HTML
				tempFile2 = File.createTempFile("tmp", ".html");

				Shell sh = new Shell(); 
				sh.exec(swfToHtmlPath, "-o", tempFile2.getAbsolutePath(), tempFile.getAbsolutePath()).consumeAsString();
				tempFile.delete();

				String data = FileUtils.readFileToString(tempFile2, "UTF-8"); 

				tempFile2.delete();

				contentType ="application/x-shockwave-flash";
				return data;
			}
		}
		catch (Exception e) {
			if (tempFile!=null && tempFile.exists()) tempFile.delete();
			if (tempFile2!=null && tempFile2.exists()) tempFile2.delete();
			e.printStackTrace();
		}

		return null;
	}

	public String inputStreamToTextWithTika(InputStream input, String contentType, String outputEncoding, String outputFormat) {
		try {
			if (outputEncoding==null || "".equals(outputEncoding))
				outputEncoding = "UTF-8";

			tikaParser = new AutoDetectParser();    

			Metadata metadata = new Metadata();
			metadata.set(Metadata.CONTENT_TYPE, contentType);

			ParseContext context = new ParseContext();

			try {
				if ("text".equals(outputFormat)) {
					BodyContentHandler handler = new BodyContentHandler(-1); // -1 to disable the write limit of the internal string buffer
					tikaParser.parse(input, handler, metadata, context);

					this.contentType = contentType;
					this.contentSize = handler.toString().length();
					this.title = metadata.get(Metadata.TITLE);
					this.author = metadata.get(Metadata.AUTHOR);
					this.date = metadata.get(Metadata.CREATION_DATE);

					return handler.toString();
				}
				else {
					ByteArrayOutputStream output = new ByteArrayOutputStream();

					SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
					TransformerHandler handler = factory.newTransformerHandler();
					handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
					handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
					if (outputEncoding != null) {
						handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, outputEncoding);
					}
					handler.setResult(new StreamResult(output));

					tikaParser.parse(input, handler, metadata, context);

					String html = output.toString(outputEncoding);	

					this.contentType = contentType;
					this.contentSize = html.length();
					this.title = metadata.get(Metadata.TITLE);
					this.author = metadata.get(Metadata.AUTHOR);

					this.date = "";
					String creationDate = metadata.get(Metadata.CREATION_DATE);
					if (creationDate!=null) {
						Pattern p = Pattern.compile("^([0-9]{4})\\-([0-9]{2})\\-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z");
						Matcher m = p.matcher(creationDate);
						if (m.find()) {
							this.date = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + " " + m.group(4) + ":" + m.group(5) + ":" + m.group(6);
						}
					}
					return html;
				}

			}catch (java.lang.NoSuchMethodError mex){
				System.err.println("Caught NoSuchMethodError");
				mex.printStackTrace();
				return null;
			}catch (Exception ex) {
				System.err.println("Caught Exception");
				ex.printStackTrace();
				return null;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return "";
	}  

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getDate() {
		return date;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getContentType() {
		return contentType;
	}

	public long getContentSize() {
		return contentSize;
	}


	/*
	 * Tests 
	 */
	public static void main(String[] args) {

		try {
			URL url;
			String text;

			MultiFormatTextExtractor extractor = new MultiFormatTextExtractor();
			extractor.setTempPath("/tmp");
			extractor.setPdfToTextPath("/usr/local/bin/pdftotext");

			url = new URL("file:////Data/Projects/Taligentia/CCI/documents tests/3.docx");
			text = extractor.inputStreamToTextWithTika(url.openStream(), "", "", "html");
			Utils.dumpToFile("/Data/Projects/Taligentia/CCI/documents tests/out/3-docx.html", text, false, "utf-8");

			url = new URL("file:////Data/Projects/Taligentia/CCI/documents tests/3.pdf");
			text = extractor.inputStreamToTextWithTika(url.openStream(), "application/pdf", "", "html");
			Utils.dumpToFile("/Data/Projects/Taligentia/CCI/documents tests/out/3-pdf.html", text, false, "utf-8");

			url = new URL("file:////Data/Projects/Taligentia/CCI/documents tests/java.pdf");
			text = extractor.inputStreamToTextWithTika(url.openStream(), "application/pdf", "", "html");
			Utils.dumpToFile("/Data/Projects/Taligentia/CCI/documents tests/out/java-pdf.html", text, false, "utf-8");


			//
			//			// Russian PDF
			//			url = new URL("http://ru.sun.com/java/j2ee/book/platform_Java.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/russian-pdf-1.xml", text);		
			//
			//			text = extractor.pdf ("http://ru.sun.com/java/j2ee/book/platform_Java.pdf");
			//			writeXmlToFile("/tmp/russian-pdf-2.xml", text);		
			//
			//			// Russian DOC
			//			url = new URL("http://www.epam.by/doc/JAVA_web_Training_Program.doc");
			//			text = extractor.inputStreamToText(url.openStream(), "");
			//			writeXmlToFile("/tmp/russian-doc-1.xml", text);			
			//
			//			//text = extractor.pdf ("http://ru.sun.com/java/j2ee/book/platform_Java.pdf");
			//			//writeXmlToFile("/tmp/russian-pdf-2.xml", text);		
			//
			//			// Chinese PDF
			//			url = new URL("http://www.neweraqh.com.cn/download/mobiletradehelp.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/chinese-simpl-pdf-1.xml", text);			
			//
			//			text = extractor.pdf ("http://www.neweraqh.com.cn/download/mobiletradehelp.pdf");
			//			writeXmlToFile("/tmp/chinese-simpl-pdf-2.xml", text);			
			//
			//			// Coréen PDF
			//			url = new URL("http://ettrends.etri.re.kr/PDFData/16-2_031_039.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/coreen-pdf-1.xml", text);	
			//
			//			text = extractor.pdf ("http://ettrends.etri.re.kr/PDFData/16-2_031_039.pdf");
			//			writeXmlToFile("/tmp/coreen-pdf-2.xml", text);	
			//
			//			// Serbe PDF
			//			url = new URL("http://www.cet.rs/cetcitaliste/CitalisteTekstovi/Visenitno_1.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/serbe-pdf-1.xml", text);	
			//
			//			text = extractor.pdf ("http://www.cet.rs/cetcitaliste/CitalisteTekstovi/Visenitno_1.pdf");
			//			writeXmlToFile("/tmp/serbe-pdf-2.xml", text);	
			//
			//			// Grec PDF
			//			url = new URL("http://courses.softlab.ntua.gr/pl1/Labs/lab2_tutorial.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/grec-pdf-1.xml", text);	
			//
			//			text = extractor.pdf ("http://courses.softlab.ntua.gr/pl1/Labs/lab2_tutorial.pdf");
			//			writeXmlToFile("/tmp/grec-pdf-2.xml", text);	
			//
			//			// Arabe PDF
			//			url = new URL("http://parsproshot.com/Upload/DiamondCMS/DiamondCMS.pdf");
			//			text = extractor.pdfInputStreamToText(url.openStream());
			//			writeXmlToFile("/tmp/arabe-pdf-1.xml", text);	
			//
			//			text = extractor.pdf ("http://parsproshot.com/Upload/DiamondCMS/DiamondCMS.pdf");
			//			writeXmlToFile("/tmp/arabe-pdf-2.xml", text);	

		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	static void writeXmlToFile(String fileName, String text) {
		Document doc = DocumentFactory.getInstance().createDocument("utf-8");
		doc.setXMLEncoding("utf-8");
		doc.addElement("text").setText(text);

		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File(fileName)), "UTF-8");
			out.write(doc.asXML(),0,doc.asXML().length());
			out.close();	
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
