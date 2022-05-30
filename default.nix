{ pkgs ? import <nixpkgs-unstable> { } }:

pkgs.mkShell {
  NIX_SHELL = "unipipe";
  shellHook = ''
    echo starting unipipe dev shell
  '';

  buildInputs = [    
    pkgs.deno
  ];
}
