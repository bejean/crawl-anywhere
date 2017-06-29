package fr.eolya.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

public class CookieHelperTest {
    private static final String COOKIE_NAME = "cookieName";
    private static final String COOKIE_VALUE = "fancy$value%22xx";
    private static final String COOKIE_PATH = "/bla$action";
    private static final String COOKIE_DOMAIN = "www.domain.com";

    @Test
    public void testSerialization() throws Exception {
        SetCookie cookie = new BasicClientCookie(COOKIE_NAME, COOKIE_VALUE);
        cookie.setPath(COOKIE_PATH);
        cookie.setDomain(COOKIE_DOMAIN);

        String serializedCookie = CookieHelper.serialize(cookie);

        assertTrue(CookieHelper.isSerializedCookie(serializedCookie));
        assertEquals("$SERIALIZED_COOKIE$fancy%24value%2522xx$www.domain.com$%2Fbla%24action$", serializedCookie);
    }

    @Test
    public void testDeserialization() throws Exception {
        String serializedCookie = "$SERIALIZED_COOKIE$fancy%24value%2522xx$www.domain.com$%2Fbla%24action$";

        Cookie cookie = CookieHelper.deserialize(COOKIE_NAME, serializedCookie);
        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals(COOKIE_VALUE, cookie.getValue());
        assertEquals(COOKIE_PATH, cookie.getPath());
        assertEquals(COOKIE_DOMAIN, cookie.getDomain());
    }
}