#!/usr/bin/env bash

set -e

source $(dirname $0)/helpers.sh
 
it_can_update() {
  local repo_osb=$(init_repo_osb)
  
  unipipe update -i "49c96fa5-46cc-4334-a718-378ceed2de81" --status "failed" --description "all is fine!" "$repo_osb"

  local yml=$(cat "$repo_osb"/instances/49c96fa5-46cc-4334-a718-378ceed2de81/status.yml)

  assert_eq "$(echo "$yml" | grep status)" "status: failed" "status not updated"
  assert_eq "$(echo "$yml" | grep description)" "description: all is fine!" "description not updated"
}

run it_can_update