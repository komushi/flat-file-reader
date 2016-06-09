# FlatFileReader
## For what is the simulator created for?
The simulator is a program to read from .csv file and then post to HTTP.

## Prerequisites

* Spring XD (1.3.1) or Spring Cloud Data Flow
* Maven

## Quick Start

### Download and Build

```
git clone https://github.com/komushi/flat-file-reader.git
cd flat-file-reader
mvn package
```

### Run FlatFileReader to show help

```
mvn exec:java -Dexec.mainClass="io.pivotal.springxd.simulator.FlatFileReader"
```

Or

```
java -jar target/FlatFileReader-0.1.0-jar-with-dependencies.jar
```

### Run FlatFileReader with Spring XD #

```
mvn exec:java -Dexec.mainClass="io.pivotal.springxd.simulator.FlatFileReader" -Dexec.args="--file=./data/sample_data.csv --url http://localhost:9000 --mode=interactive"
```

Or

```
java -jar target/FlatFileReader-0.1.0-jar-with-dependencies.jar --file=./data/sample_data.csv --url http://localhost:9000 --mode=interactive

```
