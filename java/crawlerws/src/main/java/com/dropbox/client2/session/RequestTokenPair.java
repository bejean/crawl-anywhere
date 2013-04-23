package com.dropbox.client2.session;

import java.io.Serializable;

/**
 * <p>
 * Holds a request token and secret for the web OAuth flow.
 * </p>
 */
public final class RequestTokenPair extends AccessTokenPair implements Serializable {

    // Do not change.
    private static final long serialVersionUID = 7933124160414910085L;

    public RequestTokenPair(String key, String secret) {
        super(key, secret);
    }
}
