## NoQMS Runner - Dynamic Microservice Runner/Loader

NoQMS runner is the main() around your microservices with logging integration and dynamic 
microservice lifecycle management based on individual microservice configuration files.  
Place your *.micro microservice configuration files in the path defined by the noqms.microConfigPath command line variable. 
The NoQMS runner then monitors the microservices definitions there and dynamically loads or 
unloads the microservices whenever changes are made to the microservice definition files.

## Configuration Files

Create a *.micro file for each of your microservices. Withing this properties file are the key value
pairs specific to the noqms microservice as well as any application specific properties you wish to
configure as well. An additional enable=true/false directive tells the NoQMS Runner whether
to load or unload the microservice.
 
An example "login.micro" file:
>enable=true
>
>noqms.groupName=MyApp
>noqms.serviceName=Login
>noqms.servicePath=com.mycompany.micro.Login
>noqms.threads=1
>noqms.timeoutMillis=4000
>noqms.maxMessageOutBytes=10000
>noqms.maxMessageInBytes=1000
>
>myapp.myKey=true (etc)
 
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
* Place your *.micro microservice definition files into a config directory along with
	a logback.xml containing your desired logging parameters.
* cd to the directory containing the noqms, noqms-runner, and dependency jars.
* Execute: java -server -cp *:(path to your microservice jars):(path to your *.micro config files) com.noqms.Runner noqms.microConfigPath=(path to your *.micro config files)

