#!/bin/bash

export SONATYPE_USERNAME="${SONATYPE_USERNAME}"
export SONATYPE_PASSWORD="${SONATYPE_PASSWORD}"

cd ../
./gradlew :publish