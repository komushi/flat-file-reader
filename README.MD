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
mvn clean package
```

### Run FlatFileReader to show help

```
mvn exec:java -Dexec.mainClass="io.pivotal.springxd.simulator.FlatFileReader"
```

Or

```
java -jar target/FlatFileReader-0.1.0-jar-with-dependencies.jar --file=./data/sample_data.csv --url http://localhost:9000 --mode=bulk --interval=200
```

### Run FlatFileReader with Spring XD #

```
mvn exec:java -Dexec.mainClass="io.pivotal.springxd.simulator.FlatFileReader" -Dexec.args="--file=./data/sample_data.csv --url http://localhost:9000 --mode=interactive"
```

Or

```
java -jar target/FlatFileReader-0.1.0-jar-with-dependencies.jar --file=./data/sample_data.csv --url http://localhost:9000 --mode=interactive

```

```
java -jar target/FlatFileReader-0.1.0-jar-with-dependencies.jar --file=./data/sample_data.csv --url http://localhost:9000 --mode=measure-delay --listen-port=9000

```

```
#!SpringXD DSL
stream create --name http2log --definition "http --port=9000 | log " --deploy
stream create --name tap2tcp --definition "tap:stream:http2log > tcp --port=9090" --deploy

```

dataflow shell
```
stream create --name http2log --definition "http --server.port=9000 | log " --deploy
stream create --definition ":http2log.http > tcp --host=localhost --port=9090" --name tap2tcp --deploy
```

## Run with Spring Cloud Dataflow, Stream and Gemfire
dataflow shell
```
app register --name flat2tuple --type processor --uri file:///Users/lei_xu/Desktop/Flat2TupleTransformer-0.0.1-SNAPSHOT.jar

stream create http2gem --definition "http --server.port=9000 | flat2tuple --outputType=application/json | gemfire --regionName=RegionRaw --json=true --keyExpression=payload.getField('hashCode')" --deploy

http post --target http://localhost:9000  --data "468244D1361B8A3EB8D206CC394BC9E9,BB899DFEA9CC964B50C540A1D685CCFB,2013-01-01 00:00:00,2013-01-01 00:04:00,240,1.71,-73.855383,40.779728,-73.967758,40.760326,CSH,6.50,0.50,0.50,0.00,0.00,7.50"
```
