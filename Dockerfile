FROM eclipse-temurin:17-jre-alpine

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

RUN mkdir temp-pdfs
RUN chgrp -R 0 temp-pdfs && \
        chmod -R g=u temp-pdfs

ENTRYPOINT ["java","-Xmx1g","-jar","jag-adobe-application.jar"]
