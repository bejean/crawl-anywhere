package fr.eolya.extraction;

import java.util.ArrayList;
import java.util.List;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class LanguageRecognizerLanguageDetection {
    
    private String debug = "";
    private ArrayList<String> candidateLanguages = null;
    private String languageModelsDir = null;
    private Detector detector = null;
    private Double score = 0.0;
    
    public LanguageRecognizerLanguageDetection () {}
    
    public void setCandidateLanguages(ArrayList<String> languages) {
        if (languages==null){
            if (this.candidateLanguages==null)
                return;
        }
        else{
            if (languages.equals(this.candidateLanguages)) 
                return;
        }
        this.candidateLanguages = languages;
        detector = null;
    }
    
    public void setLanguageModelsDir(String languageModelsDir) {
        if (languageModelsDir==null){
            if (this.languageModelsDir==null)
                return;
        }
        else {
            if (languageModelsDir.equals(this.languageModelsDir)) 
                return;
        }
        this.languageModelsDir = languageModelsDir;
        detector = null;
        
    }
    
    public String init() {
        try {
            List<String> langlist = DetectorFactory.getLangList();
            if (langlist==null || langlist.size()==0) {
                //DetectorFactory.loadProfile(this.languageModelsDir, this.candidateLanguages);
                DetectorFactory.loadProfile(this.languageModelsDir);
                List<String> l = DetectorFactory.getLangList();
                String ret = "";
                if (l!=null) {
                for (int i=0; i<l.size(); i++)
                    ret += l.get(i) + " ";
                }
                return ret.trim();
            }
        } catch (LangDetectException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public String RecognizeLanguage(String text, double minScore, ArrayList<String> postDetectionAcceptedLanguages) {
        this.debug = "";
        try {
            score = 0.0;
            detector = DetectorFactory.create();
            
            if (detector!=null)
            {
                detector.append(text);
                ArrayList<Language> res = detector.getProbabilities();
                
                for (int i = 0; i < res.size(); i++) {
                    this.debug+=res.get(i).lang + ":" + String.valueOf(res.get(i).prob) + "|";
                }
                
                ArrayList<String> langList = this.candidateLanguages;
                if (postDetectionAcceptedLanguages!=null) {
                    if (langList!=null)
                        langList.retainAll(postDetectionAcceptedLanguages);
                    else
                        langList = postDetectionAcceptedLanguages;
                }
                
                if (!(res.size()==1 && res.get(0).prob>0.9) && langList!=null) {
                    // on recherche le premier langage detecte qui est dans les langages candidats
                    for (int i = 0; i < res.size(); i++) {
                        for (int j = 0; j < langList.size(); j++) {
                            //if ( langList.get(j).equals( (res.get(i).lang) ) ) {
                            //String s1 = res.get(i).lang;
                            //String s2 = langList.get(j);
                            if ( res.get(i).lang.startsWith(langList.get(j)) ) {
                                if (minScore==0 || (res.get(i).prob >= minScore)) {
                                    score = res.get(i).prob;
                                    return res.get(i).lang;
                                }
                            }
                        }
                    }
                    //score = res.get(0).prob;
                    //return langList.get(0);
                }
                
                if (minScore==0 || (res.get(0).prob >= minScore)) {
                    score = res.get(0).prob;
                    return res.get(0).lang;
                } 
                else
                    return "xx";
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "xx";
        }
        return "xx";
    }
    
    public Double getScore() {
        return this.score;
    }
    
    public String getDebug() {
        return this.debug;
    }
    
    
}
