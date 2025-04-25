# This docker file builds unipipe-service-broker

# It is required to run this from the root of the repository as we need files from all over the repository
# In order to make builds faster (by reducing build context), you can use the selective docker ignore feature (details here https://stackoverflow.com/questions/40904409/how-to-specify-different-dockerignore-files-for-different-builds-in-the-same-pr)
# For this to work you will need the following
#   - at least docker 19.03
#   - docker buildkit enabled (export DOCKER_BUILDKIT=1)

FROM eclipse-temurin:17-jdk AS builder

COPY ./ /build

WORKDIR /build

RUN ./gradlew :bootJar -x test

FROM eclipse-temurin:17-jre
ARG UID=1005

COPY --chown=0:0 --chmod=555 --from=builder /build/build/libs/unipipe-service-broker-1.0.0.jar /app/

WORKDIR /app

RUN useradd -u $UID -ms /bin/bash unipipe && \
    chown $UID /app

USER $UID

EXPOSE 8075

ENTRYPOINT [ "/app/unipipe-service-broker-1.0.0.jar" ]
