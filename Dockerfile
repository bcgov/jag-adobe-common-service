FROM registry.access.redhat.com/ubi8/openjdk-11

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar

ENTRYPOINT ["java","-jar","/jag-adobe-application.jar"]
