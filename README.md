# Lucene Analyzers for Tibetan

This repository contains Lucene tools (analysers, tokenizers and filters) for the Tibetan Language. They are based on [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

Installation through maven:

```xml
    <dependency>
      <groupId>io.bdrc.lucene</groupId>
      <artifactId>lucene-bo</artifactId>
      <version>1.0.0</version>
    </dependency>
```

## Components

#### TibetanAnalyzer

The main Analyzer. 
It tokenizes the input text using *TibSyllableTokenizer*, then applies *TibAffixedFilter* and *StopFilter* with a predefined list of stop words.

#### TibSyllableTokenizer

This tokenizer produces syllabe tokens (with no tshek) from the input Tibetan text.

#### TibAffixedFilter

This filter removes non-ambiguous affixed particles (འི འོ འིའོ འམ འང འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག).

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).