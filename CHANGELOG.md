# Change log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/). It follows [some conventions](http://keepachangelog.com/).
 
 ## [1.0.0] - 2017-04-14
 ### Added
- Maven packaging
- *TibAffixedFilter*: handle affixed འིའོ, འམ and འང

### Fixed
- *TibAffixedFilter*: when removing an affixed particle, keep the suffix འ if it was in the original syllable (ex: དགའི -> དགའ)

### Changed
- *all*: adaptation to Lucene 6.4.1
- *TibSyllableTokenizer*: consider characters in range `Ux0F84` - `Ux0F8F` to be part of the syllable
