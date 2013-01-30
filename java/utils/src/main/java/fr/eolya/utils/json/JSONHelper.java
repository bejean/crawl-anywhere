package fr.eolya.utils.json;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONHelper {
	public static String getJSONFieldText(String json, String field) throws JsonParseException, JsonMappingException, IOException {
		JsonFactory factory = new JsonFactory(); 
		ObjectMapper mapper = new ObjectMapper(factory);
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {}; 
		HashMap<String,Object> o = mapper.readValue(json, typeRef); 
		return (String) o.get(field);
	}
}
