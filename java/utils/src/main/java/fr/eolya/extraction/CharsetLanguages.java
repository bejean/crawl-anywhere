package fr.eolya.extraction;

import java.io.*;
import java.util.*;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.*;

/**
 * Charset to language mapping
 */
public class CharsetLanguages {
    
    HashMap<String, ArrayList<String>> charsetLanguagesMapping=null;
    HashMap<String, ArrayList<String>> charsetAliasesMapping=null;
    HashMap<String, ArrayList<String>> unicodeBlockLanguagesMapping=null;
    
    /**
     * @throws IOException - if mapping ressource files are not accessible
     */
    public CharsetLanguages () throws IOException {
        charsetLanguagesMapping = loadCharsetLanguagesMapping();
        charsetAliasesMapping = loadCharsetAliasesMapping();
        unicodeBlockLanguagesMapping= loadUnicodeBlockLanguagesMapping();
    }
    
    private HashMap<String, ArrayList<String>> loadCharsetLanguagesMapping() throws IOException
    {
        HashMap<String, ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
        
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/eolya/extraction/CharsetLanguagesMapping.txt");
        if (stream==null) {
            stream = CharsetLanguages.class.getResourceAsStream("/META-INF/CharsetLanguagesMapping.txt");
        }	
        
        InputStreamReader streamReader = new InputStreamReader(stream); 
        BufferedReader reader = new BufferedReader(streamReader) ; 
        
        String line; 
        while ((line=reader.readLine())!= null)   
        {  
            line=line.trim();
            if (!"".equals(line) && !line.startsWith("#"))
            {
                line=line.replaceAll("\t+", "\t"); 
                String[] aItems = line.split("\t");
                ArrayList<String> aLang = new ArrayList<String>();
                
                String[] aLanguages = aItems[1].split(",");
                for (int i=0; i<aLanguages.length; i++)
                {
                    String language=aLanguages[i].trim();
                    language = language.substring(language.indexOf("(")+1, language.indexOf(")"));
                    aLang.add(language.toLowerCase());
                }
                map.put(aItems[0].toLowerCase(), aLang);
            }
        }  
        return map;
    }
    
    private HashMap<String, ArrayList<String>> loadCharsetAliasesMapping() throws IOException
    {
        HashMap<String, ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
        
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/eolya/extraction/CharsetAliasesMapping.txt");
        if (stream==null) {
            stream = CharsetLanguages.class.getResourceAsStream("/META-INF/CharsetAliasesMapping.txt");
        }	
        
        InputStreamReader streamReader = new InputStreamReader(stream); 
        BufferedReader reader = new BufferedReader(streamReader) ; 
        
        String line; 
        while ((line=reader.readLine())!= null)   
        {  
            line=line.trim();
            if (!"".equals(line) && !line.startsWith("#"))
            {
                line=line.replaceAll("\t+", "\t"); 
                String[] aItems = line.split("\t");
                ArrayList<String> charSet = new ArrayList<String>();
                
                String[] aCharSets = aItems[1].split(",");
                for (int i=0; i<aCharSets.length; i++)
                    charSet.add(aCharSets[i].trim().toLowerCase());
                
                map.put(aItems[0].toLowerCase(), charSet);
            }
        }  
        return map;
    }
    
    private HashMap<String, ArrayList<String>> loadUnicodeBlockLanguagesMapping() throws IOException
    {
        HashMap<String, ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
        
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/eolya/extraction/UnicodeBlockLanguagesMapping.txt");
        if (stream==null) {
            stream = CharsetLanguages.class.getResourceAsStream("/META-INF/UnicodeBlockLanguagesMapping.txt");
        }	
        
        InputStreamReader streamReader = new InputStreamReader(stream); 
        BufferedReader reader = new BufferedReader(streamReader) ; 
        
        String line; 
        while ((line=reader.readLine())!= null)   
        {  
            line=line.trim();
            if (!"".equals(line) && !line.startsWith("#"))
            {
                //ARABIC                          : Arabic (ar), Persian (fa)
                String[] aItems = line.split(":");
                
                if (aItems.length==2)
                {
                    // block name
                    String name = aItems[0].trim();
                    
                    // language codes
                    ArrayList<String> languages = new ArrayList<String>();
                    
                    String[] aLanguages = aItems[1].split(",");
                    for (int i=0; i<aLanguages.length; i++)
                    {
                        String language=aLanguages[i].trim();
                        language = language.substring(language.indexOf("(")+1, language.indexOf(")"));
                        languages.add(language.toLowerCase());
                    }
                    map.put(name, languages);				
                }
            }
        }  
        return map;
    }
    
    /**
     * Get the aliases for the specified charset
     * 
     * @param charsetName
     * 		Charset name (canonical)
     * @return Aliases or null if charset name is unknown
     */
    public ArrayList<String> getCharsetAliases(String charsetName)
    {
        return charsetAliasesMapping.get(charsetName.toLowerCase());	
    }
    
    /**
     * Get possible languages for the specified charset
     * 
     * @param charsetName
     * 		Charset name (canonical or alias)
     * @return Languages or null if charset name is unknown
     */
    public ArrayList<String> getCharsetLanguages(String charsetName)
    {
        return charsetLanguagesMapping.get(getCharsetCanonicalName(charsetName));	
    }
    
    /**
     * Get canonical name for the specified charset
     * 
     * @param charsetName
     * 		Charset name (canonical or alias)
     * @return Canonical name or null if charset name is unknown
     */
    public String getCharsetCanonicalName (String charsetName)
    {
        Iterator<String> iterator  = charsetAliasesMapping.keySet().iterator();
        while(iterator.hasNext())
        {
            String key = iterator.next();
            if (key.equalsIgnoreCase(charsetName))
                return key.toLowerCase(); 
            
            ArrayList<String> aCharSet = charsetAliasesMapping.get(key); 
            for (int i=0; i<aCharSet.size(); i++)
                if (aCharSet.get(i).equalsIgnoreCase(charsetName))
                    return key.toLowerCase(); 
        }
        return null;
    }
    
    /**
     * Get the java.nio canonical (prefered) charset name
     * See : 
     *     http://www.iana.org/assignments/character-sets
     *     http://java.sun.com/javase/6/docs/api/java/nio/charset/Charset.html
     * 
     * @param charsetName
     * 		Charset name (canonical or alias)
     * @return Canonical charset name or null if charsetName is unsupported or illegal
     */
    static public String getCharsetCanonicalNameNio (String charsetName)
    {
        try {
            Charset charset = Charset.forName (charsetName);
            return charset.name();
        }
        catch (Exception e) // IllegalCharsetNameException, UnsupportedCharsetException
        {
            return null;
        }
    }
    
    /**
     * Get unicode block used in the text
     * 
     * @param text
     * 		text to analyse
     * @return String with all unicode blocks detected in the text
     */
    public String getUnicodeBlockDistribution(String text)
    {
        HashMap<Character.UnicodeBlock, Integer> map = new HashMap<Character.UnicodeBlock, Integer>();
        for (int i=0; i<text.length(); i++)
        {
            UnicodeBlock ub = Character.UnicodeBlock.of(text.charAt(i));
            if (ub!=null)
            {
                if (map.containsKey(ub))
                    map.put(ub, map.get(ub)+1);
                else
                    map.put(ub, 1);
            }
        }
        
        LinkedHashMap<Character.UnicodeBlock, Integer> sortedMap = new LinkedHashMap<Character.UnicodeBlock, Integer>();
        List<Character.UnicodeBlock> mapKeys = new ArrayList<Character.UnicodeBlock>(map.keySet());
        List<Integer> mapValues = new ArrayList<Integer>(map.values());
        TreeSet sortedSet = new TreeSet(mapValues);
        Object[] sortedArray = sortedSet.toArray();
        for (int i=sortedArray.length-1; i>=0; i--) {
            sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), (Integer)sortedArray[i]);
        }
        
        String output = "";
        Iterator<Character.UnicodeBlock> iterator  = sortedMap.keySet().iterator();
        while(iterator.hasNext())
        {
            Character.UnicodeBlock key = iterator.next();
            String k = key.toString();
            String v = String.valueOf(sortedMap.get(key));
            if (!"".equals(output)) output += "|";
            output += k + ":" + v;
        }
        return output;
    }
    
    /**
     * Get possible languages for the specified unicode blocks
     * 
     * @param unicodeBlockDistribution
     * 		String with all unicode block detected in the text
     * @return Languages iso 639-1 codes (http://fr.wikipedia.org/wiki/Liste_des_codes_ISO_639-1) or null if charset name is unknown
     */
    public ArrayList<String> getUnicodeBlockLanguages(String unicodeBlockDistribution)
    {
        String[] aBlocks = unicodeBlockDistribution.split("\\|");		
        
        // Trouver la freq la plus élevée à part celle de BASIC_LATIN
        int maxFreq = 0;
        for (int i=0; i<aBlocks.length-1 && maxFreq==0; i++)
        {
            String[] aItems = aBlocks[i].trim().split(":");
            if (!"BASIC_LATIN".equals(aItems[0].trim()) && !"GENERAL_PUNCTUATION".equals(aItems[0].trim()))
                //if (maxFreq<Integer.parseInt(aItems[1].trim()))
                if (maxFreq==0)
                    maxFreq=Integer.parseInt(aItems[1].trim());		
        }
        
        // Reconstruire unicodeBlockDistribution en ne conservant que les distributions à 70% de la plus elevée (hors BASIC_LATIN)
        double coeff = 0.60; 
        String newDistribution = "";
        for (int i=0; i<aBlocks.length-1 ; i++)
        {
            String[] aItems = aBlocks[i].trim().split(":");
            //if ("BASIC_LATIN".equals(aItems[0].trim()) || "LATIN_1_SUPPLEMENT".equals(aItems[0].trim()))
            //{
            //	if (!"".equals(newDistribution)) newDistribution+="|";
            //	newDistribution+=aItems[0].trim()+":"+aItems[1].trim();
            //}
            //else
            //{
            if (!"GENERAL_PUNCTUATION".equals(aItems[0].trim()))
            {
                if (Integer.parseInt(aItems[1].trim()) > Math.round(maxFreq*coeff) )
                {
                    if (!"".equals(newDistribution)) newDistribution+="|";
                    newDistribution+=aItems[0].trim()+":"+aItems[1].trim();
                }
            }
            //}

        }		
        if (!"".equals(newDistribution)) aBlocks = newDistribution.split("\\|");
        
        
        // Si seulement BASIC_LATIN 
        if (aBlocks.length==1)
        {
            String[] aItems = aBlocks[0].trim().split(":");
            if ("BASIC_LATIN".equals(aItems[0].trim()) && aBlocks.length==1)
                return unicodeBlockLanguagesMapping.get("BASIC_LATIN");
        }
        
        // Si seulement BASIC_LATIN et LATIN_1_SUPPLEMENT
        if (aBlocks.length==2)
        {
            String[] aItems0 = aBlocks[0].trim().split(":");
            String[] aItems1 = aBlocks[1].trim().split(":");
            if (aItems0[0].indexOf("LATIN")!=-1 && aItems1[0].indexOf("LATIN")!=-1)
                return unicodeBlockLanguagesMapping.get("LATIN_1_SUPPLEMENT");
        }
        
        ArrayList<String> languages = new ArrayList<String>();
        for (int i=0; i<aBlocks.length; i++) {
            String[] aItems = aBlocks[i].trim().split(":");
            ArrayList<String> l = unicodeBlockLanguagesMapping.get(aItems[0]);
            if (l!=null) {
                for (int j=0; j<l.size(); j++) {
                    languages.add(l.get(j));
                }
            }
        }
        return languages;
        
        //		// 1er block <> BASIC_LATIN et LATIN_1_SUPPLEMENT
        //		String[] aItems = aBlocks[0].trim().split(":");
        //		if (aItems[0].indexOf("LATIN")==-1)
        //			return unicodeBlockLanguagesMapping.get(aItems[0].trim());
        //
        //		return null;
        
        /*
		ArrayList<String> ret = new ArrayList<String>();
		for (int i=0; i<aBlocks.length ;i++)
		{
			String[] aItems = aBlocks[i].trim().split(":");
			ArrayList<String> src = unicodeBlockLanguageMapping.get(aItems[0].trim());
			while (!src.isEmpty()) {
				ret.add(src.remove(0));
			}
		}		
		if (ret.size()==0)
			return null;

		return ret;
         */
    }
    
    public static void main(String[] args) {
        try {
            CharsetLanguages csu = new CharsetLanguages();
            
            ArrayList<String> aliases = csu.getCharsetAliases("ISO-8859-1");
            ArrayList<String> languages = csu.getCharsetLanguages("ISO-8859-1");
            String name = csu.getCharsetCanonicalName ("Greek8");
            
            String s = csu.getUnicodeBlockDistribution ("Cet épisode se caractérise aussi par la violence et une ambiance angoissante");
            languages = csu.getUnicodeBlockLanguages(s);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



