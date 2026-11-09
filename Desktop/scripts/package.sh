#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build_arguments=(-B clean verify dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package)
if [ -n "${MISSANYE_BUILD_API_URL:-}" ]; then build_arguments+=("-Ddesktop.api.url=$MISSANYE_BUILD_API_URL"); fi
mvn "${build_arguments[@]}"
cp target/missanye-desktop-0.1.0-SNAPSHOT.jar target/package/
jpackage --type app-image --name Missanye --app-version 0.1.0 \
  --input target/package --main-jar missanye-desktop-0.1.0-SNAPSHOT.jar \
  --main-class com.missanye.desktop.Launcher --dest target/distribution
