# UniPipe Service Broker pipeline using Github Actions

A Github Actions pipeline makes sense, if the repository that stores the service instances and bindings lives on Github.

This example inclues a catalog with an Azure vnet service and implements the full lifecycle for this Service. The pipeline uses UniPipe CLI for working with instance yaml files.

## How to set up the pipeline
You need a Github repository that is r+w accessible for a UniPipe service broker instance.

1. Copy the file `vnet.yml` to the repository to `.github/workflows/vnet.yml`. What is it? The CI/CD pipeline definition that manages requests.
2. Copy the file `catalog.yml ` to the repository. What is it? The catalog contains the service offering advertised by UniPipe service broker.
3. Copy the directory `network/` to the repository. What is it? Helper scripts for transforming UniPipe's instance yml files to terraform files and some bash scripts that are used in the pipeline.