FROM frolvlad/alpine-glibc:alpine-3.13

RUN apk update
RUN apk add curl bash sudo jq git openssh

# terraform
ARG tfversion=1.3.7
RUN apk add terraform=${tfversion}-r0 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# unipipe cli
RUN curl --silent --fail https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/install.sh | sudo bash

# unipipe terraform cron
COPY unipipe-terraform-cron /etc/cron.d/unipipe-terraform-cron
RUN chmod 0644 /etc/cron.d/unipipe-terraform-cron
RUN crontab /etc/cron.d/unipipe-terraform-cron

RUN mkdir ~/unipipe
COPY run-unipipe-terraform.sh /root/unipipe/run-unipipe-terraform.sh
RUN chmod 0755 ~/unipipe/run-unipipe-terraform.sh

# We add known_hosts entries for GitHub because they can be queried safely and we use them for development
# For other git servers, set the KNOWN_HOSTS environment variable of the container
RUN mkdir ~/.ssh
RUN curl --silent https://api.github.com/meta \
  | jq --raw-output '"github.com "+.ssh_keys[]' >> ~/.ssh/known_hosts

COPY entry.sh /root/unipipe/entry.sh
RUN chmod 0755 /root/unipipe/entry.sh

CMD ["/usr/sbin/crond", "-f", "-l", "8"]
ENTRYPOINT ["/root/unipipe/entry.sh"]
