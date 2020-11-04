FROM openjdk:latest

ADD target/scala-2.13/ats-poc.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]