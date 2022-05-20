#!/usr/bin/env sh
# Exit on all errors and undefined vars
set -o errexit
set -o nounset

(cd "$(dirname "$0")" && deno task run "$@")