# Lucene Analyzers for Tibetan

This repository contains an upgrade from Lucene 4.4 to Lucene 6.4.1 of [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

## Components

#### TibetanAnalyzer

The main Analyzer. 
It tokenizes the input text using TibSyllableTokenizer, then applies TibAffixedFilter and uses StopFilter with using tibStopWords, the list of non-ambiguous non-affixed particles.
Ex: ལོ, སོ and ཞིང are excluded.

#### TibSyllableTokenizer

This Tokenizer produces a TokenStream (syllables with no tshek) from the input Tibetan text.

#### TibAffixedFilter

This Filter removes non-ambiguous affixed particles (འི འོ འིའོ འམ འང འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག)

## Compiling and running

(to come)

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).