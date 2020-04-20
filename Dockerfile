FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y curl

EXPOSE 8080

WORKDIR /home/grundstuecksinformation

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /home/grundstuecksinformation/app/lib
COPY ${DEPENDENCY}/META-INF /home/grundstuecksinformation/app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /home/grundstuecksinformation/app
RUN chown -R 1001:0 /home/grundstuecksinformation && \
    chmod -R g=u /home/grundstuecksinformation

USER 1001

ENTRYPOINT ["java","-XX:MaxRAMPercentage=80.0","-Djava.security.egd=file:/dev/./urandom","-jar","/home/grundstuecksinformation/app.jar"]

HEALTHCHECK --interval=30s --timeout=30s --start-period=60s CMD curl http://localhost:8080/actuator/health