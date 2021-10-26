# Changelog

## vNext

- generate aci-deployment: use local backend by default in generated terraform

## v0.6.4

- generate execution-script: add an example for execution-script
- generate github-workflow: add an example for github-action workflows
- generate transform-handler: add terraform example into transform-handlers. there are 2 options at the moment ( basic | terraform )
- add parameter support on generate commands. inputs can be passes without a prompt

## v0.6.3

- generate unipipe-service-broker-deployment: add a template for a unipipe-service-broker deployment on Google Cloud Run
- generate unipipe-service-broker-deployment: improve instructions in the generated ACI terraform

## v0.6.2

- upgrade our deno version to "~1.10"
- change generate "unipipe-deployment" as "unipipe-service-broker-deployment"

## v0.6.1

- generate: add tls and random providers support to our generate aci_tf template
- list: show only existing resources on --deleted=false param
- help: execute the help subcommand if there is no other args while executing unipipe command

## v0.6.0

- update: Add support for bindings

## v0.5.0

- transform: rename the `-h/--handler` option to `-r/--registry-of-handlers` to avoid a clash with `-h/--help`
- generate: add a command to `generate` an ACI deployment for unipipe-service-broker
- list: add `--status` and `--deleted` filter options

## v0.4.0

- add support for reading service bindings, they are now available in `show`, `list`and `transform` commands
- `show` command format option now defaults to yaml

## v0.3.0

- upgraded deno runtime to 1.9
- improved robustness of command/option parsing
- added a new `generate` command that can generate commonly needed artifacts for working with unipipe-service-broker like catalog files, uuids. More generatable blueprints to follow soon.

## v0.2.0

- list: added `--output-format` option to select text or json output. JSON output is useful for chaining to other tools like jq.
- list: added `--profile` option to display key OSB API profile fields like when using `output-format text`. Currently supports OSB API profiles for meshcloud meshMarketplace and Cloud Foundry.

## v0.1.0

Initial release.
