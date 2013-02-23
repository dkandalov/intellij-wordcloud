What is this?
=============

This is a plugin for [intellij-eval](https://github.com/dkandalov/intellij_eval) plugin. It can:
 - show word cloud for selected file/package
 - track currently open file and update word cloud automatically

Word analysis is currently based on text (excluding non-letters, splitting CamelHumps, etc).
It should work on any file but it was done for Java-like languages.
The idea is that you should be able to tweak code easily for your language/project (see WordCloud.groovy).
Alternatively it could use syntax tree in IntelliJ to generate word clouds (e.g. based on identifiers only).
This is currently disabled but shouldn't be too difficult to do (see WordCloud.groovy).

See also [Code Words](https://github.com/npryce/code-words) by Nat Pryce.


How to install?
===============
 - install [IntelliJEval plugin](https://github.com/dkandalov/intellij_eval) and the
 - in "Plugins" toolwindow choose "Add Plugin -> Plugin from Git" and use this repository address
 - in "Plugins" toolwindow run "wordcloud" plugin

Visualization is implemented using [d3.js](https://github.com/mbostock/d3) and therefore displayed in a browser.
Yes.. you'll need a browser.


How to use?
===========
I don't really know! Here are some ideas:
 - use on the whole project (or modules) as high-level design feedback.
 E.g. if "string list public void" are dominant it might be "primitives obsession". Obviously, it's always a judgement call.
 - use on unfamiliar code to get an idea of what it's about (interpreted from [Code Words](https://github.com/npryce/code-words))
 - [prime](http://en.wikipedia.org/wiki/Priming_(psychology)) your brain to use words that are in-line with project's vocabulary
 - something else


Screenshots
===========
Word clouds based on plain text, plain text without camel-hump splitting and java identifiers (IntelliJ CE source code).
<img src="https://raw.github.com/dkandalov/intellij-wordcloud/master/screenshots/intellij-split-wordcloud.png" alt="" title="" align="left" />
<img src="https://raw.github.com/dkandalov/intellij-wordcloud/master/screenshots/intellij-wordcloud.png" alt="" title="" align="left" />
<img src="https://raw.github.com/dkandalov/intellij-wordcloud/master/screenshots/intellij-identifier-cloud.png" alt="" title="" align="left" />

