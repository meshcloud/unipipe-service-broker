#!/usr/bin/env bash

set -e -u

set -o pipefail

# a tiny assertsion library
source $(dirname $0)/assert.sh

# these are needed so that `tree` output does not confuse our tests
export LC_CTYPE=C 
export LANG=C

export TMPDIR_ROOT=$(mktemp -d /tmp/git-tests.XXXXXX)
# ensure that tmp directories get cleaned up after tests
# trap "rm -rf $TMPDIR_ROOT" EXIT

run() {
  export TMPDIR=$(mktemp -d ${TMPDIR_ROOT}/git-tests.XXXXXX)

  echo -e 'running \e[33m'"$@"$'\e[0m...'
  eval "$@" 2>&1 | sed -e 's/^/  /g'
  echo ""
}

unipipe() {
  # check if a binary was set via env (e.g. from all.sh)
  local unipipe_bin
  unipipe_bin="${UNIPIPE_BIN:-../unipipe/unipipe.sh}"

  "$unipipe_bin" "$@"
}

init_repo() {
  (
    set -e

    cd $(mktemp -d $TMPDIR/repo.XXXXXX)

    git init -q

    # start with an initial commit
    git \
      -c user.name='test' \
      -c user.email='test@example.com' \
      commit -q --allow-empty -m "init"

    # create some bogus branch
    git checkout -q -b bogus

    git \
      -c user.name='test' \
      -c user.email='test@example.com' \
      commit -q --allow-empty -m "commit on other branch"

    # back to master
    git checkout -q master

    # print resulting repo
    pwd
  )
}


make_commit_with_all_changes() {
  local repo="$1"

  git -C $repo add .
  git -C $repo \
    -c user.name='test' \
    -c user.email='test@example.com' \
    commit -q -m "commit"

  # output resulting sha
  git -C $repo rev-parse HEAD
}
 
init_repo_osb() {
  local repo_osb=$(init_repo)
  
  # the trailing dot in the first arg means we copy "flat", i.e. only children of the osb-git dir
  cp -r ./osb-git/. "$repo_osb"
  
  local ref=$(make_commit_with_all_changes "$repo_osb")

  # return repo_osb
  echo "$repo_osb"
}