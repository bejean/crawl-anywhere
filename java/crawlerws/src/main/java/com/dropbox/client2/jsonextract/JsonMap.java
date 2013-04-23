package com.dropbox.client2.jsonextract;

import java.util.Iterator;
import java.util.Map;

/**
 * (Internal class for extracting JSON.)
 *
 * A JSON "object" (a mapping of string keys to arbitrary JSON values).
 */
public final class JsonMap extends JsonBase<Map<String,Object>> implements Iterable<Map.Entry<String,JsonThing>> {

    public JsonMap(Map<String, Object> internal, String path) {
        super(internal, path);
    }

    public JsonMap(Map<String, Object> internal) {
        super(internal);
    }

    private static boolean isIdentLike(String s) {
        if (s.length() == 0) return false;
        if (!isEnglishLetter(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isEnglishLetter(c) && !isEnglishDigit(c)) return false;
        }
        return true;
    }

    private static boolean isEnglishLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isEnglishDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String pathConcatField(String path, String fieldName) {
        String suffix = fieldName;
        if (!isIdentLike(fieldName)) {
            suffix = '"' + fieldName + '"'; // TODO: Proper JSON quoting.
        }
        return JsonThing.pathConcat(path, suffix);
    }

    public JsonThing get(String fieldName) throws JsonExtractionException {
        if (!internal.containsKey(fieldName)) {
            throw error("expecting object to have field \"" + fieldName + "\", but it does not");
        }
        return new JsonThing(internal.get(fieldName), pathConcatField(path, fieldName));
    }

    public JsonThing getMaybe(String fieldName) {
        if (!internal.containsKey(fieldName)) {
            return null;
        }
        return new JsonThing(internal.get(fieldName), pathConcatField(path, fieldName));
    }

    /**
     * A key+value iterator that automatically wraps every value in a JsonThing.
     */
    private static final class WrapperIterator implements Iterator<Map.Entry<String,JsonThing>> {
        private final String path;
        private final Iterator<Map.Entry<String,Object>> internal;

        private WrapperIterator(String path, Iterator<Map.Entry<String,Object>> internal) {
            this.path = path;
            this.internal = internal;
        }

        public boolean hasNext() { return internal.hasNext(); }
        public Map.Entry<String,JsonThing> next() {
            return new WrappedEntry(path, internal.next());
        }
        public void remove() { throw new UnsupportedOperationException("can't remove"); }
    }

    private static final class WrappedEntry implements Map.Entry<String,JsonThing> {
        private final String key;
        private final JsonThing value;

        private WrappedEntry(String path, Map.Entry<String,Object> original) {
            this.key = original.getKey();
            this.value = new JsonThing(original.getValue(), pathConcatField(path, key));
        }

        public String getKey() { return key; }
        public JsonThing getValue() { return value; }
        public JsonThing setValue(JsonThing jsonThing) { throw new UnsupportedOperationException(); }
    }

    public Iterator<Map.Entry<String,JsonThing>> iterator() {
        return new WrapperIterator(path, internal.entrySet().iterator());
    }
}
