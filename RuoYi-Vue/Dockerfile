FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
ARG MAVEN_REPO_URL=https://mirrors.tencent.com/nexus/repository/maven-public/
ARG MAVEN_CLI_OPTS="-B -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Djava.net.preferIPv4Stack=true"
RUN mkdir -p /root/.m2 \
    && printf '%s\n' \
        '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"' \
        '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
        '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">' \
        '  <mirrors>' \
        '    <mirror>' \
        '      <id>maven-build-mirror</id>' \
        '      <name>Maven build mirror</name>' \
        '      <url>'"${MAVEN_REPO_URL}"'</url>' \
        '      <mirrorOf>*</mirrorOf>' \
        '    </mirror>' \
        '  </mirrors>' \
        '</settings>' \
        > /root/.m2/settings.xml
COPY pom.xml .
COPY ruoyi-admin/pom.xml ruoyi-admin/pom.xml
COPY ruoyi-common/pom.xml ruoyi-common/pom.xml
COPY ruoyi-framework/pom.xml ruoyi-framework/pom.xml
COPY ruoyi-generator/pom.xml ruoyi-generator/pom.xml
COPY ruoyi-quartz/pom.xml ruoyi-quartz/pom.xml
COPY ruoyi-system/pom.xml ruoyi-system/pom.xml
RUN mvn ${MAVEN_CLI_OPTS} -Dmaven.repository.url=${MAVEN_REPO_URL} dependency:go-offline
COPY . .
RUN mvn ${MAVEN_CLI_OPTS} -Dmaven.repository.url=${MAVEN_REPO_URL} -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/ruoyi-admin/target/ruoyi-admin.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
