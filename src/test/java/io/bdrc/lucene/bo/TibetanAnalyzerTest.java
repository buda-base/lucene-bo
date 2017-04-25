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
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.stempel.StempelStemmer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Diff;
import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

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
	
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');
	}

	/**
	 *  this function is inspired from getLastOnPath() in stemmer's Trie.java
	 * @param toAnalyze the string to analyse
	 * @param startCharIndex the index from which we want to analyze
	 * @param t the Trie containing the data
	 */
	// 
	public void produceOneToken(String toAnalyze, int startCharIndex, Trie t) {
		// getting the root of the tree
		System.out.println(toAnalyze);
		Row now = t.getRow(t.getRoot());
		int w; // temporary index variable
		int lastCharIndex = -1; // the index of the last match in the string we analyze
		int lastCmdIndex = -1; // the index (inside the Trie) of the cmd corresponding to the last match
		
		int i = startCharIndex; // the current index in the string
		while (i < toAnalyze.length()) {
			Character ch = toAnalyze.charAt(i); // get the current character
			System.out.println("moving to index "+i+": "+ch);
			w = now.getCmd(ch); // get the command associated with the current character at next step in the Trie
			if (w >= 0) {
				if (i >= toAnalyze.length()-1 || !isTibLetter(toAnalyze.charAt(i+1))) {
						System.out.println("current row has an command for it, so it's a match");
						lastCmdIndex = w;
						lastCharIndex = i;
					}
            } else {
//            	System.out.println("current row does not have a command for it, no match");
            }
			w = now.getRef(ch); // get the next row if there is one
			if (w >= 0) {
//				System.out.println("current row does have a reference for this char, further matches are possible, moving one row forward in the Trie");
                now = t.getRow(w);
            } else {
//            	System.out.println("current row does not have a reference to this char, so there's no further possible match, breaking the loop");
                break; // no more steps possible in our research
            }
			i++;
		}
		//w = now.getCmd(toAnalyze.charAt(i));
		if (lastCharIndex == -1) {
			System.out.println("I have found nothing");
			return;
		}
		System.out.println("I have found a token that goes from "+startCharIndex+" to "
				+ lastCharIndex);
		System.out.println("the substring is: "+toAnalyze.substring(startCharIndex, lastCharIndex+1));
		System.out.println("the command associated with this token in the Trie is: "+t.getCommandVal(lastCmdIndex));
	}
	
	@Test
	public void test4() throws IOException
	{
		System.out.println("Test4: Testing Stemmer Trie");
		Trie test = new Trie(true);
		test.add("དྲོའི"," 2");
		test.add("བདེ་ལེགས"," ");
		test.add("བདེ", " 0");
		test.add("བཀྲ་ཤིས"," ");
		test.add("བཀྲ", " 0");
		test.add("དྲོ"," 0");
		test.add("དགའི", " 1");
		test.add("དགའ"," 0");
		Optimizer opt = new Optimizer();
		test.reduce(opt);
		produceOneToken("དག", 0, test);
		produceOneToken("དགའི", 0, test);
		produceOneToken("བཀྲ་", 0, test);
		produceOneToken("བཀྲད", 0, test);
		produceOneToken("བདེ་ལེགས", 0, test);
	}

	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
