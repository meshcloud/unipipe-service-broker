# Introduction

This docker container contains the [UniPipe CLI](https://github.com/meshcloud/unipipe-service-broker/tree/main/cli) 
and schedules `unipipe terraform` every minute via a cron job. It can be used in combination with the 
[UniPipe Service Broker](https://github.com/meshcloud/unipipe-service-broker) to actually process the service instances 
that were written by the UniPipe Service Broker.

# How to run

At first you have to create an `env.list` and set some mandatory environment variables for the docker container.

```
GIT_USER_EMAIL=unipipe-terraform@meshcloud.io
GIT_USER_NAME=UniPipe Terraform Runner
GIT_REMOTE=<git repository ssh url>
TF_VAR_platform_secret=<secret of the service principal to create resources via terraform in cloud platform>
```

If your Terraform module is using a backend via azurerm, you have to add the following environment variables to the `env.list` file.
These variables cannot be set directly in the Terraform modules. Therefore they have to be provided as environment variables.

```
ARM_TENANT_ID=<azure tenant id for backend storage> 
ARM_SUBSCRIPTION_ID=<azure subscription id for backend storage> 
ARM_CLIENT_ID=<service principal with access to backend storage> 
ARM_CLIENT_SECRET=<secret of the service principal>
```

You also need an SSH key to access the git repository. This ssh-key will be submitted separately in the docker run command
as you can see in the following:

```
docker pull ghcr.io/meshcloud/unipipe-terraform-runner:latest
docker run -t -i -e GIT_SSH_KEY="$(<my-ssh-key-file)" --env-file env.list ghcr.io/meshcloud/unipipe-terraform-runner:latest
```

By default terraform-runner trusts GitHub's public SSH keys.
If you work with a remote repo that is not hosted on GitHub, you need to set the KNOWN_HOSTS environment variable.

```
docker run -it \
-e GIT_SSH_KEY="$(<my-ssh-key-file)" \
-e KNOWN_HOSTS="$(<my-known-hosts-file)" \
--env-file env.list \
ghcr.io/meshcloud/unipipe-terraform-runner:latest
```
