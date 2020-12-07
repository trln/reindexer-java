# Java-based Reindexer

This contains an application, written in Java, that reads all non-deleted records from the 
`trln-ingest` database and ingests them in batches of a configurable size, using 
the `argot ingest` command from the [argot-ruby](https://github.com/trln/argot-ruby) gem.

It is largely functionally identical to the [Golang-based version](https://github.com/trln/reindexer) 
and uses the same format for a configuration file.

## Building

Should work with JDK 8 or JDK 11 (RPMs `java-1.8.0-openjdk-devel` or `java-11-openjdk-devel`, should also work with Amazon Coretto 
JDKs). Ensure the appropriate version is installed and run

    $ ./gradlew shadowJar
    
In the project directory.

## Running

    $ java -jar build/libs/reindexer-all.jar
    
Note that this assumes an appropriate `config.json` file is in the working directory. 
