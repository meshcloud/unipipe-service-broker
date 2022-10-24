#! /bin/sh

if [[ -z "$GIT_SSH_KEY" ]] || [[ -z "$GIT_USER_EMAIL" ]] || [[ -z "$GIT_USER_NAME" ]] || [[ -z "$GIT_REPO" ]] || [[ -z "$TF_VAR_platform_secret" ]]; then
  echo "Container failed to start, please provide all of the following environment variables: GIT_SSH_KEY, GIT_USER_EMAIL, GIT_USER_NAME, GIT_REPO, TF_VAR_platform_secret"
  exit 1
else
  echo "All required environment variables set!"
fi

"$@"