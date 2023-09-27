# Changelog

## v1.7.8

### CLI
- no changes

### OSB
- no changes

### Terraform Runner
- fix issue with cron invocation

## v1.7.7

### CLI
- no changes

### OSB
- update base docker image

### Terraform Runner
- update base docker image
- update terraform to v1.3.10

## v1.7.6

### CLI
- no changes

### OSB
- fix getServiceInstanceBinding endpoint to return a 404, if binding does not exist

### Terraform Runner
- update terraform to v1.3.7
- env variable KNWON_HOSTS now supports empty values

## v1.7.5

### CLI
- no changes

### OSB
- Fixed a bug that prevented a ServiceInstance update request to get executed if planId was null

### Terraform Runner
- Configure known hosts via environment variable

## v1.7.4

### Terraform Runner
- Configure known hosts via environment variable

## v1.7.3

### CLI
- New command `unipipe git` runs Git pull/push commands resiliently. It takes care of retrying and rebasing if needed to make sure a push will be successful.

### OSB
- no changes

### Terraform Runner
- Improve terraform-runner to use the new `unipipe git` command for Git operations.

## v1.7.2

### CLI
- new command unipipe generate terraform-runner-hello-world will generate a sample catalog + terraform files for use with unipipe terraform command

### Terraform Runner
- Improve runner script robustness
- Abort cron job if Git repo setup fails

### OSB
- no changes

## v1.7.1

### Terraform Runner
- terraform-runner fix for Azure Container Instances

### CLI
- no changes

### OSB
- no changes

## v1.7.0
### CLI

- Fixed zsh completions
- `unipipe terraform` updates status.yml to succeeded for service instances without any binding
- A `UniPipe Terraform Runner` docker container is now available. You can find the versioned containers
[here](https://github.com/meshcloud/unipipe-service-broker/pkgs/container/unipipe-terraform-runner).
It can be configured via a few environment variables and executes `unipipe terraform` every minute
for the configured git repository. It also pulls changes from and pushes updated status.yml files,
etc to the configured git repository. Using this `UniPipe Terraform Runner` together with the
`UniPipe Service Broker` results in a fully functional service broker.
- Fixed mixed up plan and service column in `unipipe list` command. Service names are now shown
in the Service column and plans are shown in the Plan column.
- Added manual parameter input to `unipipe terraform` processing. This can be used if before executing
Terraform for a Service Instance, an operator needs to take some manual action and provide additional
input to the Terraform module. This can be used to e.g. provide an IP range for a service instance of a
networking service, if no IPAM solution is in place. The operator just needs to put a `params.yml` in
the according instance folder. Once this file is available the `unipipe terraform` command will apply Terraform.
Whether a service requires this manual input can be defined in the metadata of a Service Plan in the service catalog.
- Support usage of Terraform Backend for `unipipe terraform` command. If a backend.tf file exists in the service's
terraform folder it is copied to the binding directory where Terraform is executed. No configuration of
the backend.tf can be done. The file will be used as is. In order to separate the different tfstates in the backend,
the `unipipe terraform` command uses Terraform Workspaces. A workspace will be created for every service binding.
Credentials for accessing the backend have to be set via environment variables. If e.g. an azure backend is used,
ARM_CLIENT_ID and ARM_CLIENT_SECRET have to be set.
- `unipipe terraform` now supports the full lifecycle of a service instance. If a service instance or its binding is
deleted, the `unipipe terraform` command applies a `terraform destroy` to remove the instance again.
- It is now possible to add a `--plan` option to `unipipe terraform`, which executes the command basically as a dry-run.
Instead of doing `terraform apply`, a `terraform plan` is executed and the console output shows the result of `terraform plan`.
No status.yml is updated in this case.
- `unipipe terraform` provides the plan_id and plan_name as variables to the Terraform module.

### OSB
- no changes

## v1.6.0
### CLI

- Added a new `unipipe terraform` command to execute Terraform modules easily. For several service brokers execution
of a Terraform Module is the central task they have to execute. The Terraform module must exist in the git repository
that also contains the instances in a terraform/<serviceId> folder. It must be compatible with a specific set of variables
that will be provided to it via the unipipe terraform command. These variables are determined dynamically via the parameters
and bindResource information provided by the UniPipe Service Broker.

### OSB
- no changes

## v1.5.2
### CLI
- no changes

### OSB
- fix: handling x-forward-headers

## v1.5.0

### CLI

- you can now search for service instances and bindings in `unipipe browse`. This greatly enhances the usability of
  the browse mode for large repositories. Also the list now shows a summary info at the bottom how many instances
  where found vs. how many exist in total.
- generate ECDSA key instead of RSA key in templated deployment

## v1.4.0

### Service Broker

No new features

### CLI

- parameter `repo` is now optional for all commands. This is not breaking, because the commands still accept the parameter.
- command `unipipe browse` shows customer and project details if available in context.
- command `unipipe browse` can update binding credentials.
- github pipelines now use setup-github action

## v1.3.0

This release comes with quite a bunch of new features

### Service Broker

- Added support for [metrics-based service metering](https://docs.meshcloud.io/docs/meshstack.meshmarketplace.metrics-metering.html).
  This allows service developers to easily implement usage-based services that charge e.g. by the number of API requests
  served or GiB-h of storage saved. Learn more about metrics-based metering implementation in the [UniPipe wiki](https://github.com/meshcloud/unipipe-service-broker/wiki/Reference#metrics-reference).

### CLI

- add a new `unipipe browse` command to interactively explore and update service instances. This is very useful for "semi-automated" services where operators want to manually update service instance status.
- add a new `unipipe upgrade` command that allows upgrading unipipe-cli versions when unipipe was installed via `deno install`. See [installation instructions](https://github.com/meshcloud/unipipe-service-broker/wiki/How-To-Guides#deno-install)
- Add an official windows installation script for unipipe cli

## v1.2.2

- integrate cli into unipipe-service-broker repository
