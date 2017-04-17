# Lucene Analyzers for Tibetan

This repository contains an upgrade from Lucene 4.4 to Lucene 6.4.1 of [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

## Components

#### TibetanAnalyzer

The main Analyzer. 
It tokenizes the input text using TibSyllableTokenizer, then applies TibAffixedFilter and uses StopFilter with using tibStopWords, the list of non-ambiguous non-affixed particles.
Ex: ལོ, སོ and ཞིང are not in the list.

#### TibSyllableTokenizer

This Tokenizer produces syllabe tokens (with no tshek) from the input Tibetan text.

#### TibAffixedFilter

<<<<<<< WordTokenizer
This Filter removes non-ambiguous affixed particles (འི འོ འིའོ འམ འང འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག)
=======
This filter removes non-ambiguous affixed particles (འི, འོ, འིའོ, འམ, འང and འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག).
>>>>>>> 41f20de cosmetic

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).