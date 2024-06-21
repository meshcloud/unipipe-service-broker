{
  description = "Java 17 development environment with optional JetBrains IDEA";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      rec {
        # Default devShell with Java 17
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.zulu17 # zulu is the only one that provides aarch64-darwin builds
            pkgs.deno
          ];
        };

        # Separate devShell for JetBrains IntelliJ IDEA
        devShells.idea = pkgs.mkShell {
          buildInputs = [
            pkgs.jetbrains.idea-community
            pkgs.openjdk17
          ];
        };
      });
}