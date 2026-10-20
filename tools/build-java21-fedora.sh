#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${OUT:-$ROOT/qa-java21}"
JAVA_HOME="${KYLGIS_JAVA21_HOME:-/usr/lib/jvm/java-21-temurin-jdk}"
FX_VERSION="${FX_VERSION:-23.0.2}"
FX_REPO="${FX_REPO:-$HOME/.m2/repository/org/openjfx}"
CORE_JAR="${CORE_JAR:-/home/fam/NetBeansProjects/kylgis_Pos/target/kylgispos.jar}"
mkdir -p "$OUT/classes" "$OUT/dist/lib" "$OUT/dist/fx" "$OUT/logs"
if [[ ! -f "$CORE_JAR" ]]; then echo "Missing core jar: $CORE_JAR" >&2; exit 2; fi
if [[ ! -x "$JAVA_HOME/bin/javac" ]]; then echo "Missing JDK21: $JAVA_HOME" >&2; exit 2; fi
FX_JARS=()
for m in base graphics controls fxml; do
  f="$FX_REPO/javafx-$m/$FX_VERSION/javafx-$m-$FX_VERSION-linux.jar"
  [[ -f "$f" ]] || { echo "Missing $f" >&2; exit 3; }
  FX_JARS+=("$f")
done
LIB_JARS=()
for f in "$ROOT"/lib/*.jar; do [[ "$f" == *jfxrt-linux.jar ]] || LIB_JARS+=("$f"); done
CP="$(IFS=:; echo "${FX_JARS[*]}:${LIB_JARS[*]}:$CORE_JAR")"
find "$ROOT/src" "$ROOT/src-runtime" -type f -name '*.java' | sort > "$OUT/logs/sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/logs/sources.txt" > "$OUT/logs/javac.log" 2>&1
cd "$ROOT/src" && find . -type f ! -name '*.java' -exec cp --parents '{}' "$OUT/classes" \;
if [[ -d "$ROOT/src-runtime/META-INF" ]]; then cp -r "$ROOT/src-runtime/META-INF" "$OUT/classes/"; fi
printf 'Manifest-Version: 1.0\nMain-Class: com.mx.kylgis.kitchenscr.KitchenScr\n\n' > "$OUT/logs/manifest.mf"
"$JAVA_HOME/bin/jar" --create --file "$OUT/dist/KylGis_POS_Monitor_de_Cocina_Java21.jar" --manifest "$OUT/logs/manifest.mf" -C "$OUT/classes" .
for f in "${LIB_JARS[@]}"; do cp "$f" "$OUT/dist/lib/"; done
for f in "${FX_JARS[@]}"; do cp "$f" "$OUT/dist/fx/"; done
cat > "$OUT/dist/Run.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVABIN="$HERE/runtime/bin/java"
if [[ ! -x "$JAVABIN" ]]; then
  JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
  JAVABIN="${JAVABIN:-java}"
fi
CP="$HERE/KylGis_POS_Monitor_de_Cocina_Java21.jar"
for f in "$HERE"/lib/*.jar; do CP="$CP:$f"; done
FX=""
for f in "$HERE"/fx/*.jar; do FX="${FX:+$FX:}$f"; done
export GDK_BACKEND="${GDK_BACKEND:-x11}"
exec "$JAVABIN" -Dprism.order=sw --module-path "$FX" --add-modules javafx.controls,javafx.fxml -cp "$CP" com.mx.kylgis.kitchenscr.KitchenScr "$@"
EOS
chmod +x "$OUT/dist/Run.sh"
(
 cd "$OUT/dist"
 sha256sum KylGis_POS_Monitor_de_Cocina_Java21.jar lib/*.jar fx/*.jar > SHA256SUMS
)
echo "JAVA21_BUILD_OK jar=$OUT/dist/KylGis_POS_Monitor_de_Cocina_Java21.jar"
sha256sum "$OUT/dist/KylGis_POS_Monitor_de_Cocina_Java21.jar"
