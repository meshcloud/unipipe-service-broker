# This docker file builds unipipe-service-broker

# It is required to run this from the root of the repository as we need files from all over the repository
# In order to make builds faster (by reducing build context), you can use the selective docker ignore feature (details here https://stackoverflow.com/questions/40904409/how-to-specify-different-dockerignore-files-for-different-builds-in-the-same-pr)
# For this to work you will need the following
#   - at least docker 19.03
#   - docker buildkit enabled (export DOCKER_BUILDKIT=1)

FROM openjdk:11-jdk-slim-buster as builder

COPY ./ /build

WORKDIR /build

RUN ./gradlew :bootJar -x test

FROM openjdk:11-jre-slim-buster

COPY --from=builder /build/build/libs/unipipe-service-broker-1.0.0.jar /app/

WORKDIR /app

ENTRYPOINT [ "/app/unipipe-service-broker-1.0.0.jar" ]
