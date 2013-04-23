package com.dropbox.client2.jsonextract;

/**
 * (Internal class for extracting JSON.)
 */
abstract class JsonBase<T> {
    public final T internal;
    public final String path;

    public JsonBase(T internal) {
        this(internal, null);
    }

    public JsonBase(T internal, String path) {
        this.internal = internal;
        this.path = path;
    }

    public JsonExtractionException error(String message) {
        return new JsonExtractionException(path, message, internal);
    }
}
