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

FROM openjdk:8-slim-buster

WORKDIR /app

ENV _JAVA_OPTS=""
ENV PORT=80
ENV CONFIG_FILE=/app/conf/local.conf

COPY .github/files/GEANT_OV_RSA_CA_4.crt /etc/pki/GEANT.crt
RUN keytool -importcert -noprompt -trustcacerts -alias GEANT_OV_RSA_CA_4 -storepass changeit -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -file /etc/pki/GEANT.crt
COPY ./target/universal/stage /app


CMD /app/bin/with -Dhttp.address=0.0.0.0 -Dhttp.port=$PORT -Dconfig.file=$CONFIG_FILE
