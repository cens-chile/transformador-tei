FROM maven:3.8-openjdk-17-slim AS build-componente
WORKDIR /tmp/componente

COPY pom.xml .
#COPY server.xml .
RUN mvn -ntp dependency:go-offline

COPY src/ /tmp/componente/src/
RUN mvn clean install -DskipTests -Djdk.lang.Process.launchMechanism=vfork

FROM build-componente AS build-distroless
RUN mvn -DskipTests package spring-boot:repackage -Pboot
RUN mkdir /app && cp /tmp/componente/target/ROOT.war /app/main.war

########### distroless brings focus on security and runs on plain spring boot - this is the default image
FROM gcr.io/distroless/java17:nonroot AS default
COPY --chown=nonroot:nonroot --from=build-distroless /app /app
# 65532 is the nonroot user's uid
# used here instead of the name to allow Kubernetes to easily detect that the container
# is running as a non-root (uid != 0) user.
USER 65532:65532
WORKDIR /app
CMD ["/app/main.war"]
