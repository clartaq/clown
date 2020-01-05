# clown

Clown is an outliner with notes. It is written in Clojure and ClojureScript.

## Overview

This is pre-alpha software -- very early in development. Most features not implemented.

Clown lets you build an outline and attach any number of long-form notes to any headline. Editing of outlines and notes is done on the same page making it easier to work on both at the same time. I like to keep the "big picture" in mind, as embodied by the outline, even when editing small details in a note.

The intent is to build this into a personal knowledge management system.

## Usage

The program is surprisingly useful even in this primitive state. By default, when the program is opened, it will look for and load a file named `World_Domination_Plan.edn`, included in the repository.

You can make changes to the plan or open any other `.edn` file of the proper format by clicking the `Open` button. You can start a new outline using the `New` button. (Sorry, no keyboard shortcut yet.) You can save at any time by typing `Cmd-S` or clicking the `Save` button. 

#### Outliner

The outliner is pretty standard. Just type stuff in. Headlines work like most other text entry areas. Any headline can be as long as you want it to be.

The `Backspace` and `Delete` characters work pretty much as expected. They will also cross headline boundaries. For example, you could put the cursor on the last character in an outliner and erase the entire outline by continuing to press `Backspace`.

Headings with subheadings will display a chevron character (&#9660; or &#9658;) in the left gutter. Headlines without children display a bullet (&#9679;). When a right-pointing chevron (&#9658;) is displayed, the heading has children that are hidden. They can be displayed by clicking the chevron or typing `Cmd-0`. An expanded headline can be collapsed, hiding its children, by clicking the the down-pointing chevron (&#9660;) or typing `Cmd-9`.

To create a new headline below the current one, hit `Return`. To insert a new headline above the current on, press `Shft-Return`.

To indent/undent (demote/promote) a headline use the `Tab` and `Shft-Tab` keys respectively.

You can move headlines up or down by pressing `Alt-Cmd-`&#8593; or `Alt-Cmd-`&#8595;, respectively

There are more keyboard commands. If you are interested, check the source. They are likely to change in the future thought.

#### Notes

Any heading can have an arbitrary number of notes associated with it. (There is a practical limit at the moment. Only one note is displayed at a time. I have not yet devised a clever way to selecte among a very large number of notes to select the one to display.) Each note must have a unique (to the outline heading) title.

Any headline that has associated notes will display a small "note" icon in the left border. Clicking the headline (not the note icon) will display a tabbed page with the titles of all of those notes. Clicking a title will bring that note forward for display.

The note display is just a regular text area at the moment. You can add as much text as you want.

To add a note, click the button with the plus sign above the note display area. To delete a note, click the `x` displayed next to the note title.

#### Size of the Outliner and Notes Panes

Both of these sections will always extend vertically to use all of the space available to them.

The width of the two sections can be changed independently. Hover over the vertical rule on the right side of the section you want to widen or narrow.

As you hover over the rule, it will "fatten" up, giving you a bigger target. Just drag the mouse right or left to the new size you want. The new width will be saved and restored the next time you load the program.

#### Features

- The outliner supports an arbitrary number of headlines nested to an arbitrary depth.
- Each headline can have an arbitrary number of notes attached. There is a practical limit to the number of notes dictated by how many titles can be displayed. Scrolling through the note tabs is not supported yet.

#### Limitations

- There is no text formatting yet. No images. No links. No LaTeX. No code-highlighting.
- The keybindings are horrible. Trying to get a set of keybindings that work the save across browsers without screwing something else up is hard.

## Building

For building development and "production" versions of clown, you will need Java and Clojure. I have been doing my work with Java 11 and Clojure 1.10.1.

I've been testing on Safari, Firefox, Opera, and Brave browsers.

### Development

To get an interactive development environment run:

    clj -A:fig:repl

This will compile a development build, open a tab in the default browser at [`localhost:1597`](http://localhost:1597), and start a ClojureScript REPL in the terminal. Changes made to the ClojureScript portion of the project will be compile and reloaded in real time. Changes affecting the browser display (Reagent components) will show up in the browser as well.

    clj -A:fig:dev

Similar to the above but does not open a REPL.

### Production

To build an uberjar for production run:

    clj -A:fig:prod

To run the uberjar, you need Java. Clojure is not required just to run the program.

You can run the resulting jar from the project directory by entering:

    java -jar target/clown.jar

Launching the uberjar will also open a tab in your default browser.

(The `prod.clj` script, which builds the uberjar, has a setting to generate an uberjar with a name that is easier to type. The setting is marked in the source if you want to change it back to the more traditional `clown-0.1.0-SNAPSHOT-standalone.jar`.)

## Testing

Testing scripts are provided for both Clojure and ClojureScript unit testing.

### ClojureScript

After starting a development session, figwheel.main does automatic testing as well. Navigate your browser to [`http://localhost:9500/figwheel-extra-main/autotesting`](http://localhost:9500/figwheel-extra-main/auto-testing) to see the test results.

To do one-time ClojureScript testing, run:

    clj -A:fig:test-cljs

Test results will show up in the terminal. A testing web page will be opened, but you can just close it after the tests have completed.

### Clojure

To run Clojure unit tests just run:

    clj -A:test-clj

The results will appear in the terminal.

## Other Aliases

To delete all compilation artifacts, use:

    clj -A:clean

To check for outdated dependencies, run:

    clj -A:ancient

## Bugs

Probably plenty at this point. If you find one that you need to have fixed, file an issue. I'll see what I can do, but no promises.

## To Do

- As mentioned above, keyboard shortcuts are a mess.
- The undo/redo architecture is really wrong for this architecture.
- More complete usage documentation.
- Printing? How would you even do it?
- Checkboxes?
- LaTeX
- Syntax highlighting
- Stats: # Headlines, words, characters
- Open files with Drag 'n Drop
- Search
    + Entire document
    + Outline only
    + Notes only
    + Outline branch
    + All notes associated with a headline
- Import/export OPML?
- Version control?
- Version comparison?
- Change indication?

## How to Contribute

If you actually get it running and see something that you believe should be changed, open an issue.

I'm not accepting Pull Requests yet.

## License

Copyright © 2020 David D. Clark

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
