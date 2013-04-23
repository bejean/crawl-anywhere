package fr.eolya.extraction;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;


public class ScriptsWrapperHelper {
    
    ScriptEngine engine = null;
    
    public ScriptsWrapperHelper (ScriptEngine engine)  {
        this.engine = engine;
    }
    
    public void load(String filename) throws ScriptException {
        try {
            engine.eval(new FileReader(filename));
        }
        catch(FileNotFoundException e) {
            throw new RuntimeException("Error loading javascript file: " + filename, e);
        }
    }
}
