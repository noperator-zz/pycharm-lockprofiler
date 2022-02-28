# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Plugin available for 203 - 221.*

### Fixed
- Bug that did not allow multi-caret editing when profile results were being visualized

## [1.5.0] - 2022-02-16
### Added
- More colormaps. Different colormaps can now be selected in the plugin settings.

### Changed
- Made the max table alignment column configurable

### Fixed
- Use longs for 'time' and 'hits' instead of ints to prevent NumberFormatExceptions

## [1.4.0] - 2021-12-01
### Changed
- Removed deprecated code
- Plugin available for 203-213.*

## [1.3.0] - 2021-07-26
### Changed
- Use more clear units for time (microseconds, milliseconds and seconds)
- Plugin available for 203 - 212.*

## [1.2.0] - 2021-06-14
### Added
- Profile result colors are also visible as scrollbar error stripes

### Changed
- Use line-profiler-pycharm v1.1.0
  This makes it possible to use the PyCharm debugger while methods are decorated with @profile

## [1.1.0] - 2021-04-14
### Changed
- Plugin is made available for 203 - 211.*

## [1.0.0] - 2021-04-13
### Added
- Initial version with a profile executor

[Unreleased]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/tree/dev
[1.5.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.5.0
[1.4.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.4.0
[1.3.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.3.0
[1.2.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.2.0
[1.1.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.1.0
[1.0.0]: https://gitlab.com/line-profiler-pycharm/line-profiler-pycharm-plugin/-/tags/1.0.0
