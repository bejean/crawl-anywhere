package fr.eolya.utils.http;

import junit.framework.TestCase;
import org.junit.Test;

import fr.eolya.utils.http.HttpLoader;

public class HttpLoaderTest extends TestCase { 

	@Test
	public void testHttpLoader(){
		try {
			HttpLoader loader = new HttpLoader("http://www.google.fr/");
			assertEquals(HttpLoader.LOAD_SUCCESS, loader.open());
			
			assertEquals(200, loader.getResponseStatusCode());
			assertEquals("text/html; charset=ISO-8859-1", loader.getContentType());
			assertEquals("ISO-8859-1", loader.getCharSet());

			loader = new HttpLoader("https://www.google.fr/");
			assertEquals(HttpLoader.LOAD_SUCCESS, loader.open());
			
			assertEquals(200, loader.getResponseStatusCode());
			assertEquals("text/html; charset=ISO-8859-1", loader.getContentType());
			assertEquals("ISO-8859-1", loader.getCharSet());

			loader = new HttpLoader("https://www.eolya.fr/");
			assertEquals(HttpLoader.LOAD_ERROR, loader.open());

			loader = new HttpLoader("http://www.googlegooglegoogle.fr/");
			assertEquals(HttpLoader.LOAD_ERROR, loader.open());

			loader = new HttpLoader("http://www.google.fr/zzzzz/");
			assertEquals(HttpLoader.LOAD_ERROR, loader.open());
			assertEquals(404, loader.getResponseStatusCode());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
