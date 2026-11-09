#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -B clean verify dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package
cp target/missanye-desktop-0.1.0-SNAPSHOT.jar target/package/
jpackage --type app-image --name Missanye --app-version 0.1.0 \
  --input target/package --main-jar missanye-desktop-0.1.0-SNAPSHOT.jar \
  --main-class com.missanye.desktop.Launcher --dest target/distribution
