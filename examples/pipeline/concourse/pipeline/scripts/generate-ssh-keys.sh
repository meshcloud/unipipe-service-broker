#!/bin/bash

# Exit on all errors and undefined vars
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

doc() {
    cat <<-EOF
Generates SSH keys for all instances that don't have one yet.
Should be invoked from parent directory of the instances repository.
Alternativley using absolute paths also allows execution of this script from any dir.

USAGE:
    ./example-osb-ci/pipeline/scripts/generate-ssh-keys.sh <instances-repository>

EXAMPLES:
    ./example-osb-ci/pipeline/scripts/generate-ssh-keys.sh example-osb-repo

EOF
}

main(){
    INSTANCES_ROOT="$1"
    for dir in $INSTANCES_ROOT/instances/*
    do
        if [[ ! -e "$dir/ssh-key" ]]; then
            echo "Generating key for: $dir"

            # TODO: keys must be encrypted or stored in Vault
            ssh-keygen -f "$dir/ssh-key" -N ''
        fi
    done
}

if [[ $# == 1 ]]; then
    main "$@"
else
    doc
    exit 1;
fi
