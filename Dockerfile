FROM maven:3.8.3-openjdk-17 AS build

WORKDIR /libs

RUN git clone https://github.com/bcgov/spring-boot-starters.git

WORKDIR /libs/spring-boot-starters

RUN git checkout v1.0.0  && \
    mvn install -P all -f src/pom.xml

WORKDIR /

COPY . .

RUN mvn -B clean install

FROM eclipse-temurin:17-jre-jammy

COPY --from=build ./adobe-common-application/target/jag-adobe-application.jar /app/service.jar

RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

RUN mkdir temp-pdfs
RUN chgrp -R 0 temp-pdfs && \
        chmod -R g=u temp-pdfs

ENTRYPOINT ["java","-Xmx1g","-jar","/app/service.jar"]
