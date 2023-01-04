FROM ubuntu:22.04

RUN apt-get update
RUN apt-get install -y cron curl bash sudo jq git

# Install terraform
RUN apt-get install -y gnupg software-properties-common 
RUN curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
RUN sudo apt-add-repository "deb [arch=$(dpkg --print-architecture)] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
RUN sudo apt update
RUN sudo apt install terraform

# Install unipipe cli
RUN curl -sf -L https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/install.sh | sudo bash

# Set up cronjob
COPY unipipe-terraform-cron /etc/cron.d/unipipe-terraform-cron
RUN chmod 0644 /etc/cron.d/unipipe-terraform-cron
RUN crontab /etc/cron.d/unipipe-terraform-cron

RUN mkdir ~/unipipe
COPY run-unipipe-terraform.sh /root/unipipe/run-unipipe-terraform.sh
RUN chmod 0755 ~/unipipe/run-unipipe-terraform.sh

COPY entry.sh /root/unipipe/entry.sh
RUN chmod 0755 /root/unipipe/entry.sh

# We add known_hosts entries for GitHub because they can be queried safely and we use them for development
# For other git servers, set the KNOWN_HOSTS environment variable of the container
RUN mkdir ~/.ssh
RUN curl --silent https://api.github.com/meta \
  | jq --raw-output '"github.com "+.ssh_keys[]' >> ~/.ssh/known_hosts

CMD ["cron", "-f"]
ENTRYPOINT ["/root/unipipe/entry.sh"]
