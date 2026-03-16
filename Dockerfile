# FROM mozilla/sbt:latest AS BUILDER
# 
# WORKDIR /app 
# 
# COPY ./build.sbt build.sbt
# 
# RUN sbt install
# 
# COPY ./custom custom
# COPY ./public public
# COPY ./resources resources
# COPY ./conf conf
# COPY ./project project
# COPY ./test test
# COPY ./app app
# 
# RUN sbt stage

FROM eclipse-temurin:25-jre AS cacerts-source

# Stage 2: actual application image
FROM openjdk:8u342-jdk

WORKDIR /app
ENV _JAVA_OPTS=""
ENV PORT=80
ENV CONFIG_FILE=/app/conf/local.conf

# Copy the updated cacerts from the modern JRE image
COPY --from=cacerts-source /opt/java/openjdk/lib/security/cacerts \
     /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts

COPY ./target/universal/stage /app
CMD /app/bin/with -Dhttp.address=0.0.0.0 -Dhttp.port=$PORT -Dconfig.file=$CONFIG_FILE
