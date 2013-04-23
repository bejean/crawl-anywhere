package fr.eolya.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InputDocument {
    private Collection<InputField> fields;
    private String boost;
    private String type;
    private String language;
    private String configuredLanguages;
    
    public InputDocument(String type, String configuredLanguages) {
        fields = new ArrayList<InputField>();
        boost = null;
        this.type = type;
        this.configuredLanguages = configuredLanguages;
    }
    
    public void addField(String name, Object value) {
        addField(name, value, null);
        if ("language".equals(name.toLowerCase())) language = ((String)value).toLowerCase();
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
    
    public String getDocumentType() {
        if (type!=null) return type;
        List<String> items = Arrays.asList(configuredLanguages.split("\\s*,\\s*"));
        if (language!=null && items.contains(language)) return "mapping_" + language;
        return null;
    }
}
