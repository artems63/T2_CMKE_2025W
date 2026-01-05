FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    swi-prolog \
    swi-prolog-java \
    curl \
    zip \
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

# Replace Maven JPL 7.4.0 with system JPL 7.6.1 to match native library
# Delete old JPL and add new one with correct path using zip
RUN cd target && \
    zip -q -d recommendationEngine-0.0.1-SNAPSHOT.jar BOOT-INF/lib/jpl-*.jar 2>/dev/null || true && \
    mkdir -p /tmp/jpl_replace/BOOT-INF/lib && \
    cp /usr/share/java/jpl.jar /tmp/jpl_replace/BOOT-INF/lib/jpl.jar && \
    cd /tmp/jpl_replace && \
    zip -0 -q /app/target/recommendationEngine-0.0.1-SNAPSHOT.jar BOOT-INF/lib/jpl.jar && \
    rm -rf /tmp/jpl_replace

EXPOSE 8080

ENTRYPOINT ["java","-Djava.library.path=/usr/lib/swi-prolog/lib/x86_64-linux","-jar","target/recommendationEngine-0.0.1-SNAPSHOT.jar"]
