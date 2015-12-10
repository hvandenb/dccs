# DCCS
Distributed Collaborative Compressive Sampling

The use of compressive sampling algorithms has been used to reduce
sampling on individual motes and in turn energy consumption. Algorithms
like Randomized Timing Vector (RTV) have been
successful in reducing sampling data and energy consumption. These
algorithms have been focused on optimizing the time plane on a single
mote.

When multiple motes are deployed in close proximity those motes could
perform collaborative sampling. The goal of this project is to
validate if compressive sensing can be made distributed by allowing the motes
to collaborate. The motes will distribute the sampling among the
available motes.

## Project

The goal of this project is to setup a distributed process of RTV distribution and sampling.

## Setup

The project requires Java 8 and Maven 3.3.3 to run properly ensure these are installed and ideally setup using the following:
```
export MVN_HOME=~/tools/apache-maven-3.3.3
export JAVA_HOME=~/tools/jdk1.8.0_65
export PATH=$MVN_HOME/bin:$JAVA_HOME/bin:$PATH
```

## Compile

Compile is done using maven

```
mvn compile package spring-boot:repackage
```

## Runing

To run using the packaged properties you can simply run one of the following commands:

```
mvn spring-boot:run

java -jar target/mines-cs565-dccs-0.0.1-SNAPSHOT.jar
```

Any of the properties can be overwriten by passing them along in the command line. The names are all documented in the [application.properties](dccs/src/main/resources/application.properties) file.

```
java -jar target/mines-cs565-dccs-0.0.1-SNAPSHOT.jar --cluster.seeds=127.0.0.1:5555
```
