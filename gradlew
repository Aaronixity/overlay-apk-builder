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
    rm -rf "$TMP_DIR" "$TMP_ZIP"
    mkdir -p "$TMP_DIR"

    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$TMP_ZIP"
    unzip -tq "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$TMP_DIR"

    LAUNCHER_JAR=$(find "$TMP_DIR" -name "gradle-launcher-*.jar" | head -n 1)
    if [ -z "${LAUNCHER_JAR:-}" ]; then
        echo "Could not find gradle-launcher jar in extracted archive" >&2
        exit 1
    fi
    cp "$LAUNCHER_JAR" "$WRAPPER_JAR"
fi

exec java -Xmx2048m -Dfile.encoding=UTF-8 -cp "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
