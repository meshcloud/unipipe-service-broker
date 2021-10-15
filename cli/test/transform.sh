#!/usr/bin/env bash

set -e

source $(dirname $0)/helpers.sh

# use some trickery to get an absolute path to the registry file
handlers_js="$(cd "$(dirname "$0")"; pwd)"/handlers/registry.js
handlers_ts="$(cd "$(dirname "$0")"; pwd)"/handlers/registry.ts

it_can_transform_using_js() {
  local repo_osb=$(init_repo_osb)
  local repo_tf=$(init_repo)

  unipipe transform --registry-of-handlers "$handlers_js" --xport-repo "$repo_tf" "$repo_osb"

  echo "Output repo TF"
  tree "$repo_tf"

  params_json=$(jq -c < "$repo_tf"/customers/unipipe-osb-dev/osb-dev/params.json)
  assert_eq '{"cidr":"10.0.0.0/24","region":"eu-west"}' "$params_json" "expected params file generated"
}

it_can_transform_using_ts() {
  local unipipe_bin=${UNIPIPE_BIN:-}
  if [ -n "$unipipe_bin" ]; then
    echo "Skipped, not supported when running tests using a compiled unipipe binary."
    return
  fi

  local repo_osb=$(init_repo_osb)
  local repo_tf=$(init_repo)

  # note: tyoescripe registries are experimental and require an explicit allow-net permission that we do not switch
  # on by default
  deno run --allow-read --allow-write --allow-env --allow-net --unstable "$(dirname "$0")"/../unipipe/main.ts transform --registry-of-handlers "$handlers_ts" --xport-repo "$repo_tf" "$repo_osb"

  echo "Output repo TF"
  tree "$repo_tf"

  params_json=$(jq -c < "$repo_tf"/customers/unipipe-osb-dev/osb-dev/params.json)
  assert_eq '{"cidr":"10.0.0.0/24","region":"eu-west"}' "$params_json" "expected params file generated"
}

run it_can_transform_using_js
run it_can_transform_using_ts