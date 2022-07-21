#!/bin/sh

BIN_DIR=`realpath $1`
echo "Linking launchers in $BIN_DIR"
ln -sf $PWD/dist/launchers/unix/* $BIN_DIR/

DIST_DIR=$1/jolie-dist
echo "Linking the Jolie distribution to $DIST_DIR"
ln -sf $PWD/dist/jolie $DIST_DIR

echo "Success! Make sure that your environment variable JOLIE_HOME is set to $DIST_DIR."