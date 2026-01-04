FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    swi-prolog \
    swi-prolog-java \
    curl \
    && rm -rf /var/lib/apt/lists/*

ENV SWI_HOME_DIR=/usr/lib/swi-prolog
ENV LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/x86_64-linux:$LD_LIBRARY_PATH

WORKDIR /app

# Copy Maven wrapper + pom first for caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Pre-fetch dependencies (optional but speeds rebuilds)
RUN ./mvnw -B -DskipTests dependency:go-offline

# Copy sources and build
COPY src src
RUN ./mvnw -B -DskipTests clean package

EXPOSE 8080

ENTRYPOINT ["java","-Djava.library.path=/usr/lib/swi-prolog/lib/x86_64-linux","-jar","target/recommendationEngine-0.0.1-SNAPSHOT.jar"]
