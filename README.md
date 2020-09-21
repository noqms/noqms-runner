## NoQMS Runner - Dynamic Microservice Loader/Runner

NoQMS runner is the main() around your microservices with logging integration and dynamic
microservice lifecycle management based on individual microservice configuration files.
Place your microservice configuration files (with a suffix of .micro) in the path defined by the noqms.microConfigPath command line variable.
The NoQMS runner then monitors the microservice definitions there and dynamically loads (with new properties if changed) or
unloads a given microservice whenever changes are made to its definition file.

## Configuration Files

Create a configuration file for each of your microservices. Withing this file are the key value
pairs (property file format) specific to any noqms microservice as well as your own optional application properties. 
An additional enable=true/false directive tells the NoQMS Runner whether
to load or unload the microservice. An example configuration file is included in this project.
 
## Running

To run NoQMS microservices using the runner found in this project:

* Download and install the latest [OpenJDK](http://openjdk.java.net/)
* Download the latest dependencies: 
	[gson](https://mvnrepository.com/artifact/com.google.code.gson/gson)
	[logback-core](https://mvnrepository.com/artifact/ch.qos.logback/logback-core)
	[logback-classic](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic)
	[slf4j](https://mvnrepository.com/artifact/org.slf4j/slf4j-api)
* Download the [noqms](https://github.com/noqms/noqms) project and compile the noqms jar.
* Download this noqms-runner project and compile the noqms-runner jar.
* Put the jars and dependencies together into a directory.
* Put your own microservice jars in a directory.
* Place your \*.micro microservice definition files into a config directory along with
	a logback.xml containing your desired logging parameters. An example logback.xml i
	included in this project.
* cd to the directory containing the noqms, noqms-runner, and dependency jars.
* Execute: java -server -cp \*:(path to your microservice jars):(path to your \*.micro config files) com.noqms.runner.Runner noqms.microConfigPath=(path to your \*.micro config files)

