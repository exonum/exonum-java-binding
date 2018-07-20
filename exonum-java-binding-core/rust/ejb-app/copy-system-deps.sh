#!/usr/bin/env bash

EJB_APP_DIR=$(pwd)
echo "CURRENT_DIR=${EJB_APP_DIR}"

EJB_ROOT=$(realpath "../../..")
echo "PROJ_ROOT=${EJB_ROOT}"

CORE_TXT="exonum-java-binding-core/target/ejb-core-classpath.txt"
DEPENDENCIES_LINE="$(cat ${EJB_ROOT}/${CORE_TXT})"
DEPENDENCIES=$(echo $DEPENDENCIES_LINE | tr ":" "\n")

rm -rf lib
mkdir -p lib/java

for DEPENDENCY in $DEPENDENCIES
do
    cp "$DEPENDENCY" "$EJB_APP_DIR/lib/java"
done

cp "${EJB_ROOT}/exonum-java-binding-core/target/exonum-java-binding-core-0.1.0.jar" "$EJB_APP_DIR/lib/java"