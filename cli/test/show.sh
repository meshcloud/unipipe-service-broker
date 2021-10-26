#!/usr/bin/env bash

set -e

source $(dirname $0)/helpers.sh

it_can_show_json() {
  local repo_osb=$(init_repo_osb)
  
  unipipe show -o "json" -i "49c96fa5-46cc-4334-a718-378ceed2de81" --pretty "$repo_osb"
}

it_can_show_yaml() {
  local repo_osb=$(init_repo_osb)
  
  unipipe show -o "yaml" -i "49c96fa5-46cc-4334-a718-378ceed2de81" --pretty "$repo_osb"
}

run it_can_show_json
run it_can_show_yaml
