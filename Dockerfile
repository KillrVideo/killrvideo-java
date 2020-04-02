FROM openjdk:11

MAINTAINER Cedrick Lunven <cedrick.lunven@datastax.com>
MAINTAINER David Gilardi  <david.gilardi@datastax.com>

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/killrvideo-java.jar"]
EXPOSE 50101

# To create jar file, run `docker run -v ${PWD}:/opt/killrvideo-java -w /opt/killrvideo-java maven mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B`
COPY ./killrvideo-services/target/killrvideo-java.jar /killrvideo-java.jar