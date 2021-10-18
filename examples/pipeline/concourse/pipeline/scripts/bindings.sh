#!/bin/bash

# Exit on all errors and undefined vars
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

doc() {
    cat <<-EOF
Creates and deletes Service Bindings. Should be invoked from parent directory of example-osb-ci and
the instances repository. Alternativley using absolute paths also allows execution of this script from any dir.

The generate-ssh-keys.ssh and create-tfvars.sh and deploy.sh scripts must have been executed before. The deployment
uses the generated SSH key and the created instance.tfvars file. It creates the binding in the instance created via deploy.sh.

USAGE:
    ./example-osb-ci/pipeline/scripts/bindings.sh <ci-repository> <instances-repository>

EXAMPLES:
    ./example-osb-ci/pipeline/scripts/bindings.sh example-osb-ci example-osb-repo

EOF
}

createUser() {
  host="$1"
  username="$2"
  password="$3"
  cat >"inventory" <<EOL
[all:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=../../ssh-key
ansible_ssh_common_args='-o StrictHostKeyChecking=no'
ansible_python_interpreter=python3

[mc_host]
$host
EOL
  export mc_username="${username}"
  export mc_password="${password}"
  ansible-playbook -i inventory --become create-user.yml
}

deleteUser() {
  host="$1"
  username="$2"
  cat >"inventory" <<EOL
[all:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=../../ssh-key
ansible_ssh_common_args='-o StrictHostKeyChecking=no'
ansible_python_interpreter=python3

[mc_host]
$host
EOL
  export mc_username="${username}"
  ansible-playbook -i inventory --become delete-user.yml
}

main(){
  CI_ROOT="$1"
  INSTANCES_ROOT="$2"
  anyFailed=false
  for dir in $INSTANCES_ROOT/instances/*/bindings/*
  do
    echo "Work on binding $dir"
    cp "$CI_ROOT/terraform/example-sb.tf" "$dir/../.."
    cp "$CI_ROOT/ansible/create-user.yml" "$dir"
    cp "$CI_ROOT/ansible/delete-user.yml" "$dir"
    chmod 400 $dir/../../ssh-key
    oldDir="$(pwd)"
    cd $dir

    isDeleted="$(grep 'deleted:' binding.yml | cut -d\  -f2-)"

    if [[ -e "status.yml" ]]; then
      initialStatus="$(grep 'status:' status.yml | cut -d\  -f2-)"
    else
      initialStatus="\"in progress\""
    fi

    if [[ "$initialStatus" == "\"in progress\"" ]]; then
      echo "Processing 'in progress' binding"
      cd ../..
      terraform init
      host="$(terraform output ip)"
      cd -
      if [[ "$isDeleted" == "true" ]]; then
        echo "Processing deleted binding"
        unset username
        if [[ -e "credentials.yml" ]]; then
          username="$(grep 'username:' credentials.yml | cut -d\  -f2-)"
        fi
        if [[ ! -n "${username-}" ]] || deleteUser "$host" "$username"; then
          rm -f credentials.yml
          echo "Deleted user ${username} successfully"
          status="succeeded"
          description=""
        else
          status="failed"
          description="Delete Binding failed! Please contact an administrator."
          echo "--- Delete Binding of '$dir' failed! ---"
          anyFailed="true"
        fi
      else
        echo "Creating new binding"
        username="$(LC_CTYPE=C tr -dc 'a-zA-Z0-9' < /dev/urandom | head -c 16 || true)"
        password="$(LC_CTYPE=C tr -dc 'a-zA-Z0-9' < /dev/urandom | head -c 32 || true)"
        if [[ -e "credentials.yml" ]]; then
          rm "credentials.yml"
        fi
        if createUser "$host" "$username" "$password"; then
          echo "Create user ${username} executed successfully"
          cat >"credentials.yml" <<EOL
host: ${host}
username: ${username}
password: ${password}
command: ssh ${username}@${host} -o PubkeyAuthentication=no
EOL
          status="succeeded"
          description=""
        else
          status="failed"
          description="Create Binding failed! Please contact an administrator."
          echo "--- Create Binding of '$dir' failed! ---"
          anyFailed="true"
        fi
      fi
    fi

    rm -f create-user.yml
    rm -f delete-user.yml
    rm -f ../../example-sb.tf
    cd ${oldDir}

    if [[ -n "${status-}" ]]; then
      ./$CI_ROOT/pipeline/scripts/update-status.sh "$dir" "$status" "$description"
    fi
  done

  if [[ $anyFailed == "true" ]]; then
    echo "!!! At least one binding failed! Check this log for details! !!!"
    exit 1
  fi
}

if [[ $# == 2 ]]; then
    main "$@"
else
    doc
    exit 1;
fi