# The Jolie Release Tools

This tool automatically prepares a Jolie installation package, as used in the website.

Usage instructions:
- Compile Jolie in the parent directory, e.g., by running `(cd ..; mvn install)`;
- In this directory:
  - enter the `installer` directory and compile the installer, e.g., by running `mvn install`;
  - go back to this directory and run `jolie release.ol .. [$jar_name]`, where `$jar_name` (optional) is the jar file name without extension (default value is jolie-installer). Example: `jolie release.ol ../jolie jolie-installer`;
  - under folder `release` there should be an all-in-one installer for Jolie `jolie-installer.jar` or the specified `$jar_name.jar`.
