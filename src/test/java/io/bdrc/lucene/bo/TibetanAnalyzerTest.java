package io.bdrc.lucene.bo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
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
		
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
			}
			System.out.println(String.join(" ", termList));
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
		List<String> expected = Arrays.asList("བཀྲ", "ཤིས", "བདེ" ,"ལེགས");

		System.out.print(input + " => ");
		TokenStream res = tokenize(input, new TibSyllableTokenizer());
		assertTokenStream(res, expected);
	}

	@Test
	public void test2() throws IOException
	{
		System.out.println("Test2: TibAffixedFilter()");
		String input = "དག། དགའ། དགའི། དགའོ། དགའིས། དགའང་། དགའམ།";
		List<String> expected = Arrays.asList("དག", "དགའ", "དག", "དག", "དག", "དག", "དག");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(input, new TibSyllableTokenizer());
		TokenFilter res = new TibAffixedFilter(syllables);
		assertTokenStream(res, expected);
	}

	@Test
	public void test3() throws IOException
	{
		System.out.println("Test3: filter tibStopWords");
		String input = "གི་ཀྱི་གྱི་ཡི་གིས་ཀྱིས་ཧ་གྱིས་ཡིས་ན་སུ་ར་རུ་དུ་ལ་ཏུ་གོ་ངོ་དོ་ཧ་ནོ་པོ་མོ་རོ་ལོ་སོ་ཏོ་དང་།";
		List<String> expected = Arrays.asList("ཧ", "ཧ");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(input, new TibSyllableTokenizer());
		StopFilter res = new StopFilter(syllables, TibetanAnalyzer.tibStopSet);
		assertTokenStream(res, expected);
	}

	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
