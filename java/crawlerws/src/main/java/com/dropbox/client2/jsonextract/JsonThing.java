package com.dropbox.client2.jsonextract;

/**
 * (Internal class for extracting JSON.)
 *
 * <p>
 * A utility class to let you extract your required structure out of an
 * org.json.simple object.
 * </p>
 *
 * <p>
 * As you descend into the object and pull our your data, these classes keep
 * track of where you are, so if there's an error in the JSON value, you'll get
 * a "path" string describing exactly where the problem is.
 * </p>
 */
public final class JsonThing extends JsonBase<Object> {

    public JsonThing(Object internal, String path) {
        super(internal, path);
    }

    public JsonThing(Object internal) {
        super(internal, null);
    }

    private static final java.util.HashMap<Class,String> TypeNames = new java.util.HashMap<Class,String>();
    static {
        TypeNames.put(String.class, "a string");
        TypeNames.put(Number.class, "a number");
        TypeNames.put(Boolean.class, "a boolean");
        TypeNames.put(java.util.Map.class, "an object");
        TypeNames.put(java.util.List.class, "an array");
    }

    private static String typeNameForClass(Class<?> c) {
        if (c == null) return "null";
        String name = TypeNames.get(c);
        assert name != null;
        return name;
    }

    private static String typeNameForObject(Object o) {
        if (o == null) return "null";
        if (o instanceof Number) return "a number";
        if (o instanceof String) return "a string";
        if (o instanceof Boolean) return "a boolean";
        if (o instanceof java.util.Map) return "an object";
        if (o instanceof java.util.List) return "an array";
        throw new IllegalArgumentException("not a valid org.json.simple type: " + o.getClass().getName());
    }

    private boolean is(Class<?> type) {
        assert type != null;
        return type.isInstance(internal);
    }

    private <T> T expect(Class<T> type) throws JsonExtractionException {
        assert type != null;

        if (type.isInstance(internal)) {
            @SuppressWarnings("unchecked")
            T recast = (T) internal;
            return recast;
        }

        throw error("expecting " + typeNameForClass(type) + ", found " + typeNameForObject(internal));
    }

    public void expectNull() throws JsonExtractionException {
        if (internal != null) {
            throw error("expecting null");
        }
    }

    public boolean isNull() {
        return internal == null;
    }

    public JsonMap expectMap() throws JsonExtractionException {
        @SuppressWarnings("unchecked")
        java.util.Map<String,Object> mapInternal = (java.util.Map<String,Object>) expect(java.util.Map.class);
        return new JsonMap(mapInternal, path);
    }

    public boolean isMap() {
        return is(java.util.Map.class);
    }

    public JsonList expectList() throws JsonExtractionException {
        @SuppressWarnings("unchecked")
        java.util.List<Object> listInternal = (java.util.List<Object>) expect(java.util.List.class);
        return new JsonList(listInternal, path);
    }

    public boolean isList() {
        return is(java.util.List.class);
    }

    public Number expectNumber() throws JsonExtractionException {
        return expect(Number.class);
    }

    public boolean isNumber() {
        return is(Number.class);
    }

    public long expectInt64() throws JsonExtractionException {
        if (internal instanceof Number) {
            Number number = (Number) internal;
            // TODO: Be robust, since JSON actually defines "number" to mean "IEEE double"
            // - Make sure there's no fractional part.
            // - Make sure there's no overflow.
            return number.longValue();
        }
        else if (internal instanceof String) {
            String string = (String) internal;
            try {
                return Long.parseLong(string, 16);
            }
            catch (NumberFormatException ex) {
                throw error("couldn't parse string as hex (expecting a 64-bit signed integer value)");
            }
        }
        else {
            throw error("expecting an integer (or a hex string), found " + typeNameForObject(internal));
        }
    }

    public boolean isInt64() {
        try {
            expectInt64();
            return true;
        }
        catch (JsonExtractionException ex) {
            return false;
        }
    }

    public String expectString() throws JsonExtractionException {
        return expect(java.lang.String.class);
    }

    public String expectStringOrNull() throws JsonExtractionException {
        if (internal == null) return null;
        return expect(java.lang.String.class);
    }

    public boolean isString() {
        return is(java.lang.String.class);
    }

    public boolean expectBoolean() throws JsonExtractionException {
        return expect(java.lang.Boolean.class);
    }

    public boolean isBoolean() {
        return is(java.lang.Boolean.class);
    }

    static String pathConcat(String a, String b) {
        if (a == null) return b;
        return a + "/" + b;
    }

    public JsonExtractionException unexpected() {
        return error("unexpected type: " + typeNameForObject(internal));
    }

    public static final class OptionalExtractor<T> extends JsonExtractor<T> {
        public final JsonExtractor<T> elementExtractor;
        public OptionalExtractor(JsonExtractor<T> elementExtractor) {
            this.elementExtractor = elementExtractor;
        }

        public T extract(JsonThing jt) throws JsonExtractionException {
            return jt.optionalExtract(this.elementExtractor);
        }
    }

    public <T> T optionalExtract(JsonExtractor<T> extractor) throws JsonExtractionException {
        if (isNull()) return null;
        return extractor.extract(this);
    }
}
