package fr.eolya.extraction;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

public class LanguageDetect {
    
    private long processingTime = 0;
    private String languageDetectionList = "";
    private String langList = "";
    private String debug = "";
    //private int poolSize = 0;
    private double score = 0.0;
    
    private LanguageRecognizerLanguageDetection lrLD;
    
    public LanguageDetect (int poolSize) {
        //this.poolSize = poolSize;
    }
    
    public long getProcessingTime() {
        return processingTime;
    }
    
    public String getLanguageDetectionList() {
        return languageDetectionList;
    }
    
    public String getLangList() {
        return langList;
    }
    
    public double getScore() {
        return score;
    }
    
    public String getDebug() {
        return debug;
    }
    
    public String init(String ngp) {
        lrLD = new LanguageRecognizerLanguageDetection();
        if (!"".equals(ngp)) lrLD.setLanguageModelsDir(ngp);
        return lrLD.init();
    }
    
    
    //    @SuppressWarnings("unused")
    //    public String detect(String text, int maxLen, String languageDetectionList, String contentCharSet, String ngp) {
    //        this.debug = "";
    //        this.langList = "";
    //        
    //        java.util.Date startTime = new java.util.Date();
    //        
    //        if (lrp == null) lrp = new LanguageRecognizerPool(poolSize);
    //        
    //        String content = text.substring(0, Math.min(text.length(), maxLen));
    //        
    //        ArrayList<String> charSetCanditateLanguagesSuggested = null;
    //        if (languageDetectionList != null && !"".equals(languageDetectionList)) {
    //            this.languageDetectionList =languageDetectionList;
    //            // Create a list for the candidate languages
    //            charSetCanditateLanguagesSuggested = new ArrayList<String>(Arrays.asList(languageDetectionList.split(",")));
    //            for (int i = 0; i < charSetCanditateLanguagesSuggested.size(); i++) {
    //                charSetCanditateLanguagesSuggested.set(i,charSetCanditateLanguagesSuggested.get(i).trim());
    //            }
    //        }
    //        
    //        // Try to get a candidate languages list according to the charset
    //        ArrayList<String> charSetCanditateLanguagesDetected = null;
    //        try {
    //            CharsetLanguages csu = new CharsetLanguages();
    //            String charSet = contentCharSet;
    //            
    //            if (charSet == null || "".equals(charSet))
    //                charSet = "utf-8";
    //            else
    //                charSet = csu.getCharsetCanonicalName(charSet);
    //            
    //            if ("utf-8".equals(charSet)) {
    //                String s = csu.getUnicodeBlockDistribution(content);
    //                charSetCanditateLanguagesDetected = csu.getUnicodeBlockLanguages(s);
    //            } else
    //                charSetCanditateLanguagesDetected = csu.getCharsetLanguages(charSet);
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //            return null;
    //        }
    //        
    //        ArrayList<String> charSetCanditateLanguages = null;
    //        if (charSetCanditateLanguagesSuggested.size()==0 && charSetCanditateLanguagesDetected.size()>0)
    //            charSetCanditateLanguages = charSetCanditateLanguagesDetected;
    //            
    //        if (charSetCanditateLanguagesSuggested.size()>0 && charSetCanditateLanguagesDetected.size()==0)
    //            charSetCanditateLanguages = charSetCanditateLanguagesSuggested;
    //        
    //        if (charSetCanditateLanguagesSuggested.size()>0 && charSetCanditateLanguagesDetected.size()>0) {
    //            ArrayList<String> list = new ArrayList<String>();
    //            for (String t : charSetCanditateLanguagesSuggested) {
    //                if(charSetCanditateLanguagesDetected.contains(t)) {
    //                    list.add(t);
    //                }
    //            }
    //            if (list.size()>0)
    //                charSetCanditateLanguages = list;
    //        }
    //        
    //        if (charSetCanditateLanguages != null) {
    //            for (int k = 0; k < charSetCanditateLanguages.size(); k++)
    //                langList += charSetCanditateLanguages.get(k) + " ";
    //        } else {
    //            langList = "none";
    //        }
    //        
    //        LanguageRecognizerCngram lrCnGram = null;
    //        
    //        lrCnGram = (LanguageRecognizerCngram) lrp.get(langList);
    //        if (lrCnGram == null) {
    //            lrCnGram = new LanguageRecognizerCngram();
    //            if (lrCnGram == null) return null;
    //            if (!"".equals(ngp)) lrCnGram.setLanguageModelsDir(ngp);
    //            lrCnGram.setCandidateLanguages(charSetCanditateLanguages);
    //            lrp.put(langList, lrCnGram);
    //        }
    //        
    //        String lrcngram_language = lrCnGram.RecognizeLanguage(content, 0.5, null);
    //
    //        LanguageRecognizerLanguageDetection lr = null;      
    //        lr = (LanguageRecognizerLanguageDetection) lrp.get(langList);
    //        if (lr == null) {
    //            lr = new LanguageRecognizerLanguageDetection();
    //            if (lr == null) return null;
    //            if (!"".equals(ngp)) lr.setLanguageModelsDir(ngp);
    //            lr.setCandidateLanguages(charSetCanditateLanguages);
    //            lrp.put(langList, lr);
    //        }
    //        
    //        java.util.Date endTime = new java.util.Date();
    //        this.processingTime += (endTime.getTime() - startTime.getTime());
    //        this.debug = lrLD.getDebug();
    //        
    //        return lrcngram_language;
    //    }
    
    @SuppressWarnings("unused")
    public String detect2(String text, String languageDetectionList, String contentCharSet) {
        this.debug = "";
        this.langList = "";
        this.score = 0.0;
        
        if (lrLD==null) return null;
        
        java.util.Date startTime = new java.util.Date();
                
        ArrayList<String> canditateLanguagesSuggested = null;
        if (languageDetectionList != null && !"".equals(languageDetectionList)) {
            this.languageDetectionList =languageDetectionList;
            // Create a list for the candidate languages
            canditateLanguagesSuggested = new ArrayList<String>(Arrays.asList(languageDetectionList.split(",")));
            for (int i = 0; i < canditateLanguagesSuggested.size(); i++) {
                canditateLanguagesSuggested.set(i,canditateLanguagesSuggested.get(i).trim());
            }
        }
        
        // Try to get a candidate languages list according to the charset
        ArrayList<String> canditateLanguagesDetected = null;
        try {
            CharsetLanguages csu = new CharsetLanguages();
            String charSet = contentCharSet;
            
            if (charSet == null || "".equals(charSet))
                charSet = "utf-8";
            else
                charSet = csu.getCharsetCanonicalName(charSet);
            
            if ("utf-8".equals(charSet)) {
                String s = csu.getUnicodeBlockDistribution(text);
                canditateLanguagesDetected = csu.getUnicodeBlockLanguages(s);
            } else
                canditateLanguagesDetected = csu.getCharsetLanguages(charSet);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        ArrayList<String> charSetCanditateLanguages = null;
        if ((canditateLanguagesSuggested==null || canditateLanguagesSuggested.size()==0) && (canditateLanguagesDetected!=null && canditateLanguagesDetected.size()>0))
            charSetCanditateLanguages = canditateLanguagesDetected;
        
        if ((canditateLanguagesSuggested!=null && canditateLanguagesSuggested.size()>0) && (canditateLanguagesDetected==null || canditateLanguagesDetected.size()==0))
            charSetCanditateLanguages = canditateLanguagesSuggested;
        
        if ((canditateLanguagesSuggested!=null && canditateLanguagesSuggested.size()>0) && (canditateLanguagesDetected!=null && canditateLanguagesDetected.size()>0)) {
            ArrayList<String> list = new ArrayList<String>();
            for (String t : canditateLanguagesSuggested) {
                if(canditateLanguagesDetected.contains(t)) {
                    list.add(t);
                }
            }
            if (list.size()>0)
                charSetCanditateLanguages = list;
        }
        
        if (charSetCanditateLanguages != null) {
            for (int k = 0; k < charSetCanditateLanguages.size(); k++)
                langList += charSetCanditateLanguages.get(k) + " ";
        } else {
            langList = "none";
        }
        
        String language = lrLD.RecognizeLanguage(text, 0.5, charSetCanditateLanguages).substring(0, 2);
        
        java.util.Date endTime = new java.util.Date();
        this.processingTime += (endTime.getTime() - startTime.getTime());
        this.debug = lrLD.getDebug();
        this.score = lrLD.getScore();
        
        return language;
    }
    
    public static void main(String[] args) {
        
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }
        
        Getopt g = new Getopt("LanguageDetect", args, "m:l:c:n:t:f:");
        g.setOpterr(false);
        int c;
        
        int maxLen = 0;
        String languageDetectionList ="";
        String contentCharSet = "utf-8";
        String ngp = "";
        String text = "";
        String file = "";
        
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'm':
                    maxLen = Integer.parseInt(g.getOptarg());
                    break;
                    
                case 'l':
                    languageDetectionList = g.getOptarg();
                    break;
                    
                case 'c':
                    contentCharSet = g.getOptarg();
                    break;
                    
                case 'n':
                    ngp = g.getOptarg();
                    break;
                    
                case 't':
                    text = g.getOptarg();
                    break;
                    
                case 'f':
                    file = g.getOptarg();
                    break;
            }
        }
        
        try {
            if (file!=null) {
                String encoding = "UTF-8";
                if (!"".equals(contentCharSet)) encoding = contentCharSet;
                text = FileUtils.readFileToString(new File(file), encoding);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        LanguageDetect ld = new LanguageDetect(1);
        ld.init(ngp);
        text = text.substring(0, Math.min(text.length(), maxLen));
        String lan = ld.detect2(text, languageDetectionList, contentCharSet);
        
        System.out.println(lan);
        System.out.println(ld.getDebug());
        
    }
    
    private static void usage() {
        
    }
    
}
