# compile source
FROM maven:3.9.8-amazoncorretto-21 as build

WORKDIR /workspace/app

# prepare for build
COPY pom.xml pom.xml
COPY lib lib

# build dependencies in separate layer (for caching purposes)
RUN mvn dependency:go-offline -B

# copy source (this layer is rebuild everytime there are changes to code)
COPY src src

# actually compile
RUN mvn package -DskipTests

# now build deployment image
FROM amazoncorretto:21

VOLUME /tmp
ARG TARGET=/workspace/app/target
COPY --from=build ${TARGET}/medcom-mailbox-1.0.0.jar .

COPY /deploy .
RUN chmod +x run.sh

EXPOSE 8385

ENTRYPOINT ["/bin/bash", "run.sh"]
