package com.dropbox.client2.session;

import java.io.Serializable;

/**
 * <p>
 * Holds a user's access token and secret.
 * </p>
 */
public class AccessTokenPair extends TokenPair implements Serializable {

    // Do not change.
    private static final long serialVersionUID = -5526503075188547139L;

    public AccessTokenPair(String key, String secret) {
        super(key, secret);
    }
}
