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
import java.util.HashMap;
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
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
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
	
	static private String repeatChar(char c, int times) {
		char[] array = new char[times]; 
		Arrays.fill(array, c);
		return String.valueOf(array);
	}
	
	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}

	
	@Test
	public void sylTokenizerTest() throws IOException
	{
		System.out.println("Testing TibSyllableTokenizer()");
		String input = "བཀྲ་ཤིས། བདེ་ལེགས།";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བཀྲ", "ཤིས", "བདེ" ,"ལེགས");

		System.out.print(input + " => ");
		TokenStream res = tokenize(reader, new TibSyllableTokenizer());
		assertTokenStream(res, expected);
	}
	
	@Test
	public void affixedFilterTest() throws IOException
	{
		System.out.println("Testing TibAffixedFilter()");
		String input = "དག། གའམ། གའིའོ། དགའ། དགའི། དགའོ། དགའིས། དགའང་། དགའམ། དགའིའོ།";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("དག", "ག", "ག", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ", "དགའ");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(reader, new TibSyllableTokenizer());
		TokenFilter res = new TibAffixedFilter(syllables);
		assertTokenStream(res, expected);
	}

	@Test
	public void stopwordFilterTest() throws IOException
	{
		System.out.println("Testing TibetanAnalyzer.tibStopWords");
		String input = "ཧ་ཏུ་གི་ཀྱི་གིས་ཀྱིས་ཡིས་ཀྱང་སྟེ་ཏེ་མམ་རམ་སམ་ཏམ་ནོ་བོ་ཏོ་གིན་ཀྱིན་གྱིན་ཅིང་ཅིག་ཅེས་ཞེས་ཧ།";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ཧ", "ཧ");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(reader, new TibSyllableTokenizer());
		StopFilter res = new StopFilter(syllables, TibetanAnalyzer.tibStopSet);
		assertTokenStream(res, expected);
	}
	
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');
	}

	@Test
	public void wordTokenizerLemmatizeTest() throws IOException
	{
		System.out.println("Testing TibWordTokenizer() with lemmatization");
		String input = "༆ བཀྲ་ཤིས་བདེ་ལེགས་ཕུན་སུམ་ཚོགས། རྟག་ཏུ་བདེ་བ་ཐོབ་པར་ཤོག ནམ་མཁའི་མཐས་ཐུག་པར་ཤོག";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བཀྲ་ཤིས", "བདེ་ལེགས", "ཕུན", "སུམ", "ཚོགས", "རྟག", "དུ", "བདེ་བ", "ཐོབ་པ", "ཤོག", "ནམ་མཁའ", "མཐའ", "ཐུག་པ", "ཤོག");
		System.out.print(input + " => ");
		TibWordTokenizer tibWordTokenizer = new TibWordTokenizer("src/test/resources/dict-file.txt");
		TokenStream syllables = tokenize(reader, tibWordTokenizer);
		assertTokenStream(syllables, expected);
	}

	@Test
	public void wordTokenizerNoLemmatizeTest() throws IOException
	{
		System.out.println("Testing TibWordTokenizer() without lemmatization");
		String input = "༆ བཀྲ་ཤིས་བདེ་ལེགས་ཕུན་སུམ་ཚོགས། རྟག་ཏུ་བདེ་བ་ཐོབ་པར་ཤོག ནམ་མཁའི་མཐས་ཐུག་པར་ཤོག";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བཀྲ་ཤིས", "བདེ་ལེགས", "ཕུན", "སུམ", "ཚོགས", "རྟག", "ཏུ", "བདེ་བ", "ཐོབ་པར", "ཤོག", "ནམ་མཁའི", "མཐས", "ཐུག་པར", "ཤོག");
		System.out.print(input + " => ");
		TibWordTokenizer tibWordTokenizer = new TibWordTokenizer("src/test/resources/dict-file.txt");
		tibWordTokenizer.setLemmatize(false); // we don't want to lemmatize
		TokenStream syllables = tokenize(reader, tibWordTokenizer);
		assertTokenStream(syllables, expected);
	}

	
	@Test
	public void mappingCharFilterTest() throws IOException
	{
		System.out.println("Testing TibCharFilter()");
		String input = "\u0F00་ཆོ༷ས་ཀྱི་རྒྱ༵་མཚོ།";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("\u0F68\u0F7C\u0F7E", "ཆོས", "ཀྱི", "རྒྱ", "མཚོ");
		System.out.print(input + " => ");
		TokenStream res = tokenize(new TibCharFilter(reader), new TibSyllableTokenizer());
		assertTokenStream(res, expected);
	}

	@Test
	public void ewtsFilterTest() throws IOException
	{
		System.out.println("Testing TibEwtsFilter()");
		String input = "bod rgyal lo invalid བོད";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བོད", "རྒྱལ", "ལོ",  "ཨིནབ",  "ལིད",  "བོད");
		System.out.print(input + " => ");
		TokenStream res = tokenize(new TibEwtsFilter(reader), new TibSyllableTokenizer());
		assertTokenStream(res, expected);
		// long string, provoked a bug
		input = "de'i sprul sku yi ngogs chos rje dge 'dun mkhas grub ni mkhan chen dge 'dun rgya mtsho'i gyi sku tshar chu khyi (1742) lor 'khrungs/ rje de nyid las bcu gsum pa la rab tu byung/ dgon chen du mdo sngags la sbyangs/ rig gnas thams cad la mkhas/ nyer gcig pa chu sprel la smon lam rab 'byams pa mdzad/ kun mkhyen bar mas mgo 'dren mdzad de lcang skya rin po chen nas chos rje'i cho lo gnang/ mkhan chen gshegs par dngul srang stong dang nyis brgyas mchod rten bzhengs/ lcags byi lor rgyud khrir bzhugs/ bde mchog yi dam mdzad/ gsung rtsom yang 'ga' zhig snang/ bdun cu pa lcags byi  (mdo smad chos 'byung du bdun cu pa lcags byi lor gshegs pa zer ba lo grangs dang lo snying thod mi thug pa dpyad gzhi ru sor bzhag byas pa) lor gshegs/ de'i sprul sku dge 'dun yon tan rgya mtsho chos srid kyi mkhyen rgya che zhing rgyud pa'i khri mdzad/ de'i sprul sku yi ngogs nas 'khrungs pa dkon mchog rgyal mtshan da lta bzhugs";
		reader = new StringReader(input);
		res = tokenize(new TibEwtsFilter(reader), new TibSyllableTokenizer());
		while (res.incrementToken()) {} // with trigger the exception in case of a bug
	}

	@Test
	public void bugEatenSyllable() throws IOException
	{
		System.out.println("Bug testing in TibWordTokenizer()");
		String input = "༆ བཀྲ་ཤིས་བདེ་ལེགས་ཕུན་སུམ་ཚོགས། རྟག་ཏུ་བདེ་བ་ཐོབ་པར་ཤོག";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བཀྲ", "ཤིས", "བདེ", "ལེགས", "ཕུན", "སུམ", "ཚོགས", "རྟག", "ཏུ", "བདེ", "བ", "ཐོབ", "པར", "ཤོག");
		System.out.print(input + " => ");
		TibWordTokenizer tibWordTokenizer = new TibWordTokenizer("src/test/resources/eaten-syl-dict.txt");
		TokenStream syllables = tokenize(reader, tibWordTokenizer);
		assertTokenStream(syllables, expected);
	}
	
	@Test
	public void ioBufferLimitTest() throws IOException
	{
		System.out.println("Testing max size of ioBuffer");
		List<String> expected = Arrays.asList("བཀྲ་ཤིས་བདེ", "ལེགས");
		TibWordTokenizer tibWordTokenizer = new TibWordTokenizer("src/test/resources/io-buffer-size-test.txt");
		
		HashMap<Integer, Integer> ranges = new HashMap<Integer, Integer>();
		ranges.put(2030, 2049);
		ranges.put(4080, 4097);

		for (HashMap.Entry<Integer, Integer> entry : ranges.entrySet()) {
			for (int i=entry.getKey() ; i<entry.getValue(); i++) {
				System.out.println(i);
				String input = repeatChar('་', i)+"བཀྲ་ཤིས་བདེ་ལེགས།";
				Reader reader = new StringReader(input);
				System.out.print(input + " => ");
				TokenStream syllables = tokenize(reader, tibWordTokenizer);
				assertTokenStream(syllables, expected);
			}			
		}
	}
	
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}