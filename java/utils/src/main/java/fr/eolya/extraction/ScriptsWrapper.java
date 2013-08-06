package fr.eolya.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;


import fr.eolya.extraction.tika.TikaWrapper;
import fr.eolya.utils.ScriptSnippet;
import fr.eolya.utils.http.HttpLoader;
import fr.eolya.utils.http.HttpUtils;
import fr.eolya.utils.http.WebStream;
import gnu.getopt.Getopt;

public class ScriptsWrapper {
    
    static public String getScriptName(String scriptsPath, String url) {
        if (scriptsPath==null || "".equals(scriptsPath)) return "";
        if (url==null || "".equals(url)) return "";
        
        try {
            @SuppressWarnings("unused")
			URL parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return "";
        }
        
        String scriptName = ScriptSnippet.getScriptFilename (scriptsPath, url);
        if (scriptName==null) return "";
        return scriptName;
    }
    
    static public ScriptSnippet getScriptSnippet(String scriptName, String action, String url) {
        ScriptSnippet scriptSnippet = null;
        if (scriptName !=null && !"".equals(scriptName)) scriptSnippet = ScriptSnippet.getInstance (scriptName, url, action);
        return scriptSnippet;
    }
    

    
    static public String[] htmlLinks(String url, String rawPage, String[] inLinks, String scriptsPath, String scriptName) {
        if (scriptName==null || "".equals(scriptName))
            scriptName = getScriptName(scriptsPath, url);
        
        ScriptSnippet scriptSnippet = getScriptSnippet(scriptName, "links", url);
        if (scriptSnippet==null) return inLinks;
        
        String engineName = scriptSnippet.getEngineName();
        String code = scriptSnippet.getCode();
        
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName(engineName);
        
//        ScriptsWrapperHelper helper = new ScriptsWrapperHelper(engine);
//        
//        engine.put("ScriptsWrapperHelper", helper);
//        try {
//            engine.eval("function load(filename) { ScriptsWrapperHelper.load(filename); }");
//        } catch (ScriptException e1) {
//            e1.printStackTrace();
//        }

        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.clear();
        bindings.put("page",rawPage); 
        bindings.put("links",inLinks); 
        try {
            engine.eval(code, bindings);
        } catch (ScriptException e) {
            System.out.println(e.getMessage());
            return inLinks;
        } 
        
        try {
            String links[] = (String []) bindings.get("links"); 
            //				if (aLinks.length>0) {
            //					ArrayList<String> l = new ArrayList<String>();
            //					for (int i=0; i<aLinks.length; i++) {		
            //						if (!l.contains(aLinks[i]))
            //							l.add(aLinks[i]);
            //					}
            //					
            //					links.clear();
            //					if (l!=null && l.size()>0) {
            //						for (int i=0; i<l.size(); i++) {	
            //							links.add(l.get(i));
            //						}
            //					}	
            //					return links;
            //				}		
            return links;
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return inLinks;
    }
    
    static public HashMap<String, String> htmlParse(String url, String rawPage, String contentType, String scriptsPath, String scriptName) {
        
        if (scriptName==null || "".equals(scriptName))
            scriptName = getScriptName(scriptsPath, url);
        
        ScriptSnippet scriptSnippet = getScriptSnippet(scriptName, "parse", url);
        if (scriptSnippet!=null) {
            
            String engineName = scriptSnippet.getEngineName();
            String code = scriptSnippet.getCode();
            
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName(engineName);
            
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.clear();
            bindings.put("page",rawPage); 
            bindings.put("url",url); 
            bindings.put("contenttype",contentType); 
            try {
                engine.eval(code, bindings);
            } catch (ScriptException e) {
                System.out.println(e.getMessage());
                return null;
            } 
            
            try {
                String parsedData[] = (String []) bindings.get("parsedData");
                HashMap<String, String> m = new HashMap<String, String>();
                m.put("title", org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(parsedData[0]));
                m.put("date", parsedData[1]);
                m.put("page", parsedData[2]);
                return m;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } 
        }	
        return null;
    }
    
    static public HashMap<String, String> cleanText(String url, String text, String scriptsPath, String scriptName) {
        
        if (scriptName==null || "".equals(scriptName))
            scriptName = getScriptName(scriptsPath, url);
        
        ScriptSnippet scriptSnippet = getScriptSnippet(scriptName, "cleantext", url);
        if (scriptSnippet!=null) {
            
            String engineName = scriptSnippet.getEngineName();
            String code = scriptSnippet.getCode();
            
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName(engineName);
            
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.clear();
            bindings.put("text",text); 
            try {
                engine.eval(code, bindings);
            } catch (ScriptException e) {
                System.out.println(e.getMessage());
                return null;
            } 
            
            try {
                String cleanedText[] = (String []) bindings.get("cleanedText");
                HashMap<String, String> m = new HashMap<String, String>();
                m.put("text", cleanedText[0]);
                return m;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } 
        }	
        return null;
    }
    
    static public HashMap<String, String> extractMeta(String url, String rawPage, String contentType, String contentCharset, String scriptsPath, String scriptName) {
        
        if (scriptName==null || "".equals(scriptName))
            scriptName = getScriptName(scriptsPath, url);
        
        ScriptSnippet scriptSnippet = getScriptSnippet(scriptName, "extractmeta", url);
        if (scriptSnippet!=null) {
            
            String engineName = scriptSnippet.getEngineName();
            String code = scriptSnippet.getCode();
            
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName(engineName);
            
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.clear();
            bindings.put("page",rawPage); 
            bindings.put("url",url); 
            bindings.put("contenttype",contentType); 
            bindings.put("contentcharset",contentCharset); 
            try {
                engine.eval(code, bindings);
            } catch (ScriptException e) {
                System.out.println(e.getMessage());
                return null;
            } 
            
            try {
                String meta[] = (String []) bindings.get("meta");
                HashMap<String, String> m = new HashMap<String, String>();
                
                for (String am: meta) {
                    if (am!=null) {
                        am = am.trim();
                        if (!"".equals(am)) {
                            String[] aItems = am.split(":");
                            if (aItems.length==2) {
                                m.put(aItems[0].trim().toLowerCase(), aItems[1].trim().toLowerCase());
                            }
                        }
                    }
                }
                
                return m;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } 
        }   
        return null;
    }
    
    private static void usage() {
        System.out.println("Usage : java -a <parse|links|meta> -s <scripts directory> -u <url> [-v]");
        System.out.println("    -a : action");
        System.out.println("    -s : scripts directory");
        System.out.println("    -u : url");
        System.out.println("    -v : verbose");
    }
    
    public static void main(String[] args) {		
        
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }
        
        Getopt g = new Getopt("Indexer", args, "a:s:u:v");
        g.setOpterr(false);
        int c;
        
        boolean verbose = false;
        String action = "";
        String scriptsPath = "";
        String url = "";
        
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'a':
                    action = g.getOptarg();
                    break;
                    
                case 'u':
                    url = g.getOptarg();
                    break;
                    
                case 's':
                    scriptsPath = g.getOptarg();
                    break;
                    
                case 'v':
                    verbose = true;
                    break;
            }
        }
        
        if ("".equals(action)) {
            System.out.println("Error: action parameter missing ");
            System.exit(-1);
        }
        
        if ("".equals(url) || "".equals(scriptsPath)) {
            System.out.println("Error: missing parameters");
            System.exit(-1);
        }
        
        // Get page
        try {
            String rawPage = null;
            String charSet = null;
 //           String declaredLanguage = null;
            String contentType = null;
            String contentEncoding = null;
            
            HttpLoader urlLoader = new HttpLoader();
            
            int ret = -1;
 //           int tryCount = 0;
 //           String temp = url;
            
            ret = urlLoader.open(url);
            
//            while (ret == -1 && tryCount < 3) {
//                try {
//                    urlLoader.close();
//                    ret = urlLoader.open("", "", true);
//                }
//                catch (IOException e) {
//                    String msg = e.getMessage();
//                    if (tryCount == 0 && msg!=null && msg.toLowerCase().startsWith("invalid uri")) {
//                        System.out.println(msg);
//                        temp = HttpUtils.urlEncode(temp);
//                        urlLoader.setUrl(temp);
//                    }
//                    else {
//                        Utils.sleep(tryCount * 1000);								
//                    }
//                    tryCount++;
//                    ret = -1;
//                    urlLoader.close();
//                    if (tryCount == 3) throw new IOException(e.getMessage());
//                }
//            }
            
            if (ret == HttpLoader.LOAD_SUCCESS) {
                contentType = urlLoader.getContentType();
                contentEncoding = urlLoader.getContentEncoding();
                WebStream ws = new WebStream(urlLoader.getStream(), "", contentType, contentEncoding);
                rawPage = ws.getString();
                charSet = ws.getCharSet();
                String declaredLanguage = ws.getDeclaredLanguage();
                ws.clear();
            }		
            
            if ("links".equals(action)) {
                printVerbose(url, scriptsPath, action, verbose);
                List<String> links = null;
                if (HttpLoader.isRss(contentType, null)) {
                    links = HttpUtils.extractLinksFromFeed(rawPage);
                }
                else {
                    links = HttpUtils.extractAbsoluteLinks(rawPage, url, 2);
                    String [] aLinks = htmlLinks(url, rawPage, links.toArray(new String[]{}), scriptsPath, null); 
                    links = Arrays.asList(aLinks);	
                }
                
                for (String strLink : links) {
                    try {
                        //strLink = strLink.trim();
                        //strLink = URLUtils.urlGetAbsoluteURL(url, strLink);
                        strLink = HttpUtils.urlNormalize(strLink.trim(), null);	
                        System.out.println(strLink);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if ("parse".equals(action)) {
                printVerbose(url, scriptsPath, action, verbose);
                String title = "";
                String d = "";
                String page = "";
                
                HashMap<String, String> m = htmlParse(url, rawPage, contentType, scriptsPath, null); 
                if (m!=null && m.size()>0) {
                    title = m.get("title");
                    d = m.get("date");
                    page = m.get("page");
                }
                
                // Get page text
                //MultiFormatTextExtractor extractor = new MultiFormatTextExtractor();
				TikaWrapper tikaWrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_TEXT, TikaWrapper.CONTENT_TYPE_HTML);

                String text = "";
                InputStream in = null;
                if (page==null || "".equals(page)) {
                    //text = extractor.htmlPageToText(rawPage, "", "");
                	in = IOUtils.toInputStream(rawPage);
                } else {
                    //text = extractor.htmlPageToText(page, "", "");
                	in = IOUtils.toInputStream(page);
                }
                
				tikaWrapper.process(in);
				text = tikaWrapper.getText();
				
                if (title==null || "".equals(title))
                    title = tikaWrapper.getMetaTitle();
                
                System.out.println("Title = "+ title);
                System.out.println("Date  = " + d);
                System.out.println("Text  = " + text);
                System.out.println("Page  = " + page);
            }
            
            if ("meta".equals(action)) {
                printVerbose(url, scriptsPath, action, verbose);
                HashMap<String, String> m = extractMeta(url, rawPage, contentType, charSet, scriptsPath, null);
                if (m!=null && m.size()>0) {
                    for (Map.Entry<String, String> entry : m.entrySet()) {
                        System.out.println("meta_extracted_" + entry.getKey() + " = " + entry.getValue());
                    }
                }
            }
            urlLoader.close();
            urlLoader = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void printVerbose(String url, String scriptsPath, String action, boolean verbose) {  
        if (verbose) {
            System.out.println("------------------------------------------------------------");
            System.out.println("Verbose mode");
            System.out.println("------------------------------------------------------------");
            System.out.println("Url            = " + url);
            System.out.println("Scripts Path   = " + scriptsPath);
            System.out.println("Action         = " + action);
            System.out.println("Script name    = " + getScriptName(scriptsPath, url));
            
            ScriptSnippet snippet = getScriptSnippet(getScriptName(scriptsPath, url), action, url);
            if (snippet!=null)
                System.out.println("Script snippet = found");
            else
                System.out.println("Script snippet = none");
            System.out.println("------------------------------------------------------------");
        }   
    }
}
