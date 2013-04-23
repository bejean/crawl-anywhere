package com.dropbox.client2.jsonextract;

public abstract class JsonExtractor<T> {
    public abstract T extract(JsonThing jt) throws JsonExtractionException;
}
