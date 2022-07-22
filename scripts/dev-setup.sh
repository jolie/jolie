#!/bin/sh

BIN_DIR=`realpath $1`
echo -n "Linking launchers in $BIN_DIR..."
ln -sf $PWD/dist/launchers/unix/* $BIN_DIR/
echo " done!"

DIST_DIR=$1/jolie-dist
echo -n "Linking the Jolie distribution to $DIST_DIR..."
ln -sf $PWD/dist/jolie $DIST_DIR
echo " done!"

echo "Success! Make sure that your environment variable JOLIE_HOME is set to $DIST_DIR (if you're inside a devcontainer, this is done automatically)."