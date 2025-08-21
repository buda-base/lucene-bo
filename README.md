# Lucene Analyzers for Tibetan 

This repository contains Lucene components (analysers, tokenizers and filters) for Tibetan.

Content summary:

- a convertor from EWTS, DTS or ALALC encodings into Tibetan Unicode
- a filter to normalize Unicode Tibetan characters
- a filter to remove obvious affixed particles at the end of syllables (ex: `..འི`)
- a syllable-based tokenizer, with fallback to stack tokenizer on non-standard syllables to better accomodate Sanskrit
- a set of phonetic analyzers to allow search in phonetics

## Installation through maven:

```xml
    <dependency>
      <groupId>io.bdrc.lucene</groupId>
      <artifactId>lucene-bo</artifactId>
      <version>2.2.0</version>
    </dependency>
```

## Components

#### TibetanAnalyzer

It tokenizes the input text using *TibSyllableTokenizer*, then applies *TibAffixedFilter* and *StopFilter* with a predefined list of stop words.

#### Old Tibetan normalization

The analyzer implements most of the patterns that have been listed in the context of Faggionato, C. & Garrett E., Constraint Grammars for Tibetan Language Processing, https://ep.liu.se/konferensartikel.aspx?series=&issue=168&Article_No=3 . The list of patterns can be found on https://github.com/tibetan-nlp/tibcg3/blob/master/Normalize_Old_Tibetan.txt .

This mode also normalizes the gigu to just one form, removes the dadrag, and the medial འ in the TibAffixedFilter (see below).

#### Lenient char normalization

The lenient normalization normalizes a number of features that are found mostly in Sanskrit text and normalize them to more regular Tibetan features. One of the goals is that the search is less case sensitive in EWTS. This includes:
- retroflexes are "reversed": ཊ -> ཏ
- graphical variants are normalized: ཪ (fixed R form) -> ར (regular r)
- achung are removed
- gigus are normalized in one direction

#### TibSyllableTokenizer

This tokenizer produces syllable tokens (with no tshek) from the input Tibetan text. Optionally, if it detects a syllable that is not following the rules of Classical Tibetan syllable formation, it switches to produce one token per stack instead. This allows better segmentation of Sanskrit passages.

#### TibAffixedFilter

This filter removes non-ambiguous affixed particles (འི, འོ, འིའོ, འམ, འང and འིས), leaving the འ if necessary (ex: དགའི -> དགའ, གའི -> ག).

#### PaBaFilter

This filter normalizes བ and བོ into པ and པོ. It does not look into affixed particles and thus should be used after `TibAffixedFilter`.

## The phonetic analyzers

Tibetan is a language with a very high spelling opacity (letter-sound and sound-letter consistency), as do most languages where spelling has not been reformed in a long time (other examples are English or French). This leads to a very high number of homophones.

Our solution is to have two analyzers:
- **indexing**: an analyzer taking a Tibetan Unicode string and produces tokens encoded in an internal phonological notation (see below)
- **query**: an analyzer taking a typical simplified phonetic rendering in Latin characters and produces tokens encoded in the same internal phonological system

For instance:
- `སྒམ་པོ་པ།` -> index analyzer ->  `gam` `po` `pa`
- `རྒམ་པོ་པ།` -> index analyzer -> `gam` `po` `pa`
- `Gampopa` -> query analyzer -> `gam` `po` `pa`

#### Internal phonetic notation

The system we use is purely internal and thus does not need to follow conventions, but it uses the following simplifications to maximize leniency:
- no distinction of voicing (`ka` = `ga`)
- no distinction of aspiration (`ka` = `kha`)
- no distinction of tone

The system is generally oriented towards Standard Tibetan pronounciation, and is not lenient enough to accomodate Khampa, Amdoa pronounciation.

#### Caveats

Since our analyzer will operate at the syllable level, the prononciation of some syllabes cannot be unambiguously determined. )This is the case for instance for `བར་` which can be pronounced `bar` or `war`. We compensate that with a few regex giving the most likely version.

In our system the pronounciation exceptions are mapped into a normalized form. For instance `Khandro` (for *མཁའ་འདྲོ།*) is pre-processed into `khadro` so that it can easily match syllable by syllable, without having to implement nasalizations in the processing of the Tibetan string, which can be challenging.


## Maven Build Options

To sign the `.jar`s before deploying, pass `-DperformRelease=true` ; to include `ewts-converter` and `stemmer` in the built jar, pass `-DincludeDeps=true`.

To build from sources, first make sure the submodule is initialized (`git submodule init`, then `git submodule update` from the root of the repo)

The base command line to build a jar is:

```
mvn clean compile exec:java package
```

The following options alter the packaging:

- `-DincludeDeps=true` includes `io.bdrc.lucene:stemmer` and `io.bdrc.ewtsconverter:ewts-converter` in the produced jar file
- `-DperformRelease=true` signs the jar file with gpg


## Acknowledgements

The code was initially based on [these Lucene analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
