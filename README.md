# jag-adobe-common-service

[![Lifecycle:Experimental](https://img.shields.io/badge/Lifecycle-Experimental-339999)](https://github.com/bcgov/jag-adobe-common-service)
[![Maintainability](https://api.codeclimate.com/v1/badges/a492f352f279a2d1621e/maintainability)](https://codeclimate.com/github/bcgov/jag-adobe-common-service/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/a492f352f279a2d1621e/test_coverage)](https://codeclimate.com/github/bcgov/jag-adobe-common-service/test_coverage)

### Recommended Tools
* Intellij
* Docker
* Maven
* Java 11
* Lombok

### Application Endpoints

Local Host: http://127.0.0.1:8080

Code Climate: https://codeclimate.com/github/bcgov/jag-adobe-common-service

WSDL Endpoint Local:
* localhost:8080/ws/AdobeCommonServices.Source.CommonServices.ws.provider:PDFDiagnosticsWS?WSDL

* localhost:8080/ws/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsWS?WSDL

* localhost:8080/ws/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsSCPWS?WSDL

### Required Environmental Variables

BASIC_AUTH_PASS: The password for the basic authentication. This can be any value for local.

BASIC_AUTH_USER: The username for the basic authentication. This can be any value for local.

LIFECYCLE_HOST: The url for lifecycle web services.

SFEG_USERNAME: username for ssh connection for transferring scp files

SPLUNK_HTTP_URL: The url for the spluck hec. For local splunk this value should be 127.0.0.1:8088 for
remote do not include /services/collector.

SPLUNK_TOKEN: The bearer token to authenticate the application.

SPLUNK_INDEX: The index that the application will push logs to. The index must be created in splunk
before they can be pushed to.

NFS_DIR: SFEG target directory for NFS transfer

SFTP_PRIVATE_KEY: Path to private key file

SFTP_KNOWN_HOSTS: Path to known hosts

SFEG_HOST: url of the secure File Transfer Protocol (SFTP)

WEBSERVICE_READ_TIMEOUT: Timeout in milliseconds which expects the response/result from Webservice.

### Building the Application
1) Set intellij to use java 11 for the project modals and sdk
2) Run ``mvn compile``
3) Make sure ```target/generated-sources/xjc``` folder in included in module path for
```adobe-common-ws-models ```, and ```lifecycle-gateway-models ```


### Running the application
Option A) Intellij
1) Create intellij run configuration from jag-adobe-common-service Application
2) Set env variables. See the .env-template
3) Run the application

Option B) Jar
1) Run ```mvn package```
2) Run ```java -jar ./target/jag-adobe-application.jar```

Option C) Docker
1) Run ```mvn package```
2) Run ```docker build -t acs .``` from root folder to build image
3) Run ```docker docker run -e $ENV_VAR$ acs``` (Note that environment variables need to be set)
* Dockerfile is to create container from Openshift, Dockerfile.local is to create container locally. This is because Openshift terminal root can't be found via '~'.
* To create docker container that runs application, 'known_hosts' is needed, and accessibility of read and write is needed.

Option D) Eclipse
1) Clone the project into a local folder.
2) Import the Maven project using the Maven Project Import Wizard.
3) Set Variables either as Windows/Linux Environmental variables or POM goal Environment Variables:

BASIC_AUTH_PASS

BASIC_AUTH_USER

LIFECYCLE_HOST

SFEG_USERNAME

SPLUNK_HTTP_URL

SPLUNK_TOKEN

SPLUNK_INDEX

NFS_DIR

SFTP_PRIVATE_KEY

SFTP_KNOWN_HOSTS

SFEG_HOST

LIFECYCLE_HOST

4) Create POM goals: clean install, spring-boot:run  (when running locally).

### Pre Commit
1) Do not commit \CRLF use unix line enders
2) Run the linter ```mvn spotless:apply```

### JaCoCo Coverage Report
1) Run ```mvn clean verify```
2) Open ```adobe-code-coverage/target/site/jacoco-aggregate/index.html``` in a browser

### Note
1) spring-starters-bom and spring-sftp-starter are libs branched from ```https://github.com/bcgov/spring-boot-starters```
2) id_rsa.pseudo is merely a pseudo key, used to pass unit test in maven package for git pipeline
