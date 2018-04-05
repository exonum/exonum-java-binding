#!/usr/bin/env bash
# Creates a project from given archetype and runs its tests.
mvn install -pl exonum-java-binding-core,exonum-java-binding-service-archetype -am
mvn archetype:generate \
  -DinteractiveMode=false \
  -DarchetypeGroupId=com.exonum.binding \
  -DarchetypeArtifactId=exonum-java-binding-service-archetype \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=com.my.group \
  -DartifactId=myService \
  -Dversion=1.0
cd myService
mvn test
cd -
