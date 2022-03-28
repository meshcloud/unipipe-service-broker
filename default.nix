{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  NIX_SHELL = "unipipe";
  shellHook = ''
    echo starting unipipe dev shell
  '';

  buildInputs = [    

    # node / typescript (meshPanel, utilities eetc.)
    pkgs.deno
  ];
}
