FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
RUN mkdir ~/.ssh
RUN touch ~/.ssh/known_hosts
RUN chmod +wr ~/.ssh/known_hosts
ENTRYPOINT ["java","-jar","/jag-adobe-application.jar"]
