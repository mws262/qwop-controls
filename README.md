# QWOP Controls
[![Build Status](https://travis-ci.com/mws262/qwop-controls.svg?branch=master)](https://travis-ci.com/mws262/qwop-controls)
[![Javadoc](https://img.shields.io/badge/Javadoc-in%20progress-orange.svg)](https://mws262.github.io/qwop-controls/)

Controlling the game [QWOP](http://www.foddy.net/Athletics.html).

Personal research code, not well-formatted for external use.

### Development
All development is being done in IntelliJ after a recent switch from Eclipse. The IntelliJ project files are included
 in the repository here. 
 
 Maven is used to handle fetching dependencies and building.

### Running
        mvn -e exec:java -Dexec.mainClass=FOO

### Documentation

[Javadoc](https://mws262.github.io/qwop-controls/) is being filled out bit by bit as I am trying to improve the 
structure of the project. The Javadoc is directly hosted from the `/docs/` directory on a `github.io` site. This 
directory and the Javadoc itself can be regenerated by running:

        mvn javadoc:javadoc
        
##### Writeups and presentations
Go here.

### Testing

Unit tests are still fairly sparse as coding practices are getting up to speed.

#### Things to note:
1. Substantial structuring changes as the project is migrated from BitBucket.
2. For larger tree build, memory use is high. Increase Java heap space to prevent OutOfMemoryError. e.g. for 10Gb:

        export _JAVA_OPTIONS="-Xmx10g"
    View settings with:

        java -XshowSettings:vm 
