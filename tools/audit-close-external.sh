#!/usr/bin/env bash
set -euo pipefail
W=/home/fam/00-ChatGPT/00-MultiPlataforma/worktrees/kitchen-java21-modernizacion
JAVA=/usr/lib/jvm/java-21-temurin-jdk/bin/java
JCMD=/usr/lib/jvm/java-21-temurin-jdk/bin/jcmd
CP="$(find "$W/lib" -maxdepth 1 -name '*.jar' | paste -sd: -):$W/qa-java21-pinned/dist/KylGis_POS_Monitor_de_Cocina_Java21.jar"
FX=$(find "$W/qa-java21-pinned/dist/fx" -name '*.jar' | paste -sd: -)
LOGDIR="$W/qa-java21-pinned/logs"
mkdir -p "$LOGDIR"
limit="${AUDIT_TIMEOUT_SECONDS:-35}"
[[ "$limit" =~ ^[1-9][0-9]*$ ]] || { echo "FAIL INVALID_TIMEOUT"; exit 2; }
marker="$(mktemp "$LOGDIR/close-timeout.XXXXXXXX")"
socket_file="$(mktemp "$LOGDIR/close-sockets.XXXXXXXX")"
rm -f "$marker"
pid=''
watchdog=''
cleanup() {
  if [[ -n "$watchdog" ]]; then kill "$watchdog" 2>/dev/null || :; wait "$watchdog" 2>/dev/null || :; fi
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    kill -TERM "$pid" 2>/dev/null || :
    sleep 0.3
    kill -KILL "$pid" 2>/dev/null || :
    wait "$pid" 2>/dev/null || :
  fi
  rm -f "$marker" "$socket_file"
}
trap cleanup EXIT
if [[ "${AUDIT_TEST_HANG:-0}" == "1" ]]; then
  sleep 300 &
else
  GDK_BACKEND=x11 "$JAVA" --add-opens=java.base/java.lang=ALL-UNNAMED -Dprism.order=sw --module-path "$FX" --add-modules javafx.controls,javafx.fxml -cp "$W/qa-java21-pinned/logs:$CP" KitchenWindowCloseSmoke > "$LOGDIR/close-audited-java.log" 2>&1 &
fi
pid=$!
child_pid=$pid
echo "QA_JAVA_PID=$pid"
(
  sleep "$limit"
  if kill -0 "$pid" 2>/dev/null; then
    touch "$marker"
    kill -TERM "$pid" 2>/dev/null || :
    sleep 2
    kill -KILL "$pid" 2>/dev/null || :
  fi
) &
watchdog=$!
sleep 1
if kill -0 "$pid" 2>/dev/null; then
  echo "PASS PID_EXISTS_DURING_START"
  if [[ "${AUDIT_TEST_HANG:-0}" != "1" ]]; then
    "$JCMD" "$pid" Thread.print > "$LOGDIR/close-audited-threads.txt" 2>&1 || :
  fi
fi
if ! "${AUDIT_SS_BIN:-/usr/sbin/ss}" -ntp > "$socket_file" 2>"$LOGDIR/close-ss-error.log"; then
  echo "FAIL SS_INSPECTION_ERROR"; exit 1
fi
echo "PASS SS_INSPECTION_EXECUTED"
set +e
wait "$pid"
rc=$?
set -e
pid=''
kill "$watchdog" 2>/dev/null || :
wait "$watchdog" 2>/dev/null || :
watchdog=''
if [[ -e "$marker" ]]; then echo "FAIL PROCESS_TIMEOUT limit=${limit}s"; exit 124; fi
echo "JAVA_EXIT_RC=$rc"
if kill -0 "$child_pid" 2>/dev/null || [[ -d "/proc/$child_pid" ]]; then
  echo "FAIL PID_STILL_ALIVE_AFTER_WAIT"; exit 1
fi
echo "PASS PID_REAPED_AFTER_EXIT"
if ! "${AUDIT_SS_BIN:-/usr/sbin/ss}" -ntp > "$socket_file" 2>"$LOGDIR/close-ss-error.log"; then
  echo "FAIL SS_INSPECTION_ERROR"; exit 1
fi
echo "PASS SS_INSPECTION_AFTER_EXIT"
# The original child PID is no longer alive; sockets owned by it must be absent.
if grep -Fq "pid=$child_pid," "$socket_file"; then
  echo "FAIL JDBC_SOCKET_HELD"; exit 1
fi
echo "PASS NO_JDBC_SOCKET_FOR_EXITED_PID"
if [[ "${AUDIT_TEST_HANG:-0}" != "1" ]] && ! grep -q 'CLOSE_X_REQUEST_DISPATCHING' "$LOGDIR/close-audited-java.log"; then
  echo "FAIL CLOSE_EVENT_NOT_OBSERVED"; exit 1
fi
if [[ "$rc" -ne 0 ]]; then echo "FAIL CHILD_EXIT_CODE=$rc"; exit "$rc"; fi
echo "PASS AUDITED_PROCESS_EXIT"
