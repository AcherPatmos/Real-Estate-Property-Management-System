#!/usr/bin/env bash
#
# Builds the self-contained application: a folder holding PropertyManagement.exe,
# its own private copy of Java, and the app's settings file - plus a zip of it.
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

# 1. Build and test the fat jar
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

# 2. Stage: only what belongs inside the app
# Pointing jpackage at target/ itself would copy classes/, test reports and the
# plain jar into the app as well. A clean folder with two files avoids that.
echo ">> Staging $JAR_NAME and a settings template"
rm -rf "$STAGING" "$OUTPUT"      # jpackage refuses to overwrite a previous build
mkdir -p "$STAGING"
cp "target/$JAR_NAME" "$STAGING/"
cp src/main/resources/db.properties.example "$STAGING/db.properties"

# 3. Find jpackage (it ships with the JDK, in the same bin folder as java)
# Use the JDK that Maven just compiled with, so the Java bundled into the app is
# the same version the code was built for. Asking Maven works even when
# JAVA_HOME is unset: on Windows, PATH often holds only a shortcut folder
# (...\Oracle\Java\javapath) with java.exe in it but not jpackage.exe.

# Git Bash: turn C:\Program Files\... into /c/Program Files/...
to_unix() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else printf '%s\n' "$1"; fi
}

JPACKAGE=""
jdk="$("$MVN" -version 2>/dev/null | sed -n 's/.*runtime: //p' | tr -d '\r')"
if [ -n "$jdk" ] && "$(to_unix "$jdk")/bin/jpackage" --version >/dev/null 2>&1; then
    JPACKAGE="$(to_unix "$jdk")/bin/jpackage"
elif [ -n "${JAVA_HOME:-}" ] && "$(to_unix "$JAVA_HOME")/bin/jpackage" --version >/dev/null 2>&1; then
    JPACKAGE="$(to_unix "$JAVA_HOME")/bin/jpackage"
elif jpackage --version >/dev/null 2>&1; then
    JPACKAGE=jpackage
fi

if [ -z "$JPACKAGE" ]; then
    echo "!! Could not find jpackage. It comes with the JDK (version 14 or newer)." >&2
    echo "   Maven reports its JDK as: ${jdk:-<nothing - run '$MVN -version' to see why>}" >&2
    echo "   If that folder has no bin/jpackage.exe, it is not a full JDK." >&2
    exit 1
fi
echo ">> Using $JPACKAGE"

extra=()
if [ "${1:-}" = "--console" ]; then extra+=(--win-console); fi

# 4.Package
# $APPDIR is in single quotes on purpose: it must reach jpackage as literal text.
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

# 5. Zip it: the folder is the deliverable, not the .exe alone
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