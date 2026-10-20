#!/usr/bin/env bash
set -euo pipefail
W=/home/fam/00-ChatGPT/00-MultiPlataforma/worktrees/kitchen-java21-modernizacion
JAVA=/usr/lib/jvm/java-21-temurin-jdk/bin/java
JCMD=/usr/lib/jvm/java-21-temurin-jdk/bin/jcmd
CP="$(find "$W/lib" -maxdepth 1 -name '*.jar' ! -name 'jfxrt-linux.jar' | paste -sd: -):$W/qa-java21-pinned/dist/KylGis_POS_Monitor_de_Cocina_Java21.jar"
FX=$(find "$W/qa-java21-pinned/dist/fx" -name '*.jar' | paste -sd: -)
GDK_BACKEND=x11 "$JAVA" --add-opens=java.base/java.lang=ALL-UNNAMED -Dprism.order=sw --module-path "$FX" --add-modules javafx.controls,javafx.fxml -cp "$W/qa-java21-pinned/logs:$CP" KitchenWindowCloseSmoke > "$W/qa-java21-pinned/logs/close-audited-java.log" 2>&1 &
pid=$!
echo "QA_JAVA_PID=$pid"
sleep 2
if kill -0 "$pid" 2>/dev/null; then
  echo "PASS PID_EXISTS_DURING_START"
  "$JCMD" "$pid" Thread.print > "$W/qa-java21-pinned/logs/close-audited-threads.txt" 2>&1 || true
  grep -E 'C3P0|Timer|JavaFX|Glass' "$W/qa-java21-pinned/logs/close-audited-threads.txt" | head -12 || true
  ss -ntp 2>/dev/null | grep "pid=$pid," | grep 33319 || true
fi
set +e
wait "$pid"
rc=$?
set -e
echo "JAVA_EXIT_RC=$rc"
if kill -0 "$pid" 2>/dev/null; then echo "FAIL PID_STILL_ALIVE"; exit 1; fi
echo "PASS PID_REAPED_AFTER_EXIT"
if ss -ntp 2>/dev/null | grep "pid=$pid," | grep -q 33319; then echo "FAIL JDBC_SOCKET_HELD"; exit 1; fi
echo "PASS NO_JDBC_SOCKET_FOR_EXITED_PID"
if [ "$rc" -ne 0 ]; then exit "$rc"; fi
echo "PASS AUDITED_PROCESS_EXIT"
