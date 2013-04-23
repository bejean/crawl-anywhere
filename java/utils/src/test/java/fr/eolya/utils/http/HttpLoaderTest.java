package fr.eolya.utils.http;

import junit.framework.TestCase;
import org.junit.Test;

import fr.eolya.utils.http.HttpLoader;

public class HttpLoaderTest extends TestCase { 

	@Test
	public void testHttpLoader(){
		try {
			HttpLoader loader = new HttpLoader();
			assertEquals(HttpLoader.LOAD_SUCCESS, loader.open("http://www.google.fr/"));
			
			assertEquals(200, loader.getResponseStatusCode());
			assertEquals("text/html; charset=ISO-8859-1", loader.getContentType());
			assertEquals("ISO-8859-1", loader.getCharSet());

			loader = new HttpLoader();
			assertEquals(HttpLoader.LOAD_SUCCESS, loader.open("https://www.google.fr/"));
			
			assertEquals(200, loader.getResponseStatusCode());
			assertEquals("text/html; charset=ISO-8859-1", loader.getContentType());
			assertEquals("ISO-8859-1", loader.getCharSet());

			loader = new HttpLoader();
			assertEquals(HttpLoader.LOAD_ERROR, loader.open("https://www.eolya.fr/"));

			loader = new HttpLoader();
			assertEquals(HttpLoader.LOAD_ERROR, loader.open("http://www.googlegooglegoogle.fr/"));

			loader = new HttpLoader();
			assertEquals(HttpLoader.LOAD_ERROR, loader.open("http://www.google.fr/zzzzz/"));
			assertEquals(404, loader.getResponseStatusCode());	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


//http://www.amisw.com/fr/1.html