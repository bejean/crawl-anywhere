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
	
	public static HashMap<String,Object> getJSONMap(String json) throws JsonParseException, JsonMappingException, IOException {
		JsonFactory factory = new JsonFactory(); 
		ObjectMapper mapper = new ObjectMapper(factory);
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {}; 
		return mapper.readValue(json, typeRef); 
	}
	
	public static HashMap<String,String> getJSONMapString(String json) throws JsonParseException, JsonMappingException, IOException {
		HashMap<String,Object> mapIn = getJSONMap(json);
		HashMap<String,String> mapOut = new HashMap<String,String>();
		for (String key : mapIn.keySet()) {
			if ((mapIn.get(key) instanceof String)) {
				mapOut.put(key, (String)mapIn.get(key));
			} else {
				mapOut.put(key, String.valueOf(mapIn.get(key)));
			}
		}
		return mapOut;
	}

	public static long getValueAsId (String value) {
		if (value.indexOf(",")!=-1) value = value.substring(0, value.indexOf(","));
		if (value.indexOf(".")!=-1) value = value.substring(0, value.indexOf("."));
		return Long.parseLong(value);
	}	
	
	//	public static HashMap<String,String> getJSONMapString(String json) throws JsonParseException, JsonMappingException, IOException {
	//		JsonFactory factory = new JsonFactory(); 
	//		ObjectMapper mapper = new ObjectMapper(factory);
	//		TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {}; 
	//		return mapper.readValue(json, typeRef); 
	//	}
}
