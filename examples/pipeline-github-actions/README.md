# UniPipe Service Broker pipeline using Github Actions

A Github Actions pipeline makes sense, if the repository that stores the service instances and bindings lives on Github.

This example implements the full lifecycle for a Service that manages Azure vnets.

## Set Up the pipeline

1. Copy the file `vnet.yml` to the repository to `.github/workflows/vnet.yml`
2. Copy the directory `network/` to the repository.
3. Copy the file `catalog.yml ` to the repository.
