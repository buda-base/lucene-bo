/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bdrc.lucene.bo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.PagedBytes.Reader;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

public final class TibWordTokenizer extends Tokenizer {
	private Trie scanner;

	// this tokenizer generates three attributes:
	// term offset, positionIncrement and type
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
		
	public TibWordTokenizer() throws FileNotFoundException, IOException {
		init();
	}
	
	
	private void init() throws FileNotFoundException, IOException {
		this.scanner = new Trie(true);
		
//		currently only adds the entries without any diff
		String file = "./resource/output/total_lexicon.txt";
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] parts = line.split(" ");
		        this.scanner.add(parts[0], "X");
		    }
			Optimizer opt = new Optimizer();
			this.scanner.reduce(opt);
		}
	}


	@Override
	public boolean incrementToken() throws IOException {
		Row row = scanner.getRow(scanner.getRoot());
		while (offsetAtt < this.setReader.length()) {
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
		
		OffsetAttribute i = offsetAtt;
		
		return false;
	}
	
	@Override
	public void reset() throws IOException {
		super.reset();
	}

}
