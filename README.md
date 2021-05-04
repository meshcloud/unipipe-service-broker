# UniPipe Service Broker

A universal service broker to connect CI/CD pipelines with platforms that speak [OSB API](https://www.openservicebrokerapi.org/).

This project implements the OSB API and stores information about instances and bindings in a git repository.
The services will be created in a later step via a CI/CD pipeline that tracks the git repository.
This decoupling allows using UniPipe Service Broker with any CI/CD piepline of your choice that will do the actual provisioning.

![A marketplace that integrates with a Service Broker](assets/OSB-Graphic-03.png "Service Broker interactions")

UniPipe Service Broker was formerly known as generic OSB API.

## Configuration

The custom configuration can be done via environment variables. The following properties can be configured:

- `GIT_REMOTE`: The remote Git repository to push the repo to
- `GIT_REMOTE-BRANCH`: The used branch of the remote Git repository
- `GIT_LOCAL-PATH`: The path where the local Git Repo shall be created/used. Defaults to tmp/git
- `GIT_SSH-KEY`: If you want to use SSH, this is the PEM encoded RSA SSH key to be used for accessing the remote repo. Linebreaks must be replaced with spaces.
- `GIT_USERNAME`: If you use HTTPS to access the git repo, define the HTTPS username here
- `GIT_PASSWORD`: If you use HTTPS to access the git repo, define the HTTPS password here
- `APP_BASIC-AUTH-USERNAME`: The service broker API itself is secured via HTTP Basic Auth. Define the username for this here.
- `APP_BASIC-AUTH-PASSWORD`: Define the basic auth password for requests against the API

The expected format for the `GIT_SSH-KEY` variable looks like this:

```text
GIT_SSH-KEY=-----BEGIN RSA PRIVATE KEY----- Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG x9tD2aI3...ysKQfmAnDBdG4= -----END RSA PRIVATE KEY-----
```

Please note there is a space ` ` between `-----BEGIN RSA PRIVATE KEY-----` and the key as well as between the key and `----END RSA PRIVATE KEY-----`.
If you omit these spaces unipipe will not be able to read the private key. 
Please also note that the RSA key is PEM encoded and therefore starts with  `-----BEGIN RSA PRIVATE KEY-----`. 
OpenSSH encoded keys starting with `-----BEGIN OPENSSH PRIVATE KEY-----` must be converted to PEM before being used.

## Configuration using dhall

Prerequisite:

- Install dhall and dhall-to-yaml, you can find details [here](https://docs.dhall-lang.org/tutorials/Getting-started_Generate-JSON-or-YAML.html#installation).

The properties mentioned in the configuration section above can be configured in the file 
`dsl/config/AppConfig.dhall`

Example configuration to access git through ssh

```dhall
let Config = ./AppSchema.dhall
let GitAccess = ./GitAccess.dhall
let ExampleSsh 
    : Config
    = {
        app = {basic-auth-username = "username", basic-auth-password ="password"}
        , git = GitAccess.SSH {local = "local path", remote = "remote path", ssh-key = "----"}
        , server.port = 8075
    }
in ExampleSsh

```

Example configuration to access git through https

```dhall
let Config = ./AppSchema.dhall
let GitAccess = ./GitAccess.dhall
let ExampleHttps 
    : Config
    = {
        app = {basic-auth-username = "user", basic-auth-password ="password"}
        , git = GitAccess.HTTPS {local = "local path"
                                 , remote = "remote path"
                                 , username = "usernamr"
                                 , password = "password"}
        , server.port = 8075
    }
in ExampleHttps
```

After configuring the properties, execute the shell script `dsl/config/run.sh`. This will create an `application-default.yml` file in the resources folder of source.

```bash
./run.sh
```

## Deployment using Docker

We publish generic-osb-api container images to GitHub Container Registry [here](https://github.com/orgs/meshcloud/packages/container/unipipe-service-broker/versions). These images are built on GitHub actions and are available publicly

```bash
docker pull ghcr.io/meshcloud/unipipe-service-broker:v1.0.6
```

> Note: We used to publish old versions of generic-osb-api as GitHub packages (not GHCR) which unfortunately can't be deleted from GitHub. Please make sure to use `ghcr.io` to pull the latest versions and not the legacy `docker.pkg.github.com` URLs.

> Note: You should attach a persistent volume to the image to make sure the changes to the local git repository are persisted in case the application terminates unexpectedly or restarts.

## Deployment to Cloud Foundry

To deploy UniPipe Service Broker to Cloud Foundry, you have to use a configured Manifest file like this:

```yaml
applications:
- name: generic-osb-api
  memory: 1024M
  path: build/libs/generic-osb-api-0.9.0.jar
  env:
    GIT_REMOTE: <https or ssh url for remote git repo>
    GIT_USERNAME: <if you use https, enter username here>
    GIT_PASSWORD: <if you use https, enter password here>
    APP_BASIC-AUTH-USERNAME: <the username for securing the OSB API itself>
    APP_BASIC-AUTH-PASSWORD: <the password for securing the OSB API itself>
```

Build and deploy using the manifest file from above:

```sh
./gradlew build # build the jar of UniPipe Service Broker
cf push -f cf-manifest.yml # deploy it to CF
```

## Communication with the CI/CD pipeline

As the OSB API is completely provided by UniPipe Service Broker, what you as a developer of a service broker have to focus
on is to build your CI/CD pipeline. In the following, all files that are used for communication and how the git repository
for exchanging information between UniPipe Service Broker and the pipeline are described. An example pipeline can be found
[here](https://github.com/Meshcloud/example-osb-ci).

### OSB commit messages
In some cases your CI/CD needs to read the git commits of the unipipe service broker to register that some service or service binding is (de)provisioned.
The unipipe service broker will always commit the following messages:
Git commit will always start with `OSB API:`
- Service Instance creation commits: `OSB API: Created Service instance {ServiceInstanceId}`
- Service Instance deletion commits: `OSB API: Marked Service instance {ServiceInstanceId} as deleted.`
- Service Binding creation commits: `OSB API: Created Service binding {ServiceBindingID}`
- Service Binding deletion commits: `OSB API: Marked Service binding ${ServiceBindingID} as deleted.`

### GIT Repo structure

```yaml
catalog.yml           # file that contains all infos about services and plans this Service broker provides
instances
    <instance-id>
        instance.yml  # contains all service instance info written by the 
        status.yml    # this file contains the current status, which is updated by the pipeline
        bindings
            <binding-id>
                binding.yml # contains all binding info written by the UniPipe Service Broker
                status.yml  # this file contains the current status, which is updated by the pipeline
```

### catalog.yml

The list of provided services and their plans has to be defined in this file, which might look as follows.

```yaml
services:
  - id: "d40133dd-8373-4c25-8014-fde98f38a728"
    name: "example-osb"
    description: "This service spins up a host with OpenStack and Cloud Foundry CLI installed."
    bindable: true
    tags:
      - "example"
    plans:
      - id: "a13edcdf-eb54-44d3-8902-8f24d5acb07e"
        name: "S"
        description: "A small host with OpenStack and CloudFoundry CLI installed"
        free: true
        bindable: true
      - id: "b387b010-c002-4eab-8902-3851694ef7ba"
        name: "M"
        description: "A medium host with OpenStack and CloudFoundry CLI installed"
        free: true
        bindable: true
```

### instance.yml

The YAML structure is based on the OSB Spec (see [expected_instance.yml](src/test/resources/expected_instance.yml)).
You can use all information provided in this file in your CI/CD pipeline. The most essential properties that will be
used in all service brokers are `planId` and `deleted`. The `deleted` property is the one that indicates to the pipeline,
that this instance shall be deleted.

### binding.yml

It is basically the same as the instance.yml, but on binding level. For an example file, see
[expected_binding.yml](src/test/resources/expected_binding.yml).

### status.yml

This file contains the current status information and looks like this:

```yaml
status: "in progress"
description: "Provisioning service instance"
```

The pipeline has to update this file. When the instance or binding has been processed successfully, the status must be
updated to `succeeded`. In case of a failure, it must be set to `failed`. While the pipeline is still working, it might
update the description, to give the user some more information about the progress of his request.

## Implementation

If you want to contribute to this project, or create your own service broker based on this implementation
you can find some details about the implementation here.

This implementation is based on Spring's [Spring Cloud Open Service Broker](https://spring.io/projects/spring-cloud-open-service-broker).
This library provides all controllers necessary to implement the OSB API. You only have to implement specific services
and can focus on the actual implementation of your Service Broker and no details of how the API must look like exactly.
The according request and response objects are provided by the library.
