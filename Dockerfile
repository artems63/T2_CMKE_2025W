FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    swi-prolog \
    swi-prolog-java \
    curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
ENV SWI_HOME_DIR=/usr/lib/swi-prolog
ENV LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/x86_64-linux:$LD_LIBRARY_PATH

WORKDIR /app
COPY target/recommendationEngine-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Djava.library.path=/usr/lib/swi-prolog/lib/x86_64-linux","-jar","app.jar"]