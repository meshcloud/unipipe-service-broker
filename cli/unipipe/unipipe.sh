#!/usr/bin/env sh
# Exit on all errors and undefined vars
set -o errexit
set -o nounset

deno run --allow-read --allow-write --allow-env --unstable "$(dirname "$0")"/main.ts "$@"