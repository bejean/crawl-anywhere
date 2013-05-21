package fr.eolya.simplepipeline.stage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

//import fr.eolya.extraction.MultiFormatTextExtractor;
import fr.eolya.extraction.ScriptsWrapper;
import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Base64;
import fr.eolya.utils.Utils;
import fr.eolya.utils.http.HttpUtils;

import fr.eolya.extraction.tika.TikaWrapper;


/*
 * Configuration snippet sample :
 * 
 *	<stage classname="fr.eolya.simplepipeline.stage.DocTextExtractor">
 *		<param name="onaction">add</param>
 *		<param name="scriptspath">/opt/crawler/config/scripts</param>
 *		<param name="url">item_url</param>
 *		<param name="contenttype">item_contenttype</param>
 *      <param name="contentcharset">item_charset</param>
 *      <param name="cleanmethod">item_clean_method</param>
 *		<param name="cleanmethoddefault">boilerpipe_article|boilerpipe_default|boilerpipe_canola</param>
 *		<param name="source">content</param>
 *		<param name="target">text</param>
 *		<param name="pdftotextpath">/usr/bin/pdftotext</param>
 *		<param name="tmppath">/opt/crawler/tmp</param>
 *		<param name="stoppipelineonerror">yes</param>
 *		<param name="stoppipelineonemptytext">yes</param>
 *	</stage>
 */

public class DocTextExtractor extends Stage {

	private String parserLanguage = "";
	private String parserTitle = "";
	private String parserEncoding = "";
	private String parserContentType = "";
	private String parserContentSize = "";
	private String parserDate = "";
	private String parserText = "";
	private String parserImageUrl = "";

	private boolean stopPipelineOnError = false;
	private boolean stopPipelineOnEmptyText = false;
	private String scriptsPath = null;
	private String urlElement = null;
	private String contentTypeElement = null;
	private String contentCharsetElement = null;
	private String cleanMethodDefault = null;
	private String cleanMethodElement = null;
	private String sourceElement = null;
	private String targetElement = null;

	private String tmpPath = null;
	private String pdfToTextPath = null;

	private boolean VERBOSE = true;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();

		stopPipelineOnError = PipelineConfig.isEnabled(props.getProperty("stoppipelineonerror"));
		stopPipelineOnEmptyText = PipelineConfig.isEnabled(props.getProperty("stoppipelineonemptytext"));
		scriptsPath = props.getProperty("scriptspath");
		scriptsPath = Utils.getValidPropertyPath(scriptsPath, null, "HOME");
		urlElement = props.getProperty("url");
		contentTypeElement = props.getProperty("contenttype");
		contentCharsetElement = props.getProperty("contentcharset");
		cleanMethodDefault = props.getProperty("cleanmethoddefault");
		cleanMethodElement = props.getProperty("cleanmethod");
		sourceElement = props.getProperty("source");
		targetElement = props.getProperty("target");

		tmpPath = props.getProperty("tmppath");
		tmpPath = Utils.getValidPropertyPath(tmpPath, null, "HOME");

		pdfToTextPath = props.getProperty("pdftotextpath");
		pdfToTextPath = Utils.getValidPropertyPath(pdfToTextPath, null, "HOME");
	}


	@Override
	public void processDoc(Doc doc) throws Exception {

		// Check onaction
		if (!doProcess(doc)) {
			if (nextStage != null)
				nextStage.processDoc(doc);	
			return;
		}

		java.util.Date startTime = new java.util.Date();

		if (logger!=null) logger.log("    text extraction");

		// Input
		String contentType = "";
		String contentCharset = "";
		String cleanMethod = "";
		String source = "";
		String text = "";
		String url = "";
		String accountId = "";

		if (urlElement != null && !"".equals(urlElement)) {
			url = doc.getElementText("//" + urlElement);
		}

		if (contentTypeElement != null && !"".equals(contentTypeElement)) {
			contentType = doc.getElementText("//" + contentTypeElement);
		}

		if (contentCharsetElement != null && !"".equals(contentCharsetElement)) {
			contentCharset = doc.getElementText("//" + contentCharsetElement);
		}

		accountId = doc.getElementText("//account_id");

		String scriptName = "";
		if (accountId!=null && !"".equals(accountId))
			scriptName = ScriptsWrapper.getScriptName (scriptsPath + "/" + accountId, url);

		if (scriptName==null || "".equals(scriptName))
			scriptName = ScriptsWrapper.getScriptName (scriptsPath, url);

		if (scriptName!=null && !"".equals(scriptName) && logger!=null) logger.log("        using script : " + scriptName);

		if (cleanMethodElement != null && !"".equals(cleanMethodElement))
			cleanMethod = doc.getElementText("//" + cleanMethodElement);

		if ("".equals(cleanMethod) && !"".equals(cleanMethodDefault))
			cleanMethod = cleanMethodDefault;

		if (sourceElement != null && !"".equals(sourceElement)) {
			source = doc.getElementText("//" + sourceElement);
			if (source == null)
				source = "";
		}

		boolean success = true;
		try {
			if (!"".equals(source)) {
				String sourceEncoding = doc.getElementAttribute("//" + sourceElement, "encoding");

				InputStream inputSource = null;
				String rawData = null;

				if (contentType.startsWith("text/")) {  // or "application/x-shockwave-flash" ???
					if ("base64".equals(sourceEncoding)) {
						if (contentCharset==null || "".equals(contentCharset)) contentCharset = "UTF-8";
						rawData = IOUtils.toString(new Base64.InputStream(new ByteArrayInputStream(source.getBytes()), Base64.DECODE), contentCharset.toUpperCase());
					} else {
						rawData = StringEscapeUtils.unescapeHtml4(source);
					}
				}
				else {
					inputSource = new ByteArrayInputStream(source.getBytes());
					if ("base64".equals(sourceEncoding))
						inputSource = new Base64.InputStream(inputSource, Base64.DECODE);
				}

				if (inputSource != null || rawData != null) {

					if (inputSource != null) rawData = null;

					try {
						text = extract(inputSource, rawData, contentType, cleanMethod, url, scriptName);
						if (text==null) {
							success = false;
							stageList.setStagesStatus(StageList.STATUS_ERROR);
							if (logger!=null) logger.log("        parsing error (text null)");
							java.util.Date endTime = new java.util.Date();
							processingTime += (endTime.getTime() - startTime.getTime());
							return;							
						}
					}
					catch (Exception e){
						e.printStackTrace();
						success = false;
						if (logger!=null) logger.log("        parsing error");
						return;
					}

					if (text != null && !"".equals(text)) {
						doc.addElement("/job", targetElement, text);
					}
					else {
						if (logger!=null) logger.log("        empty document");
						if (stopPipelineOnEmptyText) {
							java.util.Date endTime = new java.util.Date();
							processingTime += (endTime.getTime() - startTime.getTime());
							return;
						}
					}

					if (parserLanguage!=null && !"".equals(parserLanguage))
						doc.addElement("/job", "parser_language", parserLanguage);

					if (parserTitle!=null && !"".equals(parserTitle))
						doc.addElement("/job", "parser_title", parserTitle);

					if (parserEncoding!=null && !"".equals(parserEncoding))
						doc.addElement("/job", "parser_charset", parserEncoding);

					if (parserContentType!=null && !"".equals(parserContentType))
						doc.addElement("/job", "parser_contentype", parserContentType);

					if (parserContentSize!=null && !"".equals(parserContentSize))
						doc.addElement("/job", "parser_contensize", parserContentSize);

					if (parserDate!=null && !"".equals(parserDate))
						doc.addElement("/job", "parser_date", parserDate);

					if (parserImageUrl!=null && !"".equals(parserImageUrl))
						doc.addElement("/job", "parser_imageurl", parserImageUrl);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			success = false;
			if (logger!=null) logger.log("        parsing error");
		}

		java.util.Date endTime = new java.util.Date();
		processingTime += (endTime.getTime() - startTime.getTime());

		if (success || !stopPipelineOnError)
			if (nextStage != null)
				nextStage.processDoc(doc);
	}

	private String extract(InputStream input, String rawData, String contentType, String cleanMethod, String url, String scriptName) {
		String text = doExtract(input, rawData, contentType, cleanMethod, url, scriptName);
		if (text==null) return null;

		if (parserTitle==null || "".equals(parserTitle.trim()))						
			parserTitle = HttpUtils.urlGetFileName(url);

		if (text!=null && !"".equals(text)) {
			if (url!=null && !"".equals(url) && scriptName!=null && !"".equals(scriptName)) {
				if (logger!=null && verbose) {
					logger.log("    url          = " + url);
					logger.log("    scripts name = " + scriptName);
				}
				HashMap<String, String> m = ScriptsWrapper.cleanText(url, text, null, scriptName); 
				if (m!=null && m.size()>0) {
					text = m.get("text");
				}
			}
		}
		return text;
	}

	private class HtmlParser {
		private String title = null;
		private String date = null;
		private String html = null;

		public HtmlParser() {};

		public boolean parse(String rawData, String contentType, String url, String scriptName) {
			html = rawData;
			if (url!=null && !"".equals(url) && scriptName!=null && !"".equals(scriptName)) {
				if (logger!=null && verbose) {
					logger.log("    url          = " + url);
					logger.log("    scripts name = " + scriptName);
				}
				HashMap<String, String> m = ScriptsWrapper.htmlParse(url, rawData, contentType, null, scriptName); 
				if (m!=null && m.size()>0) {
					html = m.get("page");
					date = m.get("date");
					title = m.get("title");
					if (logger!=null && verbose) {
						logger.log("    title        = " + title);
						logger.log("    date         = " + date);
					}
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
			return true;
		}

		public String getBestTitle(String parserTitle) {
			if (title!=null && !"".equals(title)) return title;
			return parserTitle;
		}

		public String getBestDate(String parserDate) {
			if (date!=null && !"".equals(date)) return date;
			return parserDate;
		}

		public String getBestHtml(String rawData) {
			if (html!=null && !"".equals(html)) return html;
			return rawData;
		}
	}

	private String doExtract(InputStream input, String rawData, String contentType, String cleanMethod, String url, String scriptName) {

		try {			
			// text/plain
			if (contentType.startsWith("text/plain")) {
				parserText = rawData;
				parserTitle = Utils.strGetStartingText(parserText, 80);
				parserContentType = contentType;
				parserContentSize = String.valueOf(parserText.length());
				return parserText;
			} 

			HtmlParser htmlParser = new HtmlParser();

			TikaWrapper wrapper = null;

			// application/pdf
			if (contentType.startsWith("application/pdf")) {
				wrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT, TikaWrapper.CONTENT_TYPE_PDF);
				wrapper.setPdfToTextPath(pdfToTextPath);
			} else {
				// text/html
				if (contentType.startsWith("text/html")) {
					
					if (htmlParser.parse(rawData, contentType, url, scriptName)) {
						//cleanMethod = ""; // if parse script was use, disable any clean algorithm
						rawData = htmlParser.getBestHtml(rawData);
					}
					
					if (input==null && rawData!=null) input = new ByteArrayInputStream(rawData.getBytes());
					if (input==null) return "";

					String outputFormat = TikaWrapper.OUTPUT_FORMAT_TEXT;
					if ("boilerpipe_article".equals(cleanMethod)) 
						outputFormat = TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_ARTICLE;
					if ("boilerpipe_default".equals(cleanMethod)) 
						outputFormat = TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_DEFAULT;
					if ("boilerpipe_canola".equals(cleanMethod)) 
						outputFormat = TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_BOILERPIPE_CANOLA;
					if ("snacktory".equals(cleanMethod)) 
						outputFormat = TikaWrapper.OUTPUT_FORMAT_TEXT_MAIN_SNACKTORY;
					
					wrapper = new TikaWrapper(outputFormat, TikaWrapper.CONTENT_TYPE_HTML);
					
				} else {
					wrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT);
				}
			}
			
			wrapper.setTempPath(tmpPath);
			wrapper.process(input);
			
			parserText = wrapper.getText();
			parserContentType = wrapper.getMetaContentType();
			if (contentType.startsWith("text/html")) {
				parserTitle = htmlParser.getBestTitle(wrapper.getMetaTitle());
				parserDate = htmlParser.getBestDate(wrapper.getMetaCreated());
			} else {
				parserTitle = wrapper.getMetaTitle();
				parserDate = wrapper.getMetaCreated();				
			}

			//parserContentSize = Long.toString(extractor.getContentSize());

			return parserText;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
