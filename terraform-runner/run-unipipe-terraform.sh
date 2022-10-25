#! /bin/bash

REPO_NAME=instances-repo
REPO_DIR=~/unipipe/$REPO_NAME
if [ ! -f "$REPO_DIR" ]; then
    cat >~/.ssh/id_rsa <<< "${GIT_SSH_KEY}"
    chmod 700 ~/.ssh
    chmod 600 ~/.ssh/id_rsa
    cd ~/unipipe
    git config --global user.email $GIT_USER_EMAIL
    git config --global user.name $GIT_USER_NAME
    git clone $GIT_REMOTE $REPO_NAME
fi

cd $REPO_DIR
git pull

/usr/local/bin/unipipe terraform $REPO_DIR

git add .
git commit -m "processed instances via unipipe terraform"
git push origin
