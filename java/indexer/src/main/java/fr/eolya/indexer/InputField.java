package fr.eolya.indexer;

public class InputField {
    
    public String name;
    public Object value;
    public String boost;
    
    public InputField(String name, Object value) {
        this.name = name;
        this.value = value;
        this.boost = null;
    }
    
    public InputField(String name, Object value, String boost) {
        this.name = name;
        this.value = value;
        this.boost = boost;
    }
}
