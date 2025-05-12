#!/bin/sh

VER=$1
if [ -z "$VER" ]; then
	echo "Usage: $0 <version>"
	exit 1
fi

echo "This will update pom.xml, create a new commit, and force push the tag v$VER"
read -p "Are you sure that you want to proceed? (y/n) " answer
if [[ $answer != "y" ]]; then
	exit 0
fi

sed -i "s|<jolie.version>.*</jolie.version>|<jolie.version>$VER</jolie.version>|" pom.xml
git commit -am "v$VER"
git tag -f v$VER
git push origin v$VER
