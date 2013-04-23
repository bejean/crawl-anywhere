package fr.eolya.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import fr.eolya.utils.http.HttpUtils;



public class ScriptSnippet {
    
    String engineName = "";
    String code = "";
    
    public ScriptSnippet(String engineName, String code) {
        this.engineName = engineName;
        this.code = code;
    }
    
    private static Document getXmlDocument(String fileName) {
        SAXReader reader = new SAXReader();
        reader.setValidation(false);
        try {
            return reader.read(new File(fileName));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }		
    }
    
    public static String getScriptFilename (String fileDir, String url) {
        
        try {
        	@SuppressWarnings("unused")
			URL u = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        
        File f = new File(fileDir);
        if (f.exists() && f.isDirectory()) {
            Iterator<File> iter = FileUtils.iterateFiles(f, new String[]{"xml"},true); 
            while(iter.hasNext()) {
                File file = (File) iter.next();
                Document document = getXmlDocument(file.getAbsolutePath());
                List<String> domains = Arrays.asList(document.getRootElement().attributeValue("domains").split(","));
                for (int i=0; i<domains.size(); i++)
                    if (HttpUtils.urlBelongSameHost(domains.get(i), url)) return file.getAbsolutePath();
            }
        }
        return null;
    }
    
    public static ScriptSnippet getInstance (String fileOrDir, String url, String action) {
        
        File f = new File(fileOrDir);
        if (!f.exists()) return null;
        
        String fileName = "";
        if (f.isDirectory()) {
            fileName = getScriptFilename (fileOrDir, url);
        }
        else {
            fileName = fileOrDir;
        }
        
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        String path = u.getPath();
        if ("".equals(path)) path = "/";
        
        Document document = getXmlDocument(fileName);
        
        @SuppressWarnings("unchecked")
		List<Element> eUrlList = document.getRootElement().elements();
        for (int i=0; i<eUrlList.size(); i++) {
            
            Element eUrl = eUrlList.get(i);
            String match = eUrl.attributeValue("match");
            
            String r = match;
            Pattern p = Pattern.compile(r);
            Matcher m = p.matcher(path);
            if (m.find()) {
                // url match
                
                @SuppressWarnings("unchecked")
				List<Element> eScriptList = eUrl.elements();
                for (int j=0; j<eScriptList.size(); j++) {
                    Element eScript = eScriptList.get(j);
                    String scriptAction = eScript.attributeValue("action");
                    if (scriptAction!=null && scriptAction.equals(action)) {
                        // action match
                        
                        String engineName = eScript.attributeValue("engine");
                        String code = evaluateCode(eScript.getText());
                        return new ScriptSnippet(engineName, code);
                    }
                }	
            }
        }	
        return null;
    }
    
    public static String evaluateCode(String code) {
        
        try
        {
            Pattern p = Pattern.compile("\\[include\\s+'(.*)'\\s*\\]");
            Matcher m = p.matcher(code);
            if (!m.find()) return code;

            m.reset();
            while(m.find()) {
                String group = m.group().substring(0, m.group().length());
                String fileName = m.group(1);
                String replacement = FileUtils.readFileToString(new File(fileName));
                code = code.replace(group, replacement);
            }
            return code;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }	    
    }
    
    public String getEngineName () {
        return this.engineName;		
    }
    
    public String getCode () {
        return this.code;		
    }
}