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


package com.dropbox.client2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.dropbox.client2.DropboxAPI.RequestAndResponse;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxSSLException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.Session.ProxyInfo;

/**
 * This class is mostly used internally by {@link DropboxAPI} for creating and
 * executing REST requests to the Dropbox API, and parsing responses. You
 * probably won't have a use for it other than {@link #parseDate(String)} for
 * parsing modified times returned in metadata, or (in very rare circumstances)
 * writing your own API calls.
 */
public class RESTUtility {

    private RESTUtility() {}

    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss ZZZZZ", Locale.US);

    public enum RequestMethod {
        GET, POST;
    }

    /**
     * Creates and sends a request to the Dropbox API, parses the response as
     * JSON, and returns the result.
     *
     * @param method GET or POST.
     * @param host the hostname to use. Should be either api server,
     *         content server, or web server.
     * @param path the URL path, starting with a '/'.
     * @param apiVersion the API version to use. This should almost always be
     *         set to {@code DropboxAPI.VERSION}.
     * @param params the URL params in an array, with the even numbered
     *         elements the parameter names and odd numbered elements the
     *         values, e.g. <code>new String[] {"path", "/Public", "locale",
     *         "en"}</code>.
     * @param session the {@link Session} to use for this request.
     *
     * @return a parsed JSON object, typically a Map or a JSONArray.
     *
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxUnlinkedException if the user has revoked access.
     * @throws DropboxParseException if a malformed or unknown response was
     *         received from the server.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    static public Object request(RequestMethod method, String host,
            String path, int apiVersion, String[] params, Session session)
            throws DropboxException {
        HttpResponse resp = streamRequest(method, host, path, apiVersion,
                params, session).response;
        return parseAsJSON(resp);
    }

    /**
    * Creates and sends a request to the Dropbox API, and returns a
    * {@link RequestAndResponse} containing the {@link HttpUriRequest} and
    * {@link HttpResponse}.
    *
    * @param method GET or POST.
    * @param host the hostname to use. Should be either api server,
    *         content server, or web server.
    * @param path the URL path, starting with a '/'.
    * @param apiVersion the API version to use. This should almost always be
    *         set to {@code DropboxAPI.VERSION}.
    * @param params the URL params in an array, with the even numbered
    *         elements the parameter names and odd numbered elements the
    *         values, e.g. <code>new String[] {"path", "/Public", "locale",
    *         "en"}</code>.
    * @param session the {@link Session} to use for this request.
    *
    * @return a parsed JSON object, typically a Map or a JSONArray.
    *
    * @throws DropboxServerException if the server responds with an error
    *         code. See the constants in {@link DropboxServerException} for
    *         the meaning of each error code.
    * @throws DropboxIOException if any network-related error occurs.
    * @throws DropboxUnlinkedException if the user has revoked access.
    * @throws DropboxException for any other unknown errors. This is also a
    *         superclass of all other Dropbox exceptions, so you may want to
    *         only catch this exception which signals that some kind of error
    *         occurred.
    */
    static public RequestAndResponse streamRequest(RequestMethod method,
            String host, String path, int apiVersion, String params[],
            Session session) throws DropboxException {
        HttpUriRequest req = null;
        String target = null;

        if (method == RequestMethod.GET) {
            target = buildURL(host, apiVersion, path, params);
            req = new HttpGet(target);
        } else {
            target = buildURL(host, apiVersion, path, null);
            HttpPost post = new HttpPost(target);

            if (params != null && params.length >= 2) {
                if (params.length % 2 != 0) {
                    throw new IllegalArgumentException("Params must have an even number of elements.");
                }
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();

                for (int i = 0; i < params.length; i += 2) {
                    if (params[i + 1] != null) {
                        nvps.add(new BasicNameValuePair(params[i], params[i + 1]));
                    }
                }

                try {
                    post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                } catch (UnsupportedEncodingException e) {
                    throw new DropboxException(e);
                }
            }

            req = post;
        }

        session.sign(req);
        HttpResponse resp = execute(session, req);

        return new RequestAndResponse(req, resp);
    }

    /**
     * Reads in content from an {@link HttpResponse} and parses it as JSON.
     *
     * @param response the {@link HttpResponse}.
     *
     * @return a parsed JSON object, typically a Map or a JSONArray.
     *
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs while
     *         reading in content from the {@link HttpResponse}.
     * @throws DropboxUnlinkedException if the user has revoked access.
     * @throws DropboxParseException if a malformed or unknown response was
     *         received from the server.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public static Object parseAsJSON(HttpResponse response)
            throws DropboxException {
        Object result = null;

        BufferedReader bin = null;
        try {
            HttpEntity ent = response.getEntity();
            if (ent != null) {
                InputStreamReader in = new InputStreamReader(ent.getContent());
                // Wrap this with a Buffer, so we can re-parse it if it's
                // not JSON
                // Has to be at least 16384, because this is defined as the buffer size in
                //     org.json.simple.parser.Yylex.java
                // and otherwise the reset() call won't work
                bin  = new BufferedReader(in, 16384);
                bin.mark(16384);

                JSONParser parser = new JSONParser();
                result = parser.parse(bin);
            }
        } catch (IOException e) {
            throw new DropboxIOException(e);
        } catch (ParseException e) {
            if (DropboxServerException.isValidWithNullBody(response)) {
                // We have something from Dropbox, but it's an error with no reason
                throw new DropboxServerException(response);
            } else {
                // This is from Dropbox, and we shouldn't be getting it
                throw new DropboxParseException(bin);
            }
        } catch (OutOfMemoryError e) {
            throw new DropboxException(e);
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                }
            }
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != DropboxServerException._200_OK) {
            if (statusCode == DropboxServerException._401_UNAUTHORIZED) {
                throw new DropboxUnlinkedException();
            } else {
                throw new DropboxServerException(response, result);
            }
        }

        return result;
    }

    /**
     * Reads in content from an {@link HttpResponse} and parses it as a query
     * string.
     *
     * @param response the {@link HttpResponse}.
     *
     * @return a map of parameter names to values from the query string.
     *
     * @throws DropboxIOException if any network-related error occurs while
     *         reading in content from the {@link HttpResponse}.
     * @throws DropboxParseException if a malformed or unknown response was
     *         received from the server.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public static Map<String, String> parseAsQueryString(HttpResponse response)
            throws DropboxException {
        HttpEntity entity = response.getEntity();

        if (entity == null) {
            throw new DropboxParseException("Bad response from Dropbox.");
        }

        Scanner scanner;
        try {
            scanner = new Scanner(entity.getContent()).useDelimiter("&");
        } catch (IOException e) {
            throw new DropboxIOException(e);
        }

        Map<String, String> result = new HashMap<String, String>();

        while (scanner.hasNext()) {
            String nameValue = scanner.next();
            String[] parts = nameValue.split("=");
            if (parts.length != 2) {
                throw new DropboxParseException("Bad query string from Dropbox.");
            }
            result.put(parts[0], parts[1]);
        }

        return result;
    }

    /**
     * Executes an {@link HttpUriRequest} with the given {@link Session} and
     * returns an {@link HttpResponse}.
     *
     * @param session the session to use.
     * @param req the request to execute.
     *
     * @return an {@link HttpResponse}.
     *
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxUnlinkedException if the user has revoked access.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public static HttpResponse execute(Session session, HttpUriRequest req)
            throws DropboxException {
        return execute(session, req, -1);
    }

    /**
     * Executes an {@link HttpUriRequest} with the given {@link Session} and
     * returns an {@link HttpResponse}.
     *
     * @param session the session to use.
     * @param req the request to execute.
     * @param socketTimeoutOverrideMs if >= 0, the socket timeout to set on
     *         this request. Does nothing if set to a negative number.
     *
     * @return an {@link HttpResponse}.
     *
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxUnlinkedException if the user has revoked access.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public static HttpResponse execute(Session session, HttpUriRequest req,
            int socketTimeoutOverrideMs) throws DropboxException {
        HttpClient client = updatedHttpClient(session);

        // Set request timeouts.
        session.setRequestTimeout(req);
        if (socketTimeoutOverrideMs >= 0) {
            HttpParams reqParams = req.getParams();
            HttpConnectionParams.setSoTimeout(reqParams, socketTimeoutOverrideMs);
        }

        try {
            HttpResponse response = null;
            for (int retries = 0; response == null && retries < 5; retries++) {
                /*
                 * The try/catch is a workaround for a bug in the HttpClient
                 * libraries. It should be returning null instead when an
                 * error occurs. Fixed in HttpClient 4.1, but we're stuck with
                 * this for now. See:
                 * http://code.google.com/p/android/issues/detail?id=5255
                 */
                try {
                    response = client.execute(req);
                } catch (NullPointerException e) {
                }

                /*
                 * We've potentially connected to a different network, but are
                 * still using the old proxy settings. Refresh proxy settings
                 * so that we can retry this request.
                 */
                if (response == null) {
                    updateClientProxy(client, session);
                }
            }

            if (response == null) {
                // This is from that bug, and retrying hasn't fixed it.
                throw new DropboxIOException("Apache HTTPClient encountered an error. No response, try again.");
            } else if (response.getStatusLine().getStatusCode() != DropboxServerException._200_OK) {
                // This will throw the right thing: either a DropboxServerException or a DropboxProxyException
                parseAsJSON(response);
            }

            return response;
        } catch (SSLException e) {
            throw new DropboxSSLException(e);
        } catch (IOException e) {
            // Quite common for network going up & down or the request being
            // cancelled, so don't worry about logging this
            throw new DropboxIOException(e);
        } catch (OutOfMemoryError e) {
            throw new DropboxException(e);
        }
    }

    /**
     * Creates a URL for a request to the Dropbox API.
     *
     * @param host the Dropbox host (i.e., api server, content server, or web
     *         server).
     * @param apiVersion the API version to use. You should almost always use
     *         {@code DropboxAPI.VERSION} for this.
     * @param target the target path, staring with a '/'.
     * @param params any URL params in an array, with the even numbered
     *         elements the parameter names and odd numbered elements the
     *         values, e.g. <code>new String[] {"path", "/Public", "locale",
     *         "en"}</code>.
     *
     * @return a full URL for making a request.
     */
    public static String buildURL(String host, int apiVersion,
            String target, String[] params) {
        if (!target.startsWith("/")) {
            target = "/" + target;
        }

        try {
            // We have to encode the whole line, then remove + and / encoding
            // to get a good OAuth URL.
            target = URLEncoder.encode("/" + apiVersion + target, "UTF-8");
            target = target.replace("%2F", "/");

            if (params != null && params.length > 0) {
                target += "?" + urlencode(params);
            }

            // These substitutions must be made to keep OAuth happy.
            target = target.replace("+", "%20").replace("*", "%2A");
        } catch (UnsupportedEncodingException uce) {
            return null;
        }

        return "https://" + host + ":443" + target;
    }

    /**
     * Parses a date/time returned by the Dropbox API. Returns null if it
     * cannot be parsed.
     *
     * @param date a date returned by the API.
     *
     * @return a {@link Date}.
     */
    public static Date parseDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Gets the session's client and updates its proxy.
     */
    private static synchronized HttpClient updatedHttpClient(Session session) {
        HttpClient client = session.getHttpClient();
        updateClientProxy(client, session);
        return client;
    }

    /**
     * Updates the given client's proxy from the session.
     */
    private static void updateClientProxy(HttpClient client, Session session) {
        ProxyInfo proxyInfo = session.getProxyInfo();
        if (proxyInfo != null && proxyInfo.host != null && !proxyInfo.host.equals("")) {
            HttpHost proxy;
            if (proxyInfo.port < 0) {
                proxy = new HttpHost(proxyInfo.host);
            } else {
                proxy = new HttpHost(proxyInfo.host, proxyInfo.port);
            }
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        } else {
            client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
        }
    }

    /**
     * URL encodes an array of parameters into a query string.
     */
    private static String urlencode(String[] params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Params must have an even number of elements.");
        }

        String result = "";
        try {
            boolean firstTime = true;
            for (int i = 0; i < params.length; i += 2) {
                if (params[i + 1] != null) {
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        result += "&";
                    }
                    result += URLEncoder.encode(params[i], "UTF-8") + "="
                    + URLEncoder.encode(params[i + 1], "UTF-8");
                }
            }
            result.replace("*", "%2A");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return result;
    }
}
