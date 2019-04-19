#!/bin/zsh

setopt extendedglob

BIN_DIR=`realpath $1`
ln -sf $(pwd)/dist/launchers/unix/* $BIN_DIR/

DIST_DIR=$1/jolie-dist

mkdir -p $DIST_DIR
ln -sf $(pwd)/jolie/dist/*.jar $DIST_DIR/
ln -sf $(pwd)/libjolie/dist/*.jar $DIST_DIR/

mkdir -p $DIST_DIR/extensions
ln -sf $(pwd)/extensions/*/dist/*.jar $DIST_DIR/extensions/

mkdir -p $DIST_DIR/lib
ln -sf $(pwd)/lib/*/dist/*.jar $DIST_DIR/lib/
ln -sf $(pwd)/lib/^jaxws*/*.jar $DIST_DIR/lib/
mkdir -p $DIST_DIR/lib/jaxws
ln -sf $(pwd)/lib/jaxws/*.jar $DIST_DIR/lib/jaxws/
ln -sf $(pwd)/support/*/dist/*.jar $DIST_DIR/lib/

mkdir -p $DIST_DIR/javaServices
ln -sf $(pwd)/javaServices/*/dist/*.jar $DIST_DIR/javaServices/

ln -sf $(pwd)/include $DIST_DIR/include
