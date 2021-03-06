# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unpublished]

### Added ###

- Added and "About" box with a little program information. It can be accessed with `Ctrl-Shft-A`, although the keystroke may change in the future. This resolves [issue #4](https://github.com/clartaq/clown/issues/4).
- Added a `CHANDELOG.md` file to show summarize interesting changes.
- Added an initial version of Markdown editing to the outliner.
- Added information to the `README.md` file about Markdown editing in the outliner.
- Added more `.gitignore` settings for emacs backup files and to make `magit` status display more readable.
- Added a keyboard shortcut to insert a timestamp into the outline.

### Changed ###

- Improved CSS for `<code>` and `<pre>` styling in the outliner.
- Updated dependencies.

### Deprecated ###

### Removed ###

- Removed an unneeded dependency that was used to compensate for a bug in the [kaocha](https://github.com/lambdaisland/kaocha) testing library.

### Fixed ###

- Fixed [issue #7](https://github.com/clartaq/clown/issues/7). "Editing the File Name of the Outline can cause Multiple Saves to Different Files." Now the file is not saved with a new name until after the cursor leaves the edit control for the file name.

- Fixed [issue #3](https://github.com/clartaq/clown/issues/3). "Up and Down Arrow Keys do not Move Cursor Correctly." Now when moving up and down in a long headline, the caret is moved one line at a time until it reaches the top/bottom line. Then it moves the the previous/next headline.

    This behavior is still different than the way the caret moves between paragraphs in a "vanilla" `textarea`.

- Fixed some directory exclusion problems with the IntelliJ project information.

### Security ###

## [0.1.0] -- 2 December 2020

### Added ###

Everything.
