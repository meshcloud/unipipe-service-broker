# Introduction

This docker container contains the [UniPipe CLI](https://github.com/meshcloud/unipipe-service-broker/tree/master/cli) 
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

You also need an SSH key to access the git repository. This ssh-key will be submitted separately in the docker run command
as you can see in the following:

```
docker run -t -i -e GIT_SSH_KEY="$(<my-ssh-key-file)" --env-file env.list meshcloud/unipipe-terraform-runner
```
