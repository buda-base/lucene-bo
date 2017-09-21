# Lucene Analyzers for Tibetan

This repository contains Lucene tools (analysers, tokenizers and filters) for the Tibetan Language. They are based on [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

Installation through maven:

```xml
    <dependency>
      <groupId>io.bdrc.lucene</groupId>
      <artifactId>lucene-bo</artifactId>
      <version>1.1.1</version>
    </dependency>
```

If the jar is needed for use in a non-maven based install, it may be found at

```
    https://repo1.maven.org/maven2/io/bdrc/lucene/lucene-bo/1.1.1/lucene-bo-1.1.1.jar
```

## Components

#### TibetanAnalyzer

The main Analyzer. 
It tokenizes the input text using *TibSyllableTokenizer*, then applies *TibAffixedFilter* and *StopFilter* with a predefined list of stop words.

There are two constructors. The nullary constructor and

```    
    TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts)

    segmentInWords - if the segmentation is on words instead of syllables
    lemmatize - if the analyzer should remove affixed particles, and normalize words in words mode
    filterChars - if the text should be converted to NFD (necessary for texts containing NFC strings)
    fromEwts - if the text should be converted from EWTS to Unicode
```

The nullary constructor is equivalent to `TibetanAnalyzer(true, true, true, false)`
#### TibWordTokenizer

This tokenizer produces words through a Maximal Matching algorithm. It builds on top of [this Trie implementation](https://github.com/BuddhistDigitalResourceCenter/stemmer).  

Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
For example, if both དོན and དོན་གྲུབ exist in the Trie, དོན་གྲུབ will be returned every time the sequence དོན + གྲུབ is found.<br>
The sentence སེམས་ཅན་གྱི་དོན་གྲུབ་པར་ཤོག will be tokenized into "སེམས་ཅན + གྱི + དོན་གྲུབ + པར + ཤོག" (སེམས་ཅན + གྱི + དོན + གྲུབ་པར + ཤོག expected)

#### TibSyllableTokenizer

This tokenizer produces syllable tokens (with no tshek) from the input Tibetan text.

#### TibAffixedFilter

This filter removes non-ambiguous affixed particles (འི, འོ, འིའོ, འམ, འང and འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག).

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).