package io.bdrc.lucene.bo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class TibetanAnalyzerTest
{
	static TokenStream tokenize(String input, TibetanTokenizer tokenizer) throws IOException {
	      tokenizer.close();
	      tokenizer.end();
	      Reader reader = new StringReader(input);
	      tokenizer.setReader(reader);
	      tokenizer.reset();
	      return tokenizer;
	}
	
	@BeforeClass
	public static void init() {
	    System.out.println("before the test sequence");
	}
	
	@Test
    public void test1()
    {
		System.out.println("test 1");
		assertTrue(false);
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
