{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  nativeBuildInputs = [
    pkgs.gradle
    pkgs.jdk17  # Explicitly use Nix's JDK 17
  ];

  # Ensure Gradle sees the Nix JDK
  JAVA_HOME = "${pkgs.jdk17}";
}
