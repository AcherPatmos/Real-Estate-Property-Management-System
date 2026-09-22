#!/usr/bin/env bash
#
# Builds the self-contained application: a folder holding PropertyManagement.exe,
# its own private copy of Java, and the app's settings file plus a zip of it.
# The machine that runs the result needs no Java installed.
#
# Run from the project root, in Git Bash:
#     ./package.sh             normal build
#     ./package.sh --console   Windows debug build: the .exe opens a console
#                              window that shows errors (use it when
#                              double-clicking seems to do nothing)
#
# jpackage builds for the operating system it runs on. Run this on Windows to
# get a Windows .exe; run it on a Mac to get a Mac app.

set -euo pipefail

APP_NAME="PropertyManagement"
MAIN_CLASS="com.Propertmanagement.PropertyManagerApp.PropertyManagerApp"
STAGING="target/dist"          # jpackage copies EVERYTHING in here into the app
OUTPUT="target/installer"

cd "$(dirname "$0")"

# --- 1. Build and test the fat jar ------------------------------------------
if command -v mvn >/dev/null 2>&1; then MVN=mvn; else MVN=./mvnw; fi
echo ">> Building with $MVN (this runs the test suite too)"
"$MVN" -q clean package

shopt -s nullglob
jars=(target/*-app.jar)
if [ "${#jars[@]}" -ne 1 ]; then
    echo "!! Expected exactly one target/*-app.jar after the build, found ${#jars[@]}." >&2
    exit 1
fi
JAR_NAME="$(basename "${jars[0]}")"

#  Stage: only what belongs inside the app
# Pointing jpackage at target/ itself would copy classes/, test reports and the
# plain jar into the app as well. A clean folder with two files avoids that.
echo ">> Staging $JAR_NAME and a settings template"
rm -rf "$STAGING" "$OUTPUT"      # jpackage refuses to overwrite a previous build
mkdir -p "$STAGING"
cp "target/$JAR_NAME" "$STAGING/"
cp src/main/resources/db.properties.example "$STAGING/db.properties"

# 3. Find jpackage (it ships with the JDK, next to java)
if [ -n "${JAVA_HOME:-}" ]; then
    # Git Bash: turn C:\Program Files\... into /c/Program Files/...
    if command -v cygpath >/dev/null 2>&1; then JAVA_HOME="$(cygpath -u "$JAVA_HOME")"; fi
    JPACKAGE="$JAVA_HOME/bin/jpackage"
else
    JPACKAGE=jpackage
fi
if ! "$JPACKAGE" --version >/dev/null 2>&1; then
    echo "!! Could not run jpackage. Set JAVA_HOME to your JDK folder, or put its bin folder on PATH." >&2
    exit 1
fi

extra=()
if [ "${1:-}" = "--console" ]; then extra+=(--win-console); fi

#  4. Package
# The launcher replaces it at run time with the app's own folder, so the app
# always reads the db.properties that sits inside it, wherever it is launched from.
echo ">> Running jpackage (takes a minute: it assembles a private Java runtime)"
"$JPACKAGE" \
    --type app-image \
    --name "$APP_NAME" \
    --input "$STAGING" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --dest "$OUTPUT" \
    --java-options '-Ddb.config=$APPDIR/db.properties' \
    "${extra[@]+"${extra[@]}"}"

# Zip it: the folder is the deliverable, not the .exe alone
# (The jar tool writes ordinary zip files, and every JDK has one.)
ZIP="target/$APP_NAME-windows.zip"
"$(dirname "$JPACKAGE")/jar" cMf "$ZIP" -C "$OUTPUT" "$APP_NAME" 2>/dev/null \
    || jar cMf "$ZIP" -C "$OUTPUT" "$APP_NAME"

echo
echo "Done."
echo "  App folder : $OUTPUT/$APP_NAME/"
echo "  Zip        : $ZIP  ($(du -h "$ZIP" | cut -f1))"
echo
echo "Before the first launch, edit the settings inside the package:"
echo "  $(find "$OUTPUT/$APP_NAME" -name db.properties)"
