# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [1.0.2] - 2016-10-05
### Changed
 - Upgraded Java Manta SDK to 2.4.3.
 - Upgraded Hadoop to 2.7.3.
### Fixed
 - Changed detection of an embedded configuration encryption key 
   and encryption key file path detection such that embedded keys
   will not trigger an error condition.

## [1.0.1] - 2016-06-08
### Added
 - We now recursively create missing paths when create() is called.

## [1.0.0] - 2016-06-01
### Added
 - Initial release.
