FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar

ENTRYPOINT ["java","-jar","/jag-adobe-application.jar"]
