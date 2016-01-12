#Jdbc Slim

JdbcSlim is the framework to easily integrate database queries and commands into Slim FitNesse testing.
The design focuses to keep configuration data, test data and SQL code separate.
This ensures that requirements are written independent of the implementation and understandable by business users.

The framework can be used by Developers, Testers and Business Users with SQL knowledge.

JdbcSlim supports all databases for which a jdbc driver exists. 

It is agnostic of database system specifics and has no code special to any database system.
Such things should be handled by the jdbc driver.
Nevertheless the jdbc code is segregated from the slim code and adding any driver specific requirements can be done by simply changing a single class.

###Read the documentation online 
https://rawgit.com/six42/jdbcslim/master/JdbcSlim.htm
###Or download it for correct formatting
https://raw.githubusercontent.com/six42/jdbcslim/master/JdbcSlim.htm
