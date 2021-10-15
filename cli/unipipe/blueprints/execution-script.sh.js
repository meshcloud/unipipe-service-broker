export const executionScript = `
#!/usr/bin/env bash
set -o errexit  # exit on error
set -o errtrace  # enables ERR traps so we can run cleanup
set -o pipefail  # exit on error in a pipe, without this only the status of the last command in a pipe is considered
set -o nounset  # exit on undefined variables

shopt -s globstar

for filepath in **/*.main.tf; do
    [ -f "$filepath"  ] || continue

    echo "executing $filepath"

    filename=$(basename "$filepath")
    instance_id="\${filename%.main.tf}"
    first_line=$(head -1 $filepath)

    terraform -chdir="$(dirname "$filepath")" init

    if [ "$first_line" = "#DELETED" ]; then
        terraform -chdir="$(dirname "$filepath")" destroy -auto-approve
        unipipe update --instance-id "$instance_id" --status "succeeded" --description "Service is Destroyed." ./
        echo "$filepath is destroyed."
    else
        terraform -chdir="$(dirname "$filepath")" apply -auto-approve
        unipipe update --instance-id "$instance_id" --status "succeeded" --description "Service is Ready." ./
        echo "$filepath is applied."
    fi
    echo "----------------------------------------------------------------"
done
`
