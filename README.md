# UniPipe Service Broker

UniPipe service broker connects CI/CD pipelines with platforms that speak
[OSB API](https://www.openservicebrokerapi.org/).

UniPipe service broker stores information about instances and
bindings in a git repository. Services will be managed via a
CI/CD pipeline that tracks the git repository. This decoupling allows using
UniPipe Service Broker with any CI/CD pipeline of your choice.

![A marketplace that integrates with a Service Broker](assets/OSB-Graphic-03.png "Service Broker interactions")

The `unipipe` cli is the swiss army knife in the UniPipe universe. Use it to:
- [Deploy UniPipe Service Broker on Azure](#how-to-deploy-unipipe-service-broker-on-azure-with-terraform)
- [Create a service catalog](#tutorial--your-first-service-catalog-for-unipipe-service-broker)
- [Transform instance.yml files](#tutorial--working-with-instance.yml-files-using-unipipe-transform)

# How to install unipipe cli

Run the command below depending on your operating system.

## Binary distribution

Check the content of the file to be sure that the install script is safe.

**Linux**

```
curl -sf -L https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/install.sh | sudo bash
```

**Mac OS X**

```
curl -sf -L https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/install.sh | sh
```

## Deno install

If you already have a [deno runtime](https://deno.land/#installation) installed on your system, you can install and upgrade unipipe cli comfortably via:

```bash
deno install --unstable --allow-read --allow-write --allow-env --allow-net --allow-run https://raw.githubusercontent.com/meshcloud/unipipe-service-broker/master/cli/unipipe/main.ts
```

Use the builtin `unipipe upgrade` command to switch to a particular official version of the cli.


# How to deploy UniPipe Service Broker on Azure with terraform

Use `unipipe` cli to generate a deployment.
```sh
unipipe generate unipipe-service-broker-deployment --deployment aci_tf --destination .
```
Open the resulting file and follow the instructions at the top.

When you are done, apply terraform.
```sh
terraform apply
```
The terraform output contains an SSH public key. The Service Broker uses the SSH key to access the git instance repository.
Grant write access on the git instance repository to this SSH key. 
- For a git repository hosted on GitHub, add the SSH key as a "Deploy Key". Follow the [official GitHub Docs](https://docs.github.com/en/developers/overview/managing-deploy-keys#setup-2) for this.
- For a git repository hosted on GitLab, add the SSH key as a "Deploy Key". Follow the [official GitLab Docs](https://docs.gitlab.com/ee/user/project/deploy_keys/#how-to-enable-deploy-keys) for this.
- For a git repository hosted on a different platform, consult the documentation of the platform provider.

# Tutorial: Your first service catalog for UniPipe Service Broker
After you have [deployed UniPipe Service Broker](#how-to-deploy-unipipe-service-broker-on-azure-with-terraform), it's time to define a [catalog.yml](#catalog-yml) that UniPipe can advertise to Platforms.

We are going to use an example file provided by `unipipe generate` to generate a catalog with an example service offering.

Open a terminal and navigate into the instance repository. Use unipipe cli to generate an example catalog:
```sh
# UniPipe Service Broker expects catalog.yml in root of the instance repository
unipipe generate catalog --destination .
```

The service broker fetches the `catalog.yml` and makes it available under `https://<service-broker-url>/v2/catalog`. To test this, simply open `https://<service-broker-url>/v2/catalog` with your browser and enter the basic auth credentials that are configured for your service broker.

Well done! With this setup you can advertise your service to any platform that speaks Open Service Broker API. 

Register your service broker with a platform of your choice to explore your catalog on the platform.
- [meshstack](https://docs.meshcloud.io/docs/meshstack.meshmarketplace.development.html#register-your-service-broker)
- [Cloud Foundry](https://docs.cloudfoundry.org/services/managing-service-brokers.html#register-broker)
- [Kubernetes](https://kubernetes.io/docs/concepts/extend-kubernetes/service-catalog/)

Once you order your first instance, an [instance.yml](#instance-yml) file will appear in the instance git repository. This is the file that your CI/CD pipeline will get all information it needs to manage the service instance.

> In the next tutorial, you will learn how `unipipe transform` allows you to process [instance.yml](#instance-yml) files.

# Tutorial: Working with instance.yml files using unipipe transform
After you have [created your first catalog](#tutorial--your-first-service-catalog-for-unipipe-service-broker) and ordered a first instance, it's time to figure out how the CI/CD pipeline will work with [instance.yml](#instance-yml) files.

We are going to use an example file provided by `unipipe generate` to generate a registry of handlers that we then use in `unipipe transform` on our example service offering. The advantage here is, that we can later use the same commands in our CI/CD pipeline to manage new instances without parsing YAML.

A handler is a typescript function that describes an output path and format for a service instance. Because [catalog.yml](#catalog-yml) holds a list of services and not every service might need the same transformation, we need to specify which handler is responsible for which service definition id. 

Let's find out what service definition id we have in our catalog. If you have [yq](https://github.com/mikefarah/yq) installed you can run `cat catalog.yml | yq '.services[].id'` to get a list of all service definition ids in your catalog.yml. Alternatively, do `cat catalog.yml` and check the `id` fields in the services list.

```sh
# Example output. Your result will differ, because `unipipe generate` uses randomly generated UUIDs for every generated catalog.
$ cat catalog.yml | yq '.services[].id'

"07555200-7f39-4125-81d3-7dd17a4bc38e"
"fa262d12-8b6b-4a45-b07a-27410cd87f9b"
"e0d19ebe-0fad-423b-a7ae-46f54b56ae84"
```

Note down one of these ids for the following steps.

Use unipipe cli to generate a handler for this id.
```sh
$ unipipe generate transform-handler --handler handler_b --destination .
# Put in the id for your service definition id
? Write the Service Id that will be controlled by the handler. Press enter to continue. (Default: Auto-generated UUID) › 07555200-7f39-4125-81d3-7dd17a4bc38e
```

The generated handler produces a file under `customers/$customer_id/$project_id/params.json` that has the same context as the instance.yml file, but in JSON format.

Try to apply the handler by running
```sh
unipipe transform --registry-of-handlers handlers.js .
```

If any instances are found that match the service definition id, the above command will create new files according to what's defined in `handlers.js`.

Good job! You are now ready to jump into developing your first UniPipe Service Broker pipeline.

# Tutorial: Your first UniPipe Service Broker pipeline using GitHub actions

We are going to use example files provided by `unipip generate` to generate a GitHub actions pipeline that acts on commits by UniPipe.

Define a pipeline that acts on any commit
```sh
# GitHub expects workflow definitions in .github/workflows/
unipipe generate github-workflow --destination .github/workflows/
```

## How to run UniPipe Service Broker using Docker

We publish unipipe-service-broker container images to GitHub Container Registry
[here](https://github.com/orgs/meshcloud/packages/container/unipipe-service-broker/versions).
These images are built on GitHub actions and are available publicly

```bash
docker pull ghcr.io/meshcloud/unipipe-service-broker:latest
```

Run the container with configuration passed as environment variables. 
> Environment variables are described in the [Configuration Reference](#configuration-reference).

> Note: We used to publish old versions of unipipe-service-broker as GitHub packages
> (not GHCR) which unfortunately can't be deleted from GitHub. Please make sure
> to use `ghcr.io` to pull the latest versions and not the legacy
> `docker.pkg.github.com` URLs.

> Note: You should attach a persistent volume to the image to make sure the
> changes to the local git repository are persisted in case the application
> terminates unexpectedly or restarts.

## How to deploy UniPipe Service Broker to Cloud Foundry

Configure a Manifest file like this:

```yaml
applications:
- name: unipipe-service-broker
  memory: 1024M
  path: build/libs/unipipe-service-broker-0.9.0.jar
  env:
    GIT_REMOTE: <https or ssh url for remote git repo>
    GIT_USERNAME: <if you use https, enter username here>
    GIT_PASSWORD: <if you use https, enter password here>
    APP_BASIC-AUTH-USERNAME: <the username for securing the OSB API itself>
    APP_BASIC-AUTH-PASSWORD: <the password for securing the OSB API itself>
```
> Environment variables are described in the [Configuration Reference](#configuration-reference).
Build and deploy using the manifest file from above:

```sh
# Build the jar
./gradlew build 
# Deploy it to CF
cf push -f cf-manifest.yml 
```

## Interface Reference

UniPipe Service broker translates OSB API communication into committed files in a git repo.
To automate CRUD operations on Service Instances and Bindings, you can to build a CI/CD pipeline that acts upon commits to this git repo.

Example implementations of pipelines are found in the `example/` directory. A complete instance repository of a running UniPipe Service Broker instance can be found under (meshcloud/unipipe-demo)[https://github.com/meshcloud/unipipe-demo].

Below is the reference documentation for ways to interact with UniPipe Service Broker.

### Git commit messages

Git commit messages by UniPipe service broker will always start with `OSB API:`.
UniPipe service broker writes the following git commit messages, depending on the event:

- Service Instance creation commits:
  `OSB API: Created Service instance {ServiceInstanceId}`
- Service Instance deletion commits:
  `OSB API: Marked Service instance {ServiceInstanceId} as deleted.`
- Service Binding creation commits:
  `OSB API: Created Service binding {ServiceBindingID}`
- Service Binding deletion commits:
  `OSB API: Marked Service binding ${ServiceBindingID} as deleted.`

> Reading git commits of UniPipe service broker is useful for registering that some service or service binding is (de)provisioned.

### Instance Git Repo structure

The repository that stores the state of instances, bindings and the catalog is called instance git repository.
UniPipe service broker expects the following structure:

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

> Use unipipe cli to parse an instance repository: `unipipe list --help`

### catalog.yml

The list of provided services and their plans has to be defined in this file.
The following is an example for a catalog file:

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

> Use unipipe cli to generate a more elaborate example file: `unipipe generate catalog` 

### instance.yml

The YAML structure is based on the OSB Spec (see
[expected_instance.yml](src/test/resources/expected_instance.yml)). You can use
all information provided in this file in your CI/CD pipeline. The most essential
properties that will be used in all service brokers are `planId` and `deleted`.
The `deleted` property is the one that indicates to the pipeline, that this
instance shall be deleted.

> Use unipipe cli to transform instance.yml files into file formats that match your needs: `unipipe transform --help`

### binding.yml

It is basically the same as the instance.yml, but on binding level. For an
example file, see
[expected_binding.yml](src/test/resources/expected_binding.yml).

### status.yml

This file contains the current status information and looks like this:

```yaml
status: "in progress"
description: "Provisioning service instance"
```

The pipeline has to update this file. When the instance or binding has been
processed successfully, the status must be updated to `succeeded`. In case of a
failure, it must be set to `failed`. While the pipeline is still working, it
might update the description, to give the user some more information about the
progress of his request.

> Use unipipe cli to write status.yml files: `unipipe update --help`

## About UniPipe Service Broker Implementation

This implementation is based on Spring's
[Spring Cloud Open Service Broker](https://spring.io/projects/spring-cloud-open-service-broker).
This library provides all controllers necessary to implement the OSB API. You
only have to implement specific services and can focus on the actual
implementation of your Service Broker and no details of how the API must look
like exactly. The according request and response objects are provided by the
library.

# Configuration Reference

UniPipe service broker reads the following environment variables.

- `GIT_REMOTE`: The remote Git repository to push the repo to
- `GIT_REMOTE-BRANCH`: The used branch of the remote Git repository
- `GIT_LOCAL-PATH`: The path where the local Git Repo shall be created/used.
  Defaults to tmp/git
- `GIT_SSH-KEY`: If you want to use SSH, this is the PEM encoded RSA SSH key to
  be used for accessing the remote repo. Linebreaks must be replaced with
  spaces.
- `GIT_USERNAME`: If you use HTTPS to access the git repo, define the HTTPS
  username here
- `GIT_PASSWORD`: If you use HTTPS to access the git repo, define the HTTPS
  password here
- `APP_BASIC-AUTH-USERNAME`: The service broker API itself is secured via HTTP
  Basic Auth. Define the username for this here.
- `APP_BASIC-AUTH-PASSWORD`: Define the basic auth password for requests against
  the API

The expected format for the `GIT_SSH-KEY` variable looks like this:

```text
GIT_SSH-KEY=-----BEGIN RSA PRIVATE KEY----- Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG x9tD2aI3...ysKQfmAnDBdG4= -----END RSA PRIVATE KEY-----
```

There is a space `` between `-----BEGIN RSA PRIVATE KEY-----` and
the key as well as between the key and `----END RSA PRIVATE KEY-----`. If you
omit these spaces unipipe will not be able to read the private key. Please also
note that the RSA key is PEM encoded and therefore starts with
`-----BEGIN RSA PRIVATE KEY-----`. OpenSSH encoded keys starting with
`-----BEGIN OPENSSH PRIVATE KEY-----` must be converted to PEM before being
used.

