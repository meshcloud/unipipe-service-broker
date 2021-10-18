##########################
## Alpine based OPENSSH with Bash and GIT ##
##########################
FROM alpine
MAINTAINER stomm

RUN apk --update --no-cache add openssh curl wget bash git && \
    adduser user -h /data/ -s /bin/bash -D

VOLUME ["/data/"]
VOLUME ["/etc/ssh/"]

EXPOSE 22

ENTRYPOINT  ["/bin/bash"]
CMD  ["start"]