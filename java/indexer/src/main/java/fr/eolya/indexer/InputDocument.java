package fr.eolya.indexer;

import java.util.ArrayList;
import java.util.Collection;

public class InputDocument {
    private Collection<InputField> fields;
    private String boost;
    
    public InputDocument() {
        fields = new ArrayList<InputField>();
        boost = null;
    }
    
    public void addField(String name, Object value) {
        addField(name, value, null);
    }
    
    public void addField(String name, Object value, String boost) {
        if (fields==null) return;
        InputField field = new InputField(name, value, boost);
        fields.add(field);
    }
    public Collection<InputField> getFields() {
        return fields;
    }
    
    public void setDocumentBoost(String boost) {
        this.boost = boost;
    }
    
    public String getDocumentBoost() {
        return boost;
    }
}
