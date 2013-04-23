package fr.eolya.simplepipeline.stage;

import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;
import fr.eolya.extraction.LanguageDetect;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.LanguageDetector">
 *		<param name="text">text</param>
 *		<param name="target">item_language</param>
 *		<param name="targetdetection">item_languagedetection</param>
 *      <param name="sourcelanguage">source_language</param>
 *      <param name="declaredlanguage">item_declaredlanguage</param>
 *		<param name="candidatelanguages">source_candidatelanguages</param>
 *		<param name="declaredcharset">item_charset</param>
 *		<param name="textdetectionlength">4096</param>
 *      <param name="mindetectionlength">50</param>
 *		<param name="defaultlanguage">en</param>
 *		<param name="languagerecognizerpoolsize">5</param>
 *		<param name="ngp">/Data/Projects/CrawlAnywhere/dev/java/ngramj/ngp/ngp</param>
 *	</stage>
 */
//item_declaredlanguage
public class LanguageDetector extends Stage {
    
    private String targetElement = null;
    private String itemUrlElement = null;
    private String sourceLanguageElement = null;
    private String declaredLanguageElement = null;
    private String candidateLanguagesElement = null;
    //private String declaredCharsetElement = null;
    private String sourceElement = null;
    private int languageDetectionLength = 0;
    private int minLanguageDetectionLength = 0;
    private int poolSize = 0;
    String ngp = "";
    private LanguageDetect ld = null;
    
    /**
     * Perform initialization.
     */
    public void initialize() {
        super.initialize(); 
        targetElement = props.getProperty("target");
        itemUrlElement = props.getProperty("url");
        sourceLanguageElement = props.getProperty("sourcelanguage");
        declaredLanguageElement = props.getProperty("declaredlanguage");
        candidateLanguagesElement = props.getProperty("candidatelanguages");
        //declaredCharsetElement = props.getProperty("declaredcharset");
        sourceElement = props.getProperty("text");
        languageDetectionLength = Integer.parseInt(props.getProperty("textdetectionlength", "4096"));
        minLanguageDetectionLength = Integer.parseInt(props.getProperty("mindetectionlength", "100"));
        poolSize = Integer.parseInt(props.getProperty("languagerecognizerpoolsize", "1"));
        ngp = props.getProperty("ngp");
        ngp = Utils.getValidPropertyPath(ngp, null, "HOME");
        if (logger!=null) logger.log("    profile directory : " + ngp);
        ld = new LanguageDetect(poolSize);   
        String lang = ld.init(ngp);
        if (logger!=null) logger.log("    profile languages : " + lang);
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
        
        if (logger!=null) logger.log("    language detection");
        
        String itemLanguage = "";
        
        String targetDetectionElement = props.getProperty("targetdetection");
        if (targetDetectionElement==null) targetDetectionElement = "";
        
        String sourceLanguageValue = null;
        String languageDetectionList = null;
        String declaredLanguageValue = null;
        String itemUrl = null;
        //String contentCharSet = null;
        
        if (sourceLanguageElement != null && !"".equals(sourceLanguageElement)) {
            sourceLanguageValue = doc.getElementText("//"+sourceLanguageElement).trim().toLowerCase();
        }
        if (declaredLanguageElement != null && !"".equals(declaredLanguageElement)) {
            declaredLanguageValue = doc.getElementText("//"+declaredLanguageElement).trim().toLowerCase();
        }
        if (declaredLanguageValue==null || "".equals(declaredLanguageValue)) declaredLanguageValue = sourceLanguageValue;
        
        if (candidateLanguagesElement != null && !"".equals(candidateLanguagesElement)) {
            languageDetectionList =  doc.getElementText("//"+candidateLanguagesElement).trim();
        }
        if (itemUrlElement != null && !"".equals(itemUrlElement)) {
            itemUrl =  doc.getElementText("//"+itemUrlElement).trim();
        }       
        
        String defaultLanguage = sourceLanguageValue;
        if ("xx".equals(sourceLanguageValue)) {
            defaultLanguage =  props.getProperty("defaultlanguage");
            if (defaultLanguage==null || !defaultLanguage.toLowerCase().trim().matches("[a-z]{2}"))
                defaultLanguage = "en";
        }
        
        //if (declaredCharsetElement != null && !"".equals(declaredCharsetElement)) {
        //    contentCharSet = doc.getElementText("//"+declaredCharsetElement).trim();
        //}
        
        if (!"xx".equals(sourceLanguageValue)) {
            doc.addElement("/job", targetElement, sourceLanguageValue);
            itemLanguage = sourceLanguageValue;
            if ("".equals(targetDetectionElement)) {
                java.util.Date endTime = new java.util.Date();
                processingTime += (endTime.getTime() - startTime.getTime());
                if (nextStage != null)
                    nextStage.processDoc(doc);
                return;
            }
        }
        
        String text=null;        
        if (sourceElement != null && !"".equals(sourceElement)) {
            text = doc.getElementText("//" + sourceElement);
        }
        
        if (text == null || text.length() < minLanguageDetectionLength) {
            if ("xx".equals(sourceLanguageValue)) {
                doc.addElement("/job", targetElement,defaultLanguage.toLowerCase().trim());
            }
            if (logger!=null && verbose) {
                logger.log("        url                 = " + itemUrl);
                logger.log("        text too short or empty => use default language");
                logger.log("        language item       = " + defaultLanguage);
            }
            java.util.Date endTime = new java.util.Date();
            processingTime += (endTime.getTime() - startTime.getTime());
            if (nextStage != null)
                nextStage.processDoc(doc);
            return;
        }
        
        text = text.substring(0, Math.min(text.length(), languageDetectionLength));
        
        String lan = ld.detect2(text, languageDetectionList, "");
        double score = ld.getScore();
        String debug = ld.getDebug();
        
        if ("xx".equals(sourceLanguageValue)) {
            if ("xx".equals(lan) || score < 0.5) {
                doc.addElement("/job", targetElement, defaultLanguage);
                itemLanguage = defaultLanguage;
            } else {
                doc.addElement("/job", targetElement, lan);
                itemLanguage = lan;
            }
        } 
        
        /*
        if (!"xx".equals(sourceLanguageValue) && "xx".equals(lan)) {
            doc.addElement("/job", targetElement, sourceLanguageValue);
            itemLanguage = sourceLanguageValue;
        } else {
            if (!"xx".equals(sourceLanguageValue) && score < 0.5) {
                doc.addElement("/job", targetElement, sourceLanguageValue);
                itemLanguage = sourceLanguageValue;
            } else {
                doc.addElement("/job", targetElement, lan.toLowerCase()); 
                itemLanguage = lan;
            }
        }
         */
        
        if (!"".equals(targetDetectionElement)) {
            doc.addElement("/job", targetDetectionElement, lan.toLowerCase());    
        }
        if (logger!=null && verbose) {
            logger.log("        url                 = " + itemUrl);
            logger.log("        language source     = " + sourceLanguageValue);
            logger.log("        language declared   = " + declaredLanguageValue);
            logger.log("        language detected   = " + lan);
            logger.log("        language item       = " + itemLanguage);
            logger.log("        debug               = " + debug);
        }
        /*
        if (!sourceLanguageValue.equals(declaredLanguageValue) || !sourceLanguageValue.equals(lan)) {
            logger.log("        language ambiguity");
            //lan = ld.detect2(text, languageDetectionList, "");
            //debug = ld.getDebug();
        }
         */
        
        //      String content = text.substring(0, Math.min(text.length(), languageDetectionLength));
        //      LanguageDetect ld = new LanguageDetect(10);
        //		if (lrp == null) lrp = new LanguageRecognizerPool(poolSize);
        //
        //		ArrayList<String> charSetCanditateLanguages = null;
        //		if (languageDetectionList != null && !"".equals(languageDetectionList)) {
        //			// Create a list for the candidate languages
        //			String[] tab = languageDetectionList.split(",");
        //			charSetCanditateLanguages = new ArrayList<String>(Arrays.asList(tab));
        //			for (int i = 0; i < charSetCanditateLanguages.size(); i++) {
        //				charSetCanditateLanguages.set(i,charSetCanditateLanguages.get(i).trim());
        //			}
        //		} else {
        //			// Try to get a candidate languages list according to the charset
        //			CharsetLanguages csu = new CharsetLanguages();
        //			String charSet = contentCharSet;
        //
        //			if (charSet == null || "".equals(charSet))
        //				charSet = "utf-8";
        //			else
        //				charSet = csu.getCharsetCanonicalName(charSet);
        //
        //			if ("utf-8".equals(charSet)) {
        //				String s = csu.getUnicodeBlockDistribution(content);
        //				charSetCanditateLanguages = csu.getUnicodeBlockLanguages(s);
        //			} else
        //				charSetCanditateLanguages = csu.getCharsetLanguages(charSet);
        //		}
        //
        //		String langList = "";
        //		if (charSetCanditateLanguages != null) {
        //			for (int k = 0; k < charSetCanditateLanguages.size(); k++)
        //				langList += charSetCanditateLanguages.get(k) + " ";
        //		} else {
        //			langList = "none";
        //		}
        //
        //		LanguageRecognizerCngram lrCnGram = null;
        //
        //		lrCnGram = (LanguageRecognizerCngram) lrp.get(langList);
        //		if (lrCnGram == null) {
        //			lrCnGram = new LanguageRecognizerCngram();
        //			//String ngp = props.getProperty("ngp");
        //			//ngp = Utils.getValidPropertyPath(ngp, null, "HOME");
        //			if (!"".equals(ngp))
        //				lrCnGram.setLanguageModelsDir(ngp);
        //			lrCnGram.setCandidateLanguages(charSetCanditateLanguages);
        //			lrp.put(langList, lrCnGram);
        //		}
        //
        //		if (lrCnGram != null) {
        //			String lrcngram_language = lrCnGram.RecognizeLanguage(content, 0.5, null);
        //			if ("xx".equals(sourceLanguageValue)) {
        //				doc.addElement("/job", targetElement, lrcngram_language.toLowerCase());	
        //				itemLanguage = lrcngram_language;
        //			}
        //			if (!"".equals(targetDetectionElement)) {
        //				doc.addElement("/job", targetDetectionElement, lrcngram_language.toLowerCase());	
        //			}
        //			if (logger!=null && verbose) {
        //				logger.log("        language source     = " + sourceLanguageValue);
        //				logger.log("        language detected   = " + lrcngram_language);
        //				logger.log("        language item       = " + itemLanguage);
        //			}
        //		}
        
        java.util.Date endTime = new java.util.Date();
        processingTime += (endTime.getTime() - startTime.getTime());
        
        if (nextStage != null)
            nextStage.processDoc(doc);
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
