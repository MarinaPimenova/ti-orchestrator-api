FROM alpine/java:21-jre

COPY build/libs/*.jar /app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app.jar"]
