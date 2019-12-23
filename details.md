# Some Details

Some project details that you might find interesting are included here.

## WebSockets

My programming career goes back decades. But it was almost all embedded or desktop-based. As such, moving to web-based applications was all new to me -- very exciting. But I never used [AJAX](https://en.wikipedia.org/wiki/Ajax_%28programming%29) and found it confusing and convoluted, at least in appearance.

By comparison, [WebSockets](https://en.wikipedia.org/wiki/WebSocket) seem very easy and straightforward. I think this example program demonstrates that. It is so simple, you can write your own WebSocket handling for the client in well under 100 lines. Many (most?) servers have the capabilities built in and seem just as easy.

## Leiningen

The project does not use [Leiningen](https://leiningen.org) to specify and control the build. But it does use Leiningen to create the uberjar. As such, Leningen is specified as a depenedency for the "prod" script to create the uberjar. That is also the reason for the `project.clj` file in the source directory. Leiningen needs the `project.clj` file to run.

## Editorializing

After trying it for awhile, I don't find deps-based project management in any way superior to using Leiningen.

I suppose it's nice to have some sort of project management built into the base Clojue distribution but scattering the build configuration around into multiple files does not seem to be an improvement.

I have the same feelings about figwheel-main. Scattering build configuration into multiple files is just not a good idea in my opinion. I can't tell you how many times I thought to myself "Now, where is that piece of configuration recorded?" when learning the "new" way of doing things.

One thing that **is** better is the setup for ClojureScript unit testing that is available in figwheel-main. There's no fiddling around downloading some shiny new testing framework that will go out of fashion and support next week, along with all the neccessary JavaScript stuff to support it. Figwheel-main is **so** much better. In fact, that was the impetus that persuaded me to switch to deps and fighwheel-main in the first place.