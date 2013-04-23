package com.dropbox.client2.jsonextract;

import static org.json.simple.JSONValue.toJSONString;

import java.util.List;
import java.util.Map;

public final class JsonExtractionException extends Exception {

    public JsonExtractionException(String path, String message, Object value) {
        super((path == null ? "" : path + ": ") + message +
              (value == null ? "" : ": " + summarizeValue(value)));
    }

    private static String summarizeValue(Object value) {
        if (value instanceof java.util.Map) {
            StringBuilder buf = new StringBuilder();
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) value;
            buf.append("{");
            String sep = "";
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                buf.append(sep); sep = ", ";
                buf.append(toJSONString(entry.getKey()));
                buf.append(" = ");
                buf.append("...");
            }
            buf.append("}");
            return buf.toString();
        }
        else if (value instanceof java.util.List) {
            List<?> list = (List) value;
            if (list.isEmpty()) {
                return "[]";
            } else if (list.size() == 1) {
                return "[" + summarizeValue(list.get(0)) + "]";
            } else {
                return "[" + summarizeValue(list.get(0)) + ", ...] (" + list.size() + " elements)";
            }
        }
        else {
            return toJSONString(value);
        }
    }
}
