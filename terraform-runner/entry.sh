#!/usr/bin/env bash

if [[ -z "$GIT_SSH_KEY" ]] || [[ -z "$GIT_USER_EMAIL" ]] || [[ -z "$GIT_USER_NAME" ]] || [[ -z "$GIT_REMOTE" ]] || [[ -z "$GIT_REMOTE_BRANCH" ]] || [[ -z "$TF_VAR_platform_secret" ]]; then
  echo "Container failed to start, please provide all of the following environment variables: GIT_SSH_KEY, GIT_USER_EMAIL, GIT_USER_NAME, GIT_REMOTE, GIT_REMOTE_BRANCH, TF_VAR_platform_secret"
  exit 1
else
  echo "All required environment variables set!"
  {
    printf 'export GIT_SSH_KEY="%s"\n' "$GIT_SSH_KEY"
    printf 'export GIT_USER_EMAIL="%s"\n' "$GIT_USER_EMAIL"
    printf 'export GIT_USER_NAME="%s"\n' "$GIT_USER_NAME"
    printf 'export GIT_REMOTE="%s"\n' "$GIT_REMOTE"
    printf 'export GIT_REMOTE_BRANCH="%s"\n' "$GIT_REMOTE_BRANCH"
    printf 'export TF_VAR_platform_secret="%s"\n' "$TF_VAR_platform_secret"
  } > ~/unipipe/terraform-runner-env.sh

  echo '* * * * * ~/unipipe/run-unipipe-terraform.sh > /proc/1/fd/1 2>/proc/1/fd/2' | crontab -
fi

"$@"
