#!/usr/bin/env bash

set -e

source $(dirname $0)/helpers.sh

it_can_list() {
  local repo_osb=$(init_repo_osb)
  
  unipipe list "$repo_osb"
}

it_can_list_with_meshmarketplace_columns() {
  local repo_osb=$(init_repo_osb)
  
  out=$(unipipe list --profile meshmarketplace "$repo_osb")
  echo "$out"
  
  cols=$(echo "$out" | head -n 1 | xargs)

  assert_eq "id customer project service plan status deleted" "$cols" "cols match"
}

it_can_list_with_cloudfoundry_columns() {
  local repo_osb=$(init_repo_osb)
  
  out=$(unipipe list --profile cloudfoundry "$repo_osb")
  echo "$out"

  cols=$(echo "$out" | head -n 1 | xargs)

  assert_eq "id organization space service plan status deleted" "$cols" "cols match"
}

it_can_list_with_unknown_profile() {
  local repo_osb=$(init_repo_osb)

  out=$(unipipe list --profile xx "$repo_osb" 2>&1) || true

  assert_contain "$out" 'Option "--profile" must be of type "profile", but got "xx". Expected values: "meshmarketplace", "cloudfoundry"' "expected illegal argument error"
}

it_can_list_with_output_json() {
  local repo_osb=$(init_repo_osb)
  
  out=$(unipipe list -o json "$repo_osb" | jq -c .[].instance.serviceInstanceId | sort | jq -sc)
  
  assert_eq '["159440dd-652a-4e2b-916e-9cd4dde33c33","49c96fa5-46cc-4334-a718-378ceed2de81","eed1df43-e63c-40ad-995e-e21068254ef9"]' "$out" "expected jq pipe works"
}

it_can_list_with_status_filter() {
  local repo_osb=$(init_repo_osb)
  
  out=$(unipipe list --status "succeeded" -o json "$repo_osb"  | jq -c '[.[]  |{ serviceInstanceId: .instance.serviceInstanceId, status: .status.status}] | sort_by(.serviceInstanceId)')
  
  assert_eq '[{"serviceInstanceId":"159440dd-652a-4e2b-916e-9cd4dde33c33","status":"succeeded"},{"serviceInstanceId":"49c96fa5-46cc-4334-a718-378ceed2de81","status":"succeeded"}]' "$out" "expected jq pipe works"
}

it_can_list_with_deleted_filter() {
  local repo_osb=$(init_repo_osb)
  
  out=$(unipipe list --deleted "true" -o json "$repo_osb"  | jq -c '[.[]  |{ serviceInstanceId: .instance.serviceInstanceId, deleted: .instance.deleted}] | sort_by(.serviceInstanceId)')
  
  assert_eq '[{"serviceInstanceId":"eed1df43-e63c-40ad-995e-e21068254ef9","deleted":true}]' "$out" "expected jq pipe works"
}


run it_can_list
run it_can_list_with_meshmarketplace_columns
run it_can_list_with_cloudfoundry_columns
run it_can_list_with_unknown_profile
run it_can_list_with_output_json
run it_can_list_with_status_filter
run it_can_list_with_deleted_filter