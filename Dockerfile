FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
ADD ./init.sh .
RUN chmod +x init.sh
RUN mkdir .ssh
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh
ENTRYPOINT ["./init.sh"]
