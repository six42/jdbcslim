#Jdbc Slim

JdbcSlim is the framework to easily integrate database queries and commands into Slim FitNesse testing.
The design focuses to keep configuration data, test data and SQL code separate.
This ensures that requirements are written independent of the implementation and understandable by business users.

The framework can be used by Developers, Testers and Business Users with SQL knowledge.

JdbcSlim is agnostic of database system specifics and has no code special to any database system.
Such things should be handled by the jdbc driver.
Nevertheless the jdbc code is segregated from the slim code and adding any driver specific requirements can be done by simply changing a single class.

##Contents:

    Installation
    Suite Set Up
    User Guide *
        1 A Simple Example + ...
        2 Second Example_ Multi Table Statement + ...
        3 Howto Configure The Database Connection + ...
        4 The Mapping Between Test Data And Commands + ...
        5 Parameters Of The S Q L Command +
        6 Output Options + ...
        7 Using Sql In Scripts And Scenarios + ...
        8 Testing On Side Effects +
        Know Limitations +- ...
        Scenario Library
    Xx Test More Drivers *
        Csv Driver * ...
        Derby Test * ...
        Htwo Test * ...



## Installation
Download the latest Jdbc Slim library from github.com\six42\jdbcslim
variable defined: JdbcSlimLib=jdbcslim.jar

Installation Path
Adujust the below path if you installed at a different location
This path is relative to the folder in which FitNesse got started
variable defined: LibInstallPath=plugins\jdbcslim\

The Jdbc Slim Library - always required
classpath: plugins\jdbcslim\jdbcslim.jar

Further dependencies
This is only required to support encryption. It can be downloaded from https://github.com/dbfit/dbfit/releases/tag/v3.2.0
classpath: plugins\jdbcslim\commons-codec-1.9.jar
classpath: plugins\jdbcslim\dbfit-core-3.2.0.jar

See the SuiteSetup pages for driver specific setup.
JDBC driver used for the samples. Not required if you use a different JDBC driver
plugins\jdbcslim\h2-1.4.188.jar
plugins\jdbcslim\csvjdbc-1.0-18.jar

## User Manual and Test Suite
To access the user manual and run the examples copy all Jdbc Slim Fitnesse pages from github to FitNesseRoot\PlugIns
Copy from github the folder plugins\jdbcslim\TestDB to plugins\jdbcslim\TestDB in your installation
variable defined: TestDbPath=${LibInstallPath}TestDB\

Execute the suite on .PlugIns.JdbcSlim.UserGuide


## To build your own test pages
1. include the Installation page on the root page of your suite
2. include the SuiteSetup page or a page with similar content in the setup of your suite 
