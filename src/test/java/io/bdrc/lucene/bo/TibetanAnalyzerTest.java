package io.bdrc.lucene.bo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit test for simple App.
 */
public class TibetanAnalyzerTest
{
	static TokenStream tokenize(String input, Tokenizer tokenizer) throws IOException {
	      tokenizer.close();
	      tokenizer.end();
	      Reader reader = new StringReader(input);
	      tokenizer.setReader(reader);
	      tokenizer.reset();
	      return tokenizer;
	}
	
	private void assertTokenStream(String string, List<String> expected) {
		Tokenizer tokenizr = new WhitespaceTokenizer();
		TokenStream tokenStream = null;
		try {
			tokenStream = tokenize(string, tokenizr);
			
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
			    termList.add(charTermAttribute.toString());
			}
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
	@BeforeClass
	public static void init() {
	    System.out.println("before the test sequence");
	}
	
	@Test
    public void test1() throws IOException
    {
		assertTokenStream("Hello World", Arrays.asList("Hello", "World"));
    }

	@Test
    public void test2()
    {
		System.out.println("test 2");
		assertFalse(false);
    }
	
	@AfterClass
	public static void finish() {
	    System.out.println("after the test sequence");
	}
	
}
