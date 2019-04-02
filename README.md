## Table of Contents

- [JdbcSlim](#jdbcslim)
- [Documentation](#documentation)
- [Installation](#installation)
- [Build](#build)

# JdbcSlim

JdbcSlim is the framework to easily integrate database queries and commands into Slim FitNesse testing.
The design focuses to keep configuration data, test data and SQL code separate.
This ensures that requirements are written independent of the implementation and understandable by business users.

The framework can be used by Developers, Testers and Business Users with SQL knowledge.

JdbcSlim supports all databases for which a jdbc driver exists. 

It is agnostic of database system specifics and has no code special to any database system.
Such things should be handled by the jdbc driver.
Nevertheless the jdbc code is segregated from the slim code and adding any driver specific requirements can be done by simply changing a single class.

## Documentation
Read the documentation online: https://six42.github.io/fitnesse-blog/PlugIns.JdbcSlim.UserGuide.html

## Installation

Read [Installation.wiki](https://github.com/six42/jdbcslim/blob/master/FitNesseRoot/PlugIns/JdbcSlim/Installation.wiki) how to use a [jdbcslim release](https://github.com/six42/jdbcslim/releases) in your fitnesse instance:


## Build
You only need to build yourself if you want to contribute to JdbcSlim.

To build jdbcSlim yourself you can use Gradle, Maven or Ant.

Gradle is the preferred build tool and the build script actively maintained.
Maven and Ant are only maintained by the community and might not be up to date.

### Build.Gradle
To build and run all tests, run the command

```
$ ./gradlew test
```

NB. On windows call `gradlew.bat` instead of `./gradlew`.

### Build.Ant
To build jdbcSlim yourself with Ant clone this repository and add the following jar files in the folder `plugins/jdbcslim`:

| **File** | **Link** |
| --- | --- |
| fitnesse-standalone.jar | [fitnesse](http://fitnesse.org) |
| dbfit-core-3.2.0.jar    | [DbFit](https://github.com/dbfit/dbfit/releases/download/v3.2.0/dbfit-complete-3.2.0.zip) - extract jar from zip|
| commons-codec-1.10.jar  | [DbFit](https://github.com/dbfit/dbfit/releases/download/v3.2.0/dbfit-complete-3.2.0.zip) - extract jar from zip|
| h2-1.4.195.jar          | [H2 database](http://www.h2database.com/html/download.html)|
| csvjdbc-1.0.34.jar      | [CsvJdbc](https://sourceforge.net/projects/csvjdbc/files/latest/download)|

Execute `ant jar` to build `jdbcslim.jar` in the folder `plugins/jdbcslim`.
Execute `ant run` to start FitNesse (default: http://localhost:8080).

JdbcSlim testpages available on .Plugins.JdbcSlim.UserGuide (http://localhost:8080/Plugins.JdbcSlim.UserGuide) execute this suite.

Once all tests pass execute `ant build` to validate that the acceptance test pass from the command line.

### Build.Maven
Note: Maven is not the strategic build tool and new versions are not tested against it.

Execute `mvn clean package` to copy dependencies and build `jdbcslim.jar` in the folder `plugins/jdbcslim`.

Execute `mvn exec:exec` to start FitNesse (default: http://localhost:8080).

JdbcSlim testpages available on .Plugins.JdbcSlim.UserGuide (http://localhost:8080/Plugins.JdbcSlim.UserGuide) execute this suite.
 
Verifying the acceptance test pass from them command line is done when you run `mvn clean install`.
