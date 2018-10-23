# Change log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/). It follows [some conventions](http://keepachangelog.com/).
 
## [1.4.3] - 20181023
### Fixed
- fixed offsets of ewts conversion
- fixed resource inclusion

### Added
- possibility to index DTS and ALALC encodings

## [1.2.0] - 20180118
### Fixed
- fixed offsets of ewts conversion

## [1.2.0] - 20180118
### Fixed
- fixed offsets of ewts conversion

### Added
- Tibetan lexicon now included in the .jar file
- maven option `-DincludeDeps=true` to include `ewts-converter`
- possibility to specify a stop word list file in the constructor
- `fromEwts` replaced by `inputMode` in constructor, see [README.md](README.md), allowing DTS and ALA-LC Transliteration schemas

## [1.1.1] - 2017-09-20
### Fixed
- fixed constructor

## [1.1.0] - 2017-09-20
### Added
- possibility to index EWTS text

## [1.0.0] - 2017-04-14
### Added
- Maven packaging
- *TibAffixedFilter*: handle affixed འིའོ, འམ and འང

### Fixed
- *TibAffixedFilter*: when removing an affixed particle, keep the suffix འ if it was in the original syllable (ex: དགའི -> དགའ)

### Changed
- *all*: adaptation to Lucene 6.4.1
- *TibSyllableTokenizer*: consider characters in range `Ux0F84` - `Ux0F8F` to be part of the syllable
