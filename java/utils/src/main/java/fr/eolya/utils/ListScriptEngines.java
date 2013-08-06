package fr.eolya.utils;

import java.util.List;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public class ListScriptEngines {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ScriptEngineManager manager = new ScriptEngineManager(); 
		List<ScriptEngineFactory> factories = manager.getEngineFactories(); 

		for (ScriptEngineFactory factory : factories) { 
			System.out.println("------------------------------------------------------------------");
			System.out.println("Name : " + factory.getEngineName()); 
			System.out.println("Version : " + factory.getEngineVersion()); 
			System.out.println("Language name : " + factory.getLanguageName()); 
			System.out.println("Language version : " + factory.getLanguageVersion()); 
			System.out.println("Extensions : " + factory.getExtensions()); 
			System.out.println("Mime types : " + factory.getMimeTypes()); 
			System.out.println("Names : " + factory.getNames()); 
		}
	}
}
