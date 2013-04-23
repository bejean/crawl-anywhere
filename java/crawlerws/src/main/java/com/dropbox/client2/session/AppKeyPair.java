package com.dropbox.client2.session;

/**
 * <p>
 * Holds your app's key and secret.
 * </p>
 */
public final class AppKeyPair extends TokenPair {

    public AppKeyPair(String key, String secret) {
        super(key, secret);
    }
}
