/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
 * Unit tests for the Tibetan tokenizers and filters.
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
		String input = "དག། གའམ། གའིའོ། དགའ། དགའི། དགའོ། དགའིས། དགའང་། དགའམ། དགའིའོ།";
		List<String> expected = Arrays.asList("དག", "ག", "ག", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(input, new TibSyllableTokenizer());
		TokenFilter res = new TibAffixedFilter(syllables);
		assertTokenStream(res, expected);
	}

	@Test
	public void test3() throws IOException
	{
		System.out.println("Test3: filter TibetanAnalyzer.tibStopWords");
		String input = "ཧ་ཏུ་གི་ཀྱི་གིས་ཀྱིས་ཡིས་ཀྱང་སྟེ་ཏེ་མམ་རམ་སམ་ཏམ་ནོ་བོ་ཏོ་གིན་ཀྱིན་གྱིན་ཅིང་ཅིག་ཅེས་ཞེས་ཧ།";
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
