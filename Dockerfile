FROM nexus-docker-registry.apps.cicd2.mdtu-ddm.projects.epam.com/mdtu-ddm-edp-cicd/openjdk:11.0.16-jre-slim AS builder
WORKDIR /application
ARG JAR_FILE=target/excerpt-service-api-*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM nexus-docker-registry.apps.cicd2.mdtu-ddm.projects.epam.com/mdtu-ddm-edp-cicd/openjdk:11.0.16-jre-slim
WORKDIR /application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher ${0} ${@}"]