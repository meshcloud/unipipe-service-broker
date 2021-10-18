#!/bin/bash

# Exit on all errors and undefined vars
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset
set -x

doc() {
    cat <<-EOF
Executes a 'git commit' in the current folder. Uses "Concourse" as committer.
A 'git rebase' is executed first to fetch changes in the remote repo first.

USAGE:
    ../example-osb-ci/pipeline/scripts/git-commit.sh <commit-message>

EXAMPLES:
    ../example-osb-ci/pipeline/scripts/git-commit.sh "Updated Status"

EOF
}

main(){
    echo '## Commiting changes to git'
    git config --global user.email "nobody@concourse-ci.org"
    git config --global user.name "Concourse"
    git add .
    git diff-index --quiet HEAD || git commit -m "$1"
    echo '## Git Commit completed'
}

if [[ $# == 1 ]]; then
    main "$@"
else
    doc
    exit 1;
fi