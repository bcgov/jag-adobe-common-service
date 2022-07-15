FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
RUN mkdir nfs
RUN chgrp -R 0 nfs && \
        chmod -R g=u nfs
ENTRYPOINT ["java","-jar","jag-adobe-application.jar"]
