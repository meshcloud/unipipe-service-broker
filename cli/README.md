# UniPipe CLI

The UniPipe CLI allows service developers to quickly implement CI/CD pipelines
for common service scenarios like

- generating and executing terraform
- sending emails
- debugging service instances

The unipipe cli ships as a single, fully self-contained binary. This allows the
cli to be easily run in any CI/CD systems that supports a bash script job like
Jenkins, Azure DevOps, GitLab or GitHub actions.

The Unipipe CLI enables you to quickly deploy and build services on top of
[Unipipe Service Broker](https://github.com/meshcloud/unipipe-service-broker).
Once you decide to use our
[OSB](https://github.com/meshcloud/unipipe-service-broker), you can easily
create the infrastructure with
`unipipe generate unipipe-service-broker-deployment` subcommand on the
`UniPipe-CLI`.

**Note: unipipe cli is still experimental**, please report any issues you
encounter.

## Commands

```text
Usage:   unipipe
Version: v0.6.4

Description:

  UniPipe CLI - supercharge your GitOps OSB service pipelines

Options:

  -h, --help     - Show this help.
  -V, --version  - Show the version number for this program.

Commands:

  completions          - Generate shell completions.
  list         <repo>  - Lists service instances status stored in a UniPipe OSB git repo.
  show         <repo>  - Shows the state stored service instance stored in a UniPipe OSB git repo.
  transform    <repo>  - Transform service instances stored in a UniPipe OSB git repo using the specified handlers.
  update       <repo>  - Update status of a service instance or binding stored in a UniPipe OSB git repo.
  generate             - Generate useful artifacts for working with UniPipe OSB such as catalogs, transform handlers, CI pipelines and more.
```

## Using unipipe cli in a CI/CD pipeline

This is an example for using `unipipe` cli inside a CI/CD pipeline bash task

```bash
# checkout git
git clone osb-repo

# transform to terraform files
unipipe transform ./osb-repo ./my-handlers.ts

# for each dir
  # run tf apply on all terraform modules
  terraform apply -auto-approve

  unipipe update ./osb-repo $instance-id "provisioning succesfull"
#

git -C osb-repo commit
git -C osb-repo push origin master
```

## Writing a Transform Handler

The `unipipe transform` allows you to transform service instances (stored as
`yaml` files by the unipipe service broker) into arbitrary infrastructure as
code files. These handlers are javascript classes registered for handling a
particular service definition. For a quick start, you can use
`unipipe generate transform-handler` to generate an sample handler file.

A handler file needs to export a registry object of the form:

```javascript
{
  "d90c2b20-1d24-4592-88e7-6ab5eb147925": new MyHandler(),
};
```

A handler gets passed a javascript object representing the entire state of the
service instance as it's stored in the unipipe git repository. Here's the type
definitions that your handler needs to conform to:

```typescript
interface ServiceInstance {
  instance: OsbServiceInstance; // contents of instance.yml
  bindings: ServiceBinding[]; // contents of all bindings/$binding-id/binding.yml
  status: OsbServiceInstanceStatus | null; // contents of status.yml, null if not available
}

export interface InstanceHandler {
  readonly name: string;
  handle(instance: ServiceInstance): Dir | null;
}
```

Because generating a target directory structure of IaC files is so common, the
`handle` function allows handlers to optionally return an object representation
of the desired directory structure. If your handler returns such an object,
unipipe cli will take care of writing out the desired files. This write will
merge with the existing directory structure, i.e. any files already present but
not explicitly specified will be preserved.

```typescript
interface Dir {
  name: string;
  entries: (File | Dir)[];
}

interface File {
  name: string;
  content: string;
}
```

Alternatively you can of course implement your own logic to write directories,
call HTTP APIs etc. using the standard Deno APIs.

Here's a full example for a `handlers.js` file that you can pass to
`unipipe transform --registry-of-handlers` and that writes service instance
parameters into a custom directory structure:

```text
/tmp/git-tests.oFi6F1/git-tests.WgUs9M/repo.fp5ZEK
  `-- customers
      `-- unipipe-osb-dev
          `-- osb-dev
              `-- params.json
```

```javascript
class MyHandler {
  name = "My Handler";

  handle(service) {
    const params = service.instance.parameters;

    const context = service.instance.context;

    return {
      name: "customers",
      entries: [{
        name: context.customer_id,
        entries: [
          {
            name: context.project_id,
            entries: [
              { name: "params.json", content: JSON.stringify(params, null, 2) },
            ],
          },
        ],
      }],
    };
  }
}

const handlers = {
  "d90c2b20-1d24-4592-88e7-6ab5eb147925": new MyHandler(),
};

handlers;
```

## Architecture

The unipipe cli is built on [deno](https://deno.land) (tl;dr: modern node.js)
using typescript. This provides the following benefits:

- service owners can extend CLI behavior quickly (e.g. for templating IaC files)
  using javascript
- easy to build single binary, fully-self contained executables via
  [deno compile](https://deno.land/manual@v1.8.3/tools/compiler)
- leveraging a generally mature ecosystem (ts/js, libraries, tooling) that is
  easy to pick up for contributors

## Development

You need [deno](https://deno.land) installed on your system.

```bash
# running locally (using deno run, not compiled)
./unipipe/unipipe.sh --help

# build binary
./build.sh

cd test
# running e2e tests (using unipipe.sh, good for development)
 ./all.sh
# running e2e tests (using the release binary, good for release testing)
./all.sh bin/unipipe
```
