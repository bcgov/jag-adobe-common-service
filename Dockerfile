FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh
ENTRYPOINT ["java","-jar","jag-adobe-application.jar"]
