{ pkgs ? import <nixpkgs> { } }:

let
  unstable = import <nixos-unstable> {};
in
pkgs.mkShell {
  NIX_SHELL = "unipipe";
  shellHook = ''
    echo starting unipipe dev shell
  '';

  buildInputs = [    

    # node / typescript (meshPanel, utilities eetc.)
    unstable.deno
  ];
}
