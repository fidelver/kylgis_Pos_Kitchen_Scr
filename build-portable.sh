#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
JAVA_HOME="${JAVA_HOME:-}"
[[ -x "$JAVA_HOME/bin/java" && -x "$JAVA_HOME/bin/jpackage" ]] || { echo "ERROR: JDK 21 required" >&2; exit 2; }
major="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F'[".]' '/version/ {print ($2 == 1 ? $3 : $2); exit}')"
[[ "$major" == "21" ]] || { echo "ERROR: Java 21 required" >&2; exit 3; }
CORE_JAR="${KYLGIS_CORE_JAR:-}"
[[ -f "$CORE_JAR" ]] || { echo "ERROR: set KYLGIS_CORE_JAR to KylGis POS jar" >&2; exit 4; }
ant -f build.xml -Dkylgis.core.jar="$CORE_JAR" clean jar
JAR="$(find dist-java21 -maxdepth 1 -type f -name '*.jar' -print -quit)"
[[ -n "$JAR" ]] || { echo "ERROR: Kitchen jar not found" >&2; exit 5; }
OUT="${PORTABLE_OUT:-dist-java21/portable}"
rm -rf "$OUT"; mkdir -p "$OUT"
RUNTIME="$OUT/runtime"
"$JAVA_HOME/bin/jlink" --add-modules java.base,java.desktop,java.logging,java.sql,java.naming,java.management,java.net.http,jdk.crypto.ec --output "$RUNTIME" --strip-debug --no-header-files --no-man-pages --compress=2
"$JAVA_HOME/bin/jpackage" --type app-image --name KylGisKitchen --app-version 1.3 --input "$(dirname "$JAR")" --main-jar "$(basename "$JAR")" --dest "$OUT" --runtime-image "$RUNTIME" --java-options "-Dfile.encoding=UTF-8" --java-options "--enable-native-access=ALL-UNNAMED"
(cd "$OUT"; sha256sum KylGisKitchen/* KylGisKitchen/lib/* 2>/dev/null) > "$OUT/KylGisKitchen.sha256"
printf 'KITCHEN_PORTABLE_PACKAGE=%s/KylGisKitchen
' "$OUT"