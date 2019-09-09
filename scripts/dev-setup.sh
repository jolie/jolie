#!/bin/sh

BIN_DIR=`realpath $1`
ln -sf $PWD/dist/launchers/unix/* $BIN_DIR/

DIST_DIR=$1/jolie-dist

ln -sf $PWD/dist/jolie $DIST_DIR