FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
RUN mkdir .ssh
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

RUN mkdir temp-pdfs
RUN chgrp -R 0 temp-pdfs && \
        chmod -R g=u temp-pdfs

ENTRYPOINT ["java","-jar","jag-adobe-application.jar"]
