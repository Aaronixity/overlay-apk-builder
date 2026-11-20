#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
GRADLE_VERSION="8.7"

if [ ! -f "$WRAPPER_JAR" ]; then
    mkdir -p "$WRAPPER_DIR"
    TMP_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
    TMP_DIR="/tmp/gradle_extract"
    rm -rf "$TMP_DIR"
    mkdir -p "$TMP_DIR"
    curl -sSL "https://repo.gradle.org/gradle/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$TMP_DIR"
    LAUNCHER_JAR=$(find "$TMP_DIR" -name "gradle-launcher-*.jar" | head -n 1)
    cp "$LAUNCHER_JAR" "$WRAPPER_JAR"
fi

exec java -Xmx2048m -Dfile.encoding=UTF-8 -cp "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
