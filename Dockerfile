FROM openjdk:11-jre-slim

COPY ./adobe-common-application/target/jag-adobe-application.jar jag-adobe-application.jar
ADD ./init.sh .
RUN chmod +x init.sh
RUN RUN chgrp -R 0 / && \
        chmod -R g=u /
ENTRYPOINT ["./init.sh"]
