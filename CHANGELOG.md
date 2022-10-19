# Changelog

## vNext

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
