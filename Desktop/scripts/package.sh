#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
app_version="${MISSANYE_APP_VERSION:-0.2.2}"
[[ "$app_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Version attendue: major.minor.patch" >&2; exit 1; }
build_arguments=("-Ddesktop.release.version=$app_version" -B clean verify dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package)
if [ -n "${MISSANYE_BUILD_API_URL:-}" ]; then build_arguments+=("-Ddesktop.api.url=$MISSANYE_BUILD_API_URL"); fi
mvn "${build_arguments[@]}"
cp target/missanye-desktop-0.1.0-SNAPSHOT.jar target/package/
if [ -n "${MISSANYE_BUILD_DEPLOYMENT_FILE:-}" ]; then cp "$MISSANYE_BUILD_DEPLOYMENT_FILE" target/package/parish.properties; fi
jpackage --type app-image --name Missanye --app-version "$app_version" \
  --input target/package --main-jar missanye-desktop-0.1.0-SNAPSHOT.jar \
  --main-class com.missanye.desktop.Launcher --java-options '-Dmissanye.deploymentFile=$APPDIR/parish.properties' --dest target/distribution
