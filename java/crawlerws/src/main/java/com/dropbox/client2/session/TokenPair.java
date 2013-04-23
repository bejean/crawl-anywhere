package com.dropbox.client2.session;

import java.io.Serializable;

/**
 * <p>
 * Just two strings -- a "key" and a "secret". Used by OAuth in several
 * places (consumer key/secret, request token/secret, access token/secret).
 * Use specific subclasses instead of using this class directly.
 * </p>
 */
public abstract class TokenPair implements Serializable
{

    /**
     * The "key" portion of the pair.  For example, the "consumer key",
     * "request token", or "access token".  Will never contain the "|"
     * character.
     */
    public final String key;

    /**
     * The "secret" portion of the pair.  For example, the "consumer secret",
     * "request token secret", or "access token secret".
     */
    public final String secret;

    /**
     * @param key assigned to {@link #key}.
     * @param secret assigned to {@link #secret}.
     *
     * @throws IllegalArgumentException if key or secret is null or invalid.
     */
    public TokenPair(String key, String secret) {
        if (key == null)
            throw new IllegalArgumentException("'key' must be non-null");
        if (key.contains("|"))
            throw new IllegalArgumentException("'key' must not contain a \"|\" character: \"" + key + "\"");
        if (secret == null)
            throw new IllegalArgumentException("'secret' must be non-null");

        this.key = key;
        this.secret = secret;
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ (secret.hashCode() << 1);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TokenPair && equals((TokenPair) o);
    }

    public boolean equals(TokenPair o) {
        return key.equals(o.key) && secret.equals(o.secret);
    }

    @Override
    public String toString() {
        return "{key=\"" + key + "\", secret=\"" + secret + "\"}";
    }
}
