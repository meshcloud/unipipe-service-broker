#!/bin/bash

# Exit on all errors and undefined vars
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset
set -x

doc() {
    cat <<-EOF
Translates the instance.yml to a tfvars file, that can be used by terraform for deployment.
Should be invoked from parent directory of example-osb-ci and the instances repository.
Alternativley using absolute paths also allows execution of this script from any dir.

USAGE:
    ./example-osb-ci/pipeline/scripts/create-tfvars.sh <ci-repository> <instances-repository> <os-image-id> <os_external_network_id> <small_flavor> <medium_flavor> <external_ip_pool> <dns_nameservers>

EXAMPLES:
    ./example-osb-ci/pipeline/scripts/create-tfvars.sh example-osb-ci example-osb-repo 7e72a4b2-8dd4-4a3c-94f9-0ad895b7a622 e41cf977-49a4-4551-adc8-379057acacd7 cb1.small cb1.medium public00 "8.8.8.8"

EOF
}

main(){
  CI_ROOT="$1"
  INSTANCES_ROOT="$2"
  IMAGE_ID="$3"
  EXTERNAL_NETWORK_ID="$4"
  SMALL_FLAVOR="$5"
  MEDIUM_FLAVOR="$6"
  EXTERNAL_IP_POOL="$7"
  DNS_NAMESERVERS="$8"

  for dir in $INSTANCES_ROOT/instances/*
  do
      echo "Generating tfvars for: $dir"
      pub_key="$(cat $dir/ssh-key.pub)"
      plan_id="$(grep 'planId:' $dir/instance.yml | cut -d\  -f2-)"
      # deleted is also written to tfvars to trigger an update of the git resource used in deploy job
      isDeleted="$(grep 'deleted:' $dir/instance.yml | cut -d\  -f2-)"

      if [[ "$plan_id" == "\"a13edcdf-eb54-44d3-8902-8f24d5acb07e\"" ]]
      then
        flavor="$SMALL_FLAVOR"
      elif [[ "$plan_id" == "\"b387b010-c002-4eab-8902-3851694ef7ba\"" ]]
      then
        flavor="$MEDIUM_FLAVOR"
      else
        echo "plan $plan_id not found! I am just doing nothing!"
        ./$CI_ROOT/pipeline/scripts/update-status.sh "$dir" "succeeded" "just did nothing"
        continue
      fi

      if [[ -e "$dir/instance.tfvars" ]]; then
        rm "$dir/instance.tfvars"
      fi
      cat >"$dir/instance.tfvars" <<EOL
service_instance_id="${dir##*/}"
flavor="${flavor}"
public_key="${pub_key}"
image_id="${IMAGE_ID}"
external_network_id="${EXTERNAL_NETWORK_ID}"
external_ip_pool="${EXTERNAL_IP_POOL}"
deleted="${isDeleted}"
dns_nameservers=[${DNS_NAMESERVERS}]
EOL

      if [[ -e "$dir/status.yml" ]]; then
        last_status="$(grep 'status:' $dir/status.yml | cut -d\  -f2-)"
      else
        last_status="\"in progress\""
      fi
      if [[ $last_status == "\"in progress\"" ]]; then
        is_deleted="$(grep 'deleted:' $dir/instance.yml |  cut -d\  -f2-)"
        description="provisioning service"
        if [[ $is_deleted == "true" ]]; then
          description="deprovisioning service"
        fi
        ./$CI_ROOT/pipeline/scripts/update-status.sh "$dir" "in progress" "$description"
      fi
  done
}

if [[ $# == 8 ]]; then
    main "$@"
else
    doc
    exit 1;
fi