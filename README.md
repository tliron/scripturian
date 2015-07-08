
Scripturian
===========

Scripturian is a library that makes it easy to integrate various programming
and templating languages into your JVM-based software. It is designed from the
ground-up for high performance concurrent execution, and as such offers a
scalable alternative to the Java scripting standard (JSR-223).

Please see the [Scripturian web site] (http://threecrickets.com/scripturian/)
for comprehensive documentation. Here we will only explain how to build it.

[![Download](http://threecrickets.com/media/download.png "Download")](http://threecrickets.com/scripturian/download/)

Maven:

    <repository>
        <id>threecrickets</id>
        <name>Three Crickets Repository</name>
        <url>http://repository.threecrickets.com/maven/</url>
    </repository>
    <dependency>
        <groupId>com.threecrickets.scripturian</groupId>
        <artifactId>scripturian</artifactId>
    </dependency>


Building Scripturian
--------------------

All you need to build Scripturian is [Ant] (http://ant.apache.org/).

Simply change to the "/build/" directory and run "ant".

Your JDK should be at least version 8 in order to support the Nashorn adapter,
although there is a workaround for earlier JDK versions (see comment in
"/build/custom.properties".)

During the build process, build and distribution dependencies will be
downloaded from an online repository at http://repository.threecrickets.com/, so
you will need Internet access.

The result of the build will go into the "/build/distribution/" directory.
Temporary files used during the build process will go into "/build/cache/",
which you are free to delete.


Configuring the Build
---------------------

The "/build/custom.properties" file contains configurable settings, along with
some commentary on what they are used for. You are free to edit that file,
however to avoid git conflicts, it would be better to create your own
"/build/private.properties" instead, in which you can override any of the
settings. That file will be ignored by git.

To avoid the "bootstrap class path not set" warning during compilation
(harmless), configure the "compile.boot" setting in "private.properties" to the
location of an "rt.jar" file belonging to JVM version 6.


Deploying to Maven
------------------

You do *not* need Maven to build Scripturian, however you can deploy your build
to a Maven repository using the "deploy-maven" Ant target. To enable this, you
must install [Maven] (http://maven.apache.org/) and configure its path in
"private.properties".


Still Having Trouble?
---------------------

Join the [Scripturian Community]
(http://groups.google.com/group/scripturian-community), and tell us where you're
stuck! We're very happy to help newcomers get up and running.