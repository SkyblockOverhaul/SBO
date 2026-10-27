#!/bin/sh
set -eu
version="${1:-26.2}"
case "$version" in 26.1.2|26.2|26.3) ;; *) echo "Usage: $0 [26.1.2|26.2|26.3]" >&2; exit 2 ;; esac
cd "$(dirname "$0")/../.."
if [ -z "${JAVA_HOME:-}" ]; then
    for cached_jdk in "$HOME"/.gradle/jdks/*/*/Contents/Home; do
        if [ -x "$cached_jdk/bin/java" ]; then export JAVA_HOME="$cached_jdk"; break; fi
    done
fi
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Djava.awt.headless=true" SBO_UI_REVIEW=1 \
./gradlew ":${version}-fabric:test" --tests net.sbo.mod.test.HubPresentationTest --offline --no-daemon --console=plain
