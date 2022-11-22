#!/usr/bin/env bash
# Exit on all errors and undefined vars
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

REPO_NAME=instances-repo
REPO_DIR=~/unipipe/$REPO_NAME
if [[ ! -d "$REPO_DIR" ]]; then
    cat > ~/.ssh/id_rsa <<< "${GIT_SSH_KEY}"
    chmod 700 ~/.ssh
    chmod 600 ~/.ssh/id_rsa
    cd ~/unipipe
    git config --global user.email "$GIT_USER_EMAIL"
    git config --global user.name "$GIT_USER_NAME"
    git clone "$GIT_REMOTE" "$REPO_NAME"
fi

cd $REPO_DIR

/usr/local/bin/unipipe git pull $REPO_DIR
/usr/local/bin/unipipe terraform $REPO_DIR
/usr/local/bin/unipipe git push $REPO_DIR