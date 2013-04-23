package fr.eolya.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class XMLUtils {

	static public String strXmlCleanup(String str) {
		if (str == null)
			return "";

		if (str.indexOf("&#")!=-1) {
			str = str.replaceAll("&#(0|1|2|3|4|5|6|7|8|11|14|15|16|17|18|19|20|21|22|23|24|25|26|27|28|29|30|31);", " ");
		}
		return str;
	}

	static public String writeToQueue(String uid, Document doc, String queueDir, String tempFileNamePrefix, String fileNamePrefix, boolean prettyPrint, boolean isBase64) throws IOException
	{
		String jobsFileNameT = queueDir + "/" + tempFileNamePrefix + uid + ".xml";

		Document doc2 = null;

		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		String str = null;
		try {
			str = doc.asXML();
			if (!isBase64)
				str=XMLUtils.strXmlCleanup(str);
			doc2 = reader.read(new StringReader(str));
			//Utils.dumpToFile("/tmp/ok-" + uid + ".xml", str, false, "");
		} catch (Exception e) {
		    File tmp = new File("/tmp");
		    if (tmp!=null && tmp.isDirectory()) Utils.dumpToFile("/tmp/ko-" + uid + ".xml", str, false, "");
			e.printStackTrace();
			return "";
		}

		try {
			XMLWriter writer = null;
			if (prettyPrint) {
				//OutputFormat format = OutputFormat.createPrettyPrint();
				//format.setLineSeparator("\n"); 
				//format.setNewlines(true);
				OutputFormat format = new OutputFormat();
				format.setEncoding("UTF-8");
				
				format.setNewLineAfterDeclaration(true); 
				//format.setNewLineAfterNTags(1);
				format.setNewlines(true);
				
				writer = new XMLWriter( new OutputStreamWriter(new FileOutputStream(new File(jobsFileNameT)),"UTF-8"), format );				
			}
			else {
				writer = new XMLWriter( new OutputStreamWriter(new FileOutputStream(new File(jobsFileNameT)),"UTF-8"));				
			}
			if (writer!=null) {
				writer.write(doc2);
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		File fT = new File(jobsFileNameT);
		String jobsFileNameR = queueDir + "/" + fileNamePrefix + uid + ".xml";
		File fR = new File(jobsFileNameR);
		fT.renameTo(fR);

		return fR.getAbsolutePath();
	}

//	static public void writeJob(String uid, Document doc, String queueDir, String tempFileNamePrefix, String fileNamePrefix, boolean prettyPrint, boolean isBase64) {
//
//		String jobsFileNameT = queueDir + "/" + tempFileNamePrefix + uid + ".xml";
//
//		Document doc2 = null;
//
//		SAXReader reader = new SAXReader();
//		reader.setValidation(false);
//		try {
//			String str = doc.asXML();
//			if (!isBase64)
//				str=XMLUtils.strXmlCleanup(str);
//			doc2 = reader.read(new StringReader(str));
//		} catch (Exception e) {
//			e.printStackTrace();
//			return;
//		}
//
//		try {
//			XMLWriter writer = null;
//
//			if (prettyPrint) {
//				OutputFormat format = OutputFormat.createPrettyPrint();
//				format.setEncoding("UTF-8");
//				writer = new XMLWriter( new OutputStreamWriter(new FileOutputStream(new File(jobsFileNameT)),"UTF-8"), format );				
//			}
//			else {
//				writer = new XMLWriter( new OutputStreamWriter(new FileOutputStream(new File(jobsFileNameT)),"UTF-8"));				
//			}
//			if (writer!=null) {
//				writer.write(doc2);
//				writer.close();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//
//		File fT = new File(jobsFileNameT);
//		String jobsFileNameR = queueDir + "/" + fileNamePrefix + uid + ".xml";
//		File fR = new File(jobsFileNameR);
//		fT.renameTo(fR);
//	}

}
