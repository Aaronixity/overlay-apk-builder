#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
GRADLE_VERSION="8.7"
GRADLE_HOME_DIR="/tmp/gradle_home"
DIST_DIR="$GRADLE_HOME_DIR/gradle-${GRADLE_VERSION}"

if [ ! -d "$DIST_DIR" ]; then
    TMP_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
    rm -rf "$GRADLE_HOME_DIR" "$TMP_ZIP"
    mkdir -p "$GRADLE_HOME_DIR"

    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$TMP_ZIP"
    unzip -tq "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$GRADLE_HOME_DIR"
fi

CP=$(find "$DIST_DIR/lib" -name "*.jar" | tr '\n' ':')
export GRADLE_USER_HOME="$APP_HOME/.gradle"

exec java -Xmx2048m -Dfile.encoding=UTF-8 -cp "$CP" org.gradle.launcher.GradleMain --project-dir "$APP_HOME" "$@"
