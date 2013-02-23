What is this?
=============

This is a plugin for [intellij-eval](https://github.com/dkandalov/intellij_eval) plugin. It can:
 - show word cloud for selected file/package
 - track currently open file and update word cloud automatically

Word analysis is currently based on text (excluding non-letters, splitting CamelHumps, etc).
It should work on any file but was done for Java. The idea is that you should be able to tweak it
easily for your language/project. See WordCloud.groovy.

Visualization implemented using [d3.js](https://github.com/mbostock/d3) and therefore displayed in a browser.

See also [Code Words](https://github.com/npryce/code-words) by Nat Pryce.)


How to install?
===============
 - install [IntelliJEval plugin](https://github.com/dkandalov/intellij_eval) and the
 - in "Plugins" toolwindow choose "Add Plugin -> Plugin from Git" and use this repository address
 - in "Plugins" toolwindow run "wordcloud" plugin


How to use?
===========
I don't really know!<br/>
Here are some ideas:
 - TODO



Screenshots
===========
Word clouds based on plain text analysis and java identifiers (IntelliJ CE source code).
<img src="https://github.com/dkandalov/d3_in_intellij/blob/master/screenshots/intellij-wordcloud.png?raw=true" alt="auto-revert screenshot" title="auto-revert screenshot" align="left" />
<img src="https://github.com/dkandalov/d3_in_intellij/blob/master/screenshots/intellij-identifier-cloud.png?raw=true" alt="auto-revert screenshot" title="auto-revert screenshot" align="left" />

