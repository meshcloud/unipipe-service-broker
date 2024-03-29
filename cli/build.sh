#!/usr/bin/env bash

set -e
# before we do anything else, ensure we typecheck fine
deno task check

deno_flags=$(deno run flags.ts --quiet)

mkdir -p bin

compile(){
  target="$1"

  deno compile $deno_flags --target "$target" --output "./bin/unipipe-cli-$target" unipipe/main.ts
}

deno test $deno_flags

compile "x86_64-unknown-linux-gnu"
compile "x86_64-apple-darwin"
compile "x86_64-pc-windows-msvc"


# first grep filters for the first line of the output "deno 1.8.1 (release, x86_64-apple-darwin)"
# second grep extracts the architecture string, but can't get rid of the trailing ): "x86_64-apple-darwin)"
# sed then removes the last trailing character: "x86_64-apple-darwin"
arch=$(deno --version | grep deno | grep -o "[a-z0-9_-]*)" | sed 's/.$//')

echo "currently running on $arch, creating symlink at ./bin/unipipe"
ln -fs "./unipipe-cli-$arch" ./bin/unipipe
chmod +x ./bin/unipipe