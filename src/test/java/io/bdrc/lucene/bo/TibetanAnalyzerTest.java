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
		
	static private void assertTokenStream(TokenStream tokenizer, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
			while (tokenizer.incrementToken()) {
			    termList.add(charTermAttribute.toString());
			}
			/** prints out the list of terms */
			System.out.println(" => " + String.join(" ", termList));
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
		System.out.println("Test1: TibSyllableTokenizer()");
		String input = "བཀྲ་ཤིས། བདེ་ལེགས།";
		System.out.print(input);
		List<String> expected = Arrays.asList("བཀྲ", "ཤིས", "བདེ" ,"ལེགས");
		TokenStream tokenizr = tokenize(input, new TibSyllableTokenizer());
		assertTokenStream(tokenizr, expected);
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
