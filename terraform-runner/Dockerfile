FROM hashicorp/terraform:1.3.2

ENV GLIBC_REPO=https://github.com/sgerrand/alpine-pkg-glibc
ENV GLIBC_VERSION=2.30-r0

# we need glibc for unipipe cli
RUN set -ex && \
    apk --update add libstdc++ curl ca-certificates && \
    for pkg in glibc-${GLIBC_VERSION} glibc-bin-${GLIBC_VERSION}; \
        do curl -sSL ${GLIBC_REPO}/releases/download/${GLIBC_VERSION}/${pkg}.apk -o /tmp/${pkg}.apk; done && \
    apk add --allow-untrusted /tmp/*.apk && \
    rm -v /tmp/*.apk && \
    /usr/glibc-compat/sbin/ldconfig /lib /usr/glibc-compat/lib

RUN apk add --no-cache curl bash sudo
RUN curl -sf -L https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/install.sh | sudo bash

COPY unipipe-terraform-cron /etc/cron.d/unipipe-terraform-cron
RUN chmod 0644 /etc/cron.d/unipipe-terraform-cron
RUN crontab /etc/cron.d/unipipe-terraform-cron

RUN mkdir ~/unipipe
COPY run-unipipe-terraform.sh /root/unipipe/run-unipipe-terraform.sh
RUN chmod 0755 ~/unipipe/run-unipipe-terraform.sh

RUN mkdir ~/.ssh
RUN ssh-keyscan github.com >> ~/.ssh/known_hosts

COPY entry.sh /root/unipipe/entry.sh
RUN chmod 0755 /root/unipipe/entry.sh

CMD ["/usr/sbin/crond", "-f", "-l", "8"]
ENTRYPOINT ["/root/unipipe/entry.sh"]