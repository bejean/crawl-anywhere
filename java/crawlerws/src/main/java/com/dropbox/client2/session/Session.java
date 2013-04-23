/*
 * Copyright (c) 2009-2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.dropbox.client2.session;

import java.util.Locale;

import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import com.dropbox.client2.DropboxAPI;

/**
 * Keeps track of a logged in user. Contains configuration options for the
 * {@link DropboxAPI}.
 */
public interface Session {

    /**
     * Returns the app key and secret.
     */
    public AppKeyPair getAppKeyPair();

    /**
     * Returns the currently logged in user's access token and secret.
     */
    public AccessTokenPair getAccessTokenPair();

    /**
     * Returns what Dropbox access type to use (currently app folder or entire
     * Dropbox).
     */
    public AccessType getAccessType();

    /**
     * Returns the locale to use. Must not return null. Currently, this is used
     * for user-facing messages that are returned by the API. You should not
     * cache this value, but instead get it every time from the system in case
     * the locale changes.
     */
    public Locale getLocale();

    /**
     * Returns whether or not this session has a user's access token and
     * secret.
     */
    public boolean isLinked();

    /**
     * Unlinks the session by removing any stored access token and secret.
     */
    public void unlink();

    /**
     * OAuth signs the request with the currently-set tokens and secrets.
     *
     * @param request an {@link HttpRequest}.
     */
    public void sign(HttpRequest request);

    /**
     * Will be called every time a request is made to Dropbox, in case the
     * proxy changes between requests. Return null if you do not want to use
     * a proxy, or a {@link ProxyInfo} object with a host and optionally a
     * port set.
     */
    public ProxyInfo getProxyInfo();

    /**
     * Will be called every time a request is made to Dropbox, in case you want
     * to use a new client every time. However, it's highly recommended to
     * create a client once and reuse it to take advantage of connection reuse.
     */
    public HttpClient getHttpClient();

    /**
     * Will be called every time right before a request is sent to Dropbox. It
     * should set the socket and connection timeouts on the request if you want
     * to override the default values. This is abstracted out to cope with
     * signature changes in the Apache HttpClient libraries.
     */
    public void setRequestTimeout(HttpUriRequest request);

    /**
     * Returns the Dropbox API server. Changing this will break things.
     */
    public String getAPIServer();

    /**
     * Returns the Dropbox content server. Changing this will break things.
     */
    public String getContentServer();

    /**
     * Returns the Dropbox web server. Changing this will break things.
     */
    public String getWebServer();

    public enum AccessType {
        DROPBOX("dropbox"), APP_FOLDER("sandbox");

        private final String urlPart;

        private AccessType(String urlPart) {
            this.urlPart = urlPart;
        }

        @Override
        public String toString() {
            return urlPart;
        }
    }

    /**
     * Describes a proxy.
     */
    public static final class ProxyInfo {
        /** The address of the proxy. */
        public final String host;

        /** The port of the proxy, or -1 to use the default port. */
        public final int port;

        /**
         * Creates a proxy info.
         *
         * @param host the host to use without a protocol (required).
         * @param port the port to use, or -1 for default port.
         */
        public ProxyInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * Creates a proxy info using the default port.
         *
         * @param host the host to use without a protocol (required).
         */
        public ProxyInfo(String host) {
            this(host, -1);
        }
    }
}
