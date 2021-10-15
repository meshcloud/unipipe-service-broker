#!/usr/bin/env bash

set -e

source $(dirname $0)/helpers.sh

it_can_show_generate_help() {
  unipipe generate --help
}

it_can_generate_uuid() {
  local uuid=$(unipipe generate uuid)
  assert_eq 36 ${#uuid} "uuid length matches"
}

it_can_generate_catalog() {
  unipipe generate catalog --destination ./output

  # test the generated catalog is valid yaml
  deno run ./parse-yaml.ts -q <<< ./output/catalog.yml
}

it_can_generate_transform-handler() {
  unipipe generate transform-handler --handler handler_b --uuid `unipipe generate uuid` --destination ./output

  # test the generated handler is valid js
  deno run --no-check - <<< "// deno-lint-ignore-file\n "`cat ./output/handler.js`
}

run it_can_show_generate_help
run it_can_generate_uuid
run it_can_generate_catalog
run it_can_generate_transform-handler