
# Lucene Analyzers for Tibetan

A collection of Lucene components (analyzers, tokenizers, and filters) for processing Tibetan text.

---

## Features

- **Encoding Conversion:** Convert EWTS, DTS, or ALALC encodings to Tibetan Unicode.
- **Unicode Normalization:** Normalize Tibetan Unicode characters for consistent search and analysis.
- **Affixed Particle Removal:** Remove obvious affixed particles at the end of syllables (e.g., `..འི`).
- **Syllable-Based Tokenization:** Tokenize text into syllables, with fallback to stack tokenization for non-standard syllables (useful for Sanskrit).
- **Phonetic Analyzers:** Search using phonetic representations, supporting both Tibetan and Latin-script queries.

---

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
  <groupId>io.bdrc.lucene</groupId>
  <artifactId>lucene-bo</artifactId>
  <version>2.2.0</version>
</dependency>
```

---

## Components

### TibetanAnalyzer

- Tokenizes input using `TibSyllableTokenizer`.
- Applies `TibAffixedFilter` and `StopFilter` with a predefined stop word list.

### Old Tibetan Normalization

Implements normalization patterns from Faggionato & Garrett (see [reference](https://ep.liu.se/konferensartikel.aspx?series=&issue=168&Article_No=3)), including:
- Gigu normalization
- Dadrag removal
- Medial འ removal in `TibAffixedFilter`

Patterns are based on [Normalize_Old_Tibetan.txt](https://github.com/tibetan-nlp/tibcg3/blob/master/Normalize_Old_Tibetan.txt).

### Lenient Character Normalization

- Normalizes retroflexes (e.g., ཊ → ཏ)
- Normalizes graphical variants (e.g., ཪ → ར)
- Removes achung
- Normalizes gigus

### Tokenizers & Filters

- **TibSyllableTokenizer:** Produces syllable tokens (without tshek). Falls back to stack tokenization for non-standard syllables.
- **TibAffixedFilter:** Removes non-ambiguous affixed particles (e.g., འི, འོ, འིའོ, འམ, འང, འིས), preserving འ when necessary.
- **PaBaFilter:** Normalizes བ and བོ to པ and པོ. Should be used after `TibAffixedFilter`.

---

## Phonetic Analyzers

Tibetan has high spelling opacity, leading to many homophones. This package provides:

- **Index Analyzer:** Converts Tibetan Unicode to an internal phonological notation.
- **Query Analyzer:** Converts Latin-script phonetic queries to the same internal notation.

**Example:**

| Input                | Analyzer         | Output tokens      |
|----------------------|------------------|--------------------|
| སྒམ་པོ་པ།         | index            | gam, po, pa        |
| རྒམ་པོ་པ།         | index            | gam, po, pa        |
| Gampopa              | query            | gam, po, pa        |

**Phonetic System:**
- No voicing distinction (`ka` = `ga`)
- No aspiration distinction (`ka` = `kha`)
- No tone distinction
- Oriented towards Standard Tibetan pronunciation

**Caveats:**
- Some syllables are ambiguous (e.g., བར་ can be `bar` or `war`)
- Pronunciation exceptions are mapped to normalized forms (e.g., "Khandro" → "khadro")

---

## Building

To build from source:

1. Initialize submodules:
  ```bash
  git submodule init
  git submodule update
  ```
2. Build the JAR:
  ```bash
  mvn clean compile exec:java package
  ```

**Build Options:**

- `-DincludeDeps=true`: Includes `io.bdrc.lucene:stemmer` and `io.bdrc.ewtsconverter:ewts-converter` in the JAR.
- `-DperformRelease=true`: Signs the JAR with GPG.

---

## Acknowledgements

Based on [lucene-analyzers](https://github.com/tibetan-nlp/lucene-analyzers).

---

## License

Copyright 2017 Buddhist Digital Resource Center  
Licensed under the [Apache License 2.0](LICENSE).

