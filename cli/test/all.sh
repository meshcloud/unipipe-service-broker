#!/usr/bin/env bash

set -e

unipipe_bin="${1}"

if [ -n "$unipipe_bin" ]; then
  export UNIPIPE_BIN="$unipipe_bin"
fi

$(dirname $0)/help.sh
$(dirname $0)/list.sh
$(dirname $0)/show.sh
$(dirname $0)/transform.sh
$(dirname $0)/update.sh
$(dirname $0)/generate.sh

echo -e '\e[32mall tests passed!\e[0m'
