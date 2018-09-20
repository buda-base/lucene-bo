# Lucene Analyzers for Tibetan

This repository contains Lucene tools (analysers, tokenizers and filters) for the Tibetan Language. They are based on [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

Content summary:

- a convertor from EWTS, DTS or ALALC encodings to Tibetan Unicode
- a filter to normalise unicode Tibetan characters
- a filter to remove obvious affixed particles
- a stopword filter
- a syllable-based tokenizer
- a maxmatch-based word tokenizer that:
    - can lemmatize (remove ambiguous affixes ར and ས)
    - uses user-defined word lists

## Installation through maven:

```xml
    <dependency>
      <groupId>io.bdrc.lucene</groupId>
      <artifactId>lucene-bo</artifactId>
      <version>1.2.0</version>
    </dependency>
```

If the jar is needed for use in a non-maven based install, it may be found at

```
    https://repo1.maven.org/maven2/io/bdrc/lucene/lucene-bo/1.2.0/lucene-bo-1.2.0.jar
```

## Building from source

First, make sure the submodule is initialized (`git submodule init`, then `git submodule update` from the root of the repo)

The base command line to build a jar is:

```
mvn clean compile exec:java package
```

The following options alter the packaging:

- `-DincludeDeps=true` includes `io.bdrc.lucene:stemmer` and `io.bdrc.ewtsconverter:ewts-converter` in the produced jar file
- `-DperformRelease=true` signs the jar file with gpg

## Components

#### TibetanAnalyzer

The main Analyzer. 
It tokenizes the input text using *TibSyllableTokenizer*, then applies *TibAffixedFilter* and *StopFilter* with a predefined list of stop words.

There are two constructors. The nullary constructor and

```    
    TibetanAnalyzer(boolean segmentInWords, boolean lemmatize, boolean filterChars, boolean fromEwts, String lexiconFileName)

    segmentInWords - if the segmentation is on words instead of syllables
    lemmatize - if the analyzer should remove affixed particles, and normalize words in words mode
    filterChars - if the text should be converted to NFD (necessary for texts containing NFC strings)
    inputMode - "unicode" (default), "ewts", "dts" (Diacritics Transliteration Schema) or "alalc" ([ALA-LC](https://www.loc.gov/catdir/cpso/romanization/tibetan.pdf))
    stopFilename - file name of the stop word list (defaults to empty string for the shipped one, set to null for no stop words)
```

The nullary constructor is equivalent to `TibetanAnalyzer(true, true, true, false, null)`

#### TibWordTokenizer

This tokenizer produces words through a Maximal Matching algorithm. It builds on top of [this Trie implementation](https://github.com/BuddhistDigitalResourceCenter/stemmer).  

Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
For example, if both དོན and དོན་གྲུབ exist in the Trie, དོན་གྲུབ will be returned every time the sequence དོན + གྲུབ is found.<br>
The sentence སེམས་ཅན་གྱི་དོན་གྲུབ་པར་ཤོག will be tokenized into "སེམས་ཅན + གྱི + དོན་གྲུབ + པར + ཤོག" (སེམས་ཅན + གྱི + དོན + གྲུབ་པར + ཤོག expected)

#### TibSyllableTokenizer

This tokenizer produces syllable tokens (with no tshek) from the input Tibetan text.

#### TibAffixedFilter

This filter removes non-ambiguous affixed particles (འི, འོ, འིའོ, འམ, འང and འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག).

## Maven Build Options

To sign the `.jar`s before deploying, pass `-DperformRelease=true` ; to include `ewts-converter` in the built jar, pass `-DincludeDeps=true`.

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
