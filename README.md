# clown

Clown is an outliner with notes. It is written in Clojure and ClojureScript.

## Overview

This is pre-alpha software -- very early in development.

Clown lets you build an outline and attach any number of long-form notes to any headline. Editing of outlines and notes is done on the same page making it easier to work on both at the same time. I like to keep the "big picture" in mind, as embodied by the outline, even when editing small details in a note.

The intent is to build this into a personal knowledge management system.

Right now, it only reads a single outline into the program. You can make changes and save them though. The changes will persist from one run of the program to the next.

## Usage

The program is surprisingly useful even in this primitive state.

#### Outliner

#### Notes

#### Size of the Outliner and Notes Panes

Both of these sections will always extend vertically to use all of the space available to them.

The width of the two sections can be changed independently. Hover over the vertical rule on the right side of the section you want to widen or narrow.

As you hover over the rule, it will "fatten" up, giving you a bigger target. Just drag the mouse right or left to the new size you want. The new width will be saved and restored the next time you load the program.

#### Features

- The outliner supports an arbitrary number of headlines nested to an arbitrary depth.
- Each headline can have an arbitrary number of notes attached. There is a practical limit to the number of notes dictated by how many titles can be displayed. Scrolling through the note tabs is not supported yet.

#### Limitations

- Can only load the development outline named `World_Domination_Plan.edn`
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

## What it Does


## Bugs

Probably plenty at this point. If you find one that you need to have fixed, file an issue. I'll see what I can do, but no promises.

## How to Contribute

If you actually get it running and see something that you believe should be changed, open an issue.

I'm not accepting Pull Requests yet.

## License

Copyright © 2020 David D. Clark

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
