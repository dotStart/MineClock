[![License](https://img.shields.io/github/license/dotStart/MineClock.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![GitHub Release](https://img.shields.io/github/release/dotStart/MineClock.svg?style=flat-square)](https://github.com/dotStart/MineClock/releases)
[![CircleCI](https://img.shields.io/circleci/project/github/dotStart/MineClock.svg?style=flat-square)](https://circleci.com/gh/dotStart/MineClock)

MineClock
=========

Did you ever wish there was an absolutely over engineered clock which would give you a simple
indication of whether it is dark on the surface or whether it rains while you are mining in the
depths of Moria?

No? Me neither. But let's get you introduced to MineClock anyways:

![Screenshot](https://i.imgur.com/sEG8khC.gif)

This Java based program easily integrates with Minecraft and allows you to see what time it is while
you focus on mining or PvP (I especially recommend trying to run it on another screen while playing
UHC). As part of the program there is two methods of receiving correct times:

* By installing the Java Development Kit and letting the magic happen
* By timing correctly using one of the four settings for Morning, Noon, Evening and Midnight respectively.

Table of Contents
-----------------
* [Contacts](#contacts)
* [License](#license)
* [Downloads](#downloads)
* [Issues](#issues)
* [Building](#building)
* [Contributing](#contributing)

Contacts
--------

* [IRC #.start on EsperNet](http://webchat.esper.net/?channels=.start)
* [GitHub](https://github.com/dotStart/MineClock)

License
-------

Copyright (C) 2016 Johannes "Akkarin" Donath and other copyright owners as documented in the project's IP log.
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.txt)

Downloads
---------

Released versions of the library can be found on [GitHub](https://github.com/dotStart/MineClock/releases).

Issues
------

You encountered problems with the library or have a suggestion? Create an issue!

1. Make sure your issue has not been fixed in a newer version (check the list of [closed issues](https://github.com/dotStart/MineClock/issues?q=is%3Aissue+is%3Aclosed)
1. Create [a new issue](https://github.com/dotStart/MineClock/issues/new) from the [issues page](https://github.com/dotStart/MineClock/issues)
1. Enter your issue's title (something that summarizes your issue) and create a detailed description containing:
   - What is the expected result?
   - What problem occurs?
   - How to reproduce the problem?
   - Crash Log (Please use a [Pastebin](http://www.pastebin.com) service)
1. Click "Submit" and wait for further instructions

Building
--------

1. Clone this repository via ```git clone https://github.com/dotStart/MineClock.git``` or download a [zip](https://github.com/dotStart/MineClock/archive/master.zip)
1. Build the modification by running ```mvn clean package```
1. The resulting jars can be found in their respective ```target``` directories as well as your local maven repository

Contributing
------------

Before you add any major changes to the library you may want to discuss them with us (see [Contact](#contact)) as
we may choose to reject your changes for various reasons. All contributions are applied via [Pull-Requests](https://help.github.com/articles/creating-a-pull-request).
Patches will not be accepted. Also be aware that all of your contributions are made available under the terms of the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt). Please read the [Contribution Guidelines](CONTRIBUTING.md)
for more information.
