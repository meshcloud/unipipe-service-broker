FROM ubuntu:jammy

RUN apt-get update && \
  apt-get install -y --no-install-recommends curl jq cron ca-certificates git ssh && \
  rm -rf /var/lib/apt/lists/*

# terraform
COPY --from=hashicorp/terraform:1.3.10 /bin/terraform /usr/local/bin/terraform

# unipipe cli
RUN curl https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/main/cli/install.sh | bash

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

CMD ["cron", "-f", "-l", "8"]
ENTRYPOINT ["/root/unipipe/entry.sh"]
