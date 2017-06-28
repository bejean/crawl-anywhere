package fr.eolya.utils.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
 * This class is used to add cookie support to {@link HttpLoader} without changing its API.
 *
 * The idea behind it is to work around {@link HttpLoader} storing cookie info in a single String value (persisting the value of the cookie, but not
 * domain and path). As a workaround, the three attributes of the cookie (Value, Domain and Path) are serialized into a single String and later parsed
 * into a Cookie object.
 */
class CookieHelper {
    private static final String UTF_8 = StandardCharsets.UTF_8.name();
    private static final char SEPARATOR_CHAR = '$';
    private static final String SERIALIZED_COOKIE_MARKER = "$SERIALIZED_COOKIE$";

    static boolean isSerializedCookie(String value) {
        return value.startsWith(SERIALIZED_COOKIE_MARKER);
    }

    static Cookie deserialize(String name, String value) {
        String serializedCookie = StringUtils.substringAfter(value, SERIALIZED_COOKIE_MARKER);
        String[] tokens = StringUtils.split(serializedCookie, SEPARATOR_CHAR);

        try {
            BasicClientCookie cookie = new BasicClientCookie(name, URLDecoder.decode(tokens[0], UTF_8));
            cookie.setDomain(URLDecoder.decode(tokens[1], UTF_8));
            cookie.setPath(URLDecoder.decode(tokens[2], UTF_8));
            return cookie;
        } catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible);
        }
    }

    static String serialize(Cookie cookie) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(SERIALIZED_COOKIE_MARKER);
            sb.append(URLEncoder.encode(cookie.getValue(), UTF_8)).append(SEPARATOR_CHAR);
            sb.append(URLEncoder.encode(cookie.getDomain(), UTF_8)).append(SEPARATOR_CHAR);
            sb.append(URLEncoder.encode(cookie.getPath(), UTF_8)).append(SEPARATOR_CHAR);
            return sb.toString();

        } catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible);
        }
    }
}