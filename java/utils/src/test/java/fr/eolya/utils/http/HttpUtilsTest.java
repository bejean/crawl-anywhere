package fr.eolya.utils.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import fr.eolya.utils.http.HttpLoader;

public class HttpUtilsTest extends TestCase { 

	@Test
	public void testUrlBelongSameHost(){
			
		String urlReferer = "www.test.com";
		String urlAliases = "www.test.fr,*.test.fr, www.test.*";
		List<String> aliases = new ArrayList<String>(Arrays.asList(urlAliases.split(",")));
		
		assertTrue(HttpUtils.urlBelongSameHost(urlReferer, "www.test.fr", aliases));
		assertTrue(HttpUtils.urlBelongSameHost(urlReferer, "www.test.net", aliases));
		assertTrue(HttpUtils.urlBelongSameHost(urlReferer, "www2.test.fr", aliases));
		
		assertFalse(HttpUtils.urlBelongSameHost(urlReferer, "www.test2.fr", aliases));
	}
}