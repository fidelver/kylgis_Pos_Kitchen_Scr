#!/usr/bin/env python3
"""Reproducible Kitchen Java21 GUI QA against an owned internal MariaDB fixture."""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
JAVA_HOME = Path("/usr/lib/jvm/temurin-21-jdk")
IMAGE = "docker.io/library/mariadb:10.11"
PORT = 33319

def run(argv, *, input=None, timeout=45, env=None):
    return subprocess.run(argv, input=input, text=True, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, check=True, timeout=timeout, env=env)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--core-jar", required=True)
    ap.add_argument("--core-sha256", required=True)
    args = ap.parse_args()
    core = Path(args.core_jar).resolve()
    if "target" in core.parts or hashlib.sha256(core.read_bytes()).hexdigest() != args.core_sha256:
        raise RuntimeError("REFUSE_UNSEALED_CORE")
    # Refuse to reuse any pre-existing DB listener, even if the port is local.
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", PORT))
    run(["podman", "image", "exists", IMAGE])
    tag = uuid.uuid4().hex[:12]
    name, network = "kylgis-kitchen-functional-" + tag, "kylgis-kitchen-net-" + tag
    temp = Path(tempfile.mkdtemp(prefix="kylgis-kitchen-functional-"))
    made_network = made_container = False
    env = os.environ.copy()
    env.update(KYLGIS_JAVA21_HOME=str(JAVA_HOME), OUT=str(temp / "build"),
               CORE_JAR=str(core), CORE_SHA256=args.core_sha256, GDK_BACKEND="x11")
    try:
        result = run(["bash", str(ROOT / "tools/build-java21-fedora.sh")], timeout=150, env=env)
        print(result.stdout.strip())
        dist = temp / "build/dist"
        monitor = dist / "KylGis_POS_Monitor_de_Cocina_Java21.jar"
        print("KITCHEN_CORE_INPUT_SHA256=" + args.core_sha256)
        print("KITCHEN_MONITOR_SHA256=" + hashlib.sha256(monitor.read_bytes()).hexdigest())
        run(["podman", "network", "create", "--internal", network])
        made_network = True
        run(["podman", "run", "-d", "--rm", "--pull=never", "--name", name,
             "--network", network, "--publish", "127.0.0.1:33319:3306",
             "--label", "kylgis.qa.scope=kitchen-functional-" + tag,
             "--memory", "600m", "--cpus", "2",
             "--tmpfs", "/var/lib/mysql:rw,size=384m",
             "--env", "MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1", IMAGE])
        made_container = True
        for _ in range(60):
            check = subprocess.run(["podman", "exec", name, "mariadb", "-uroot",
                                    "--protocol=SOCKET", "--skip-ssl", "-e", "SELECT 1"],
                                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            logs = run(["podman", "logs", name]).stdout
            if check.returncode == 0 and "MariaDB init process done. Ready for start up." in logs:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("SYNTHETIC_DB_NOT_READY")
        def sql(statement):
            return run(["podman", "exec", "-i", name, "mariadb", "-uroot",
                        "--protocol=SOCKET", "--skip-ssl"], input=statement)
        sql("""CREATE DATABASE kitchen_sintetica;
CREATE TABLE kitchen_sintetica.orders (
 ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, ORDERID VARCHAR(50),
 QTY INT, DETAILS VARCHAR(255), ATTRIBUTES VARCHAR(255), NOTES VARCHAR(255),
 TICKETID VARCHAR(50), ORDERTIME TIMESTAMP(3) NULL, DISPLAYID INT,
 AUXILIARY INT, COMPLETETIME TIMESTAMP(3) NULL
) ENGINE=InnoDB;""")
        classes = temp / "tests"
        classes.mkdir()
        fx = ":".join(str(f) for f in sorted((dist / "fx").glob("*.jar")))
        cp = ":".join([str(core), str(monitor)] + [str(f) for f in sorted((dist / "lib").glob("*.jar"))])
        tests = ["KitchenGuiOrdersSmoke", "KitchenGuiButtonSmoke", "KitchenStartupF12Smoke", "KitchenWindowCloseSmoke"]
        run([str(JAVA_HOME / "bin/javac"), "--release", "21", "--module-path", fx,
             "--add-modules", "javafx.controls,javafx.fxml", "-cp", cp, "-d", str(classes)]
            + [str(ROOT / "tools" / (name + ".java")) for name in tests])
        java = [str(JAVA_HOME / "bin/java"), "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "-Dprism.order=sw", "-Duser.home=" + str(temp),
                "--module-path", fx, "--add-modules", "javafx.controls,javafx.fxml",
                "-cp", str(classes) + ":" + cp]
        def seed(prefix, count):
            sql("DELETE FROM kitchen_sintetica.orders;")
            rows = []
            for i in range(1, count + 1):
                for aux in ([0, 1] if i == (3 if count == 8 else 1) else [0]):
                    rows.append("('%s-%d',1,'%s-%d','','QA','T-%d','2026-10-21 12:%02d:00',1,%d,'2026-10-21 12:%02d:00')"
                                % (prefix, i, "Auxiliar" if aux else "Producto", i, i, i, aux, i))
            sql("INSERT INTO kitchen_sintetica.orders (ORDERID,QTY,DETAILS,ATTRIBUTES,NOTES,TICKETID,ORDERTIME,DISPLAYID,AUXILIARY,COMPLETETIME) VALUES "
                + ",".join(rows) + ";")
        for test in tests:
            seed("GUI-J21" if test == tests[0] else "BTN-J21", 8 if test == tests[0] else 1)
            process = subprocess.Popen(java + [test], stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, text=True, env=env)
            try:
                output, _ = process.communicate(timeout=40)
            except subprocess.TimeoutExpired:
                process.kill()
                process.communicate(timeout=5)
                raise RuntimeError("KITCHEN_QA_PROCESS_TIMEOUT " + test)
            if process.returncode:
                print(output[-8000:])
                raise RuntimeError("KITCHEN_QA_FAILED " + test + " rc=" + str(process.returncode))
            if Path("/proc/" + str(process.pid)).exists():
                raise RuntimeError("KITCHEN_QA_PID_NOT_REAPED")
            if test == "KitchenWindowCloseSmoke" and "CLOSE_X_REQUEST_DISPATCHING" not in output:
                raise RuntimeError("CLOSE_EVENT_NOT_OBSERVED")
            sockets = run(["/usr/sbin/ss", "-ntp"]).stdout
            if "pid=" + str(process.pid) + "," in sockets:
                raise RuntimeError("KITCHEN_QA_SOCKET_LEFT")
            for line in output.splitlines():
                if line.startswith(("PASS ", "GUI_SLOT_", "CLOSE_X_")):
                    print(line)
            print("PASS KITCHEN_JVM_EXITED_AND_SOCKETS_RELEASED " + test)
        # Prove the harness cannot turn an injected rendering failure into success.
        seed("GUI-J21", 8)
        negative = subprocess.run(java + ["-Dkylgis.qa.forceFailure=true", tests[0]],
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  text=True, timeout=40, env=env)
        if negative.returncode != 1 or "INTENTIONAL_QA_FAILURE" not in negative.stdout:
            raise RuntimeError("KITCHEN_NEGATIVE_GATE_NOT_EFFECTIVE")
        print("PASS KITCHEN_INJECTED_GUI_FAILURE_RETURNS_ONE")
        sql("DELETE FROM kitchen_sintetica.orders;")
        print("KITCHEN_CORE_FUNCTIONAL_QA_PASS")
    finally:
        if made_container:
            run(["podman", "stop", "-t", "5", name])
            if subprocess.run(["podman", "container", "exists", name]).returncode == 0:
                raise RuntimeError("KITCHEN_QA_CONTAINER_LEFT")
        if made_network:
            run(["podman", "network", "rm", network])
        shutil.rmtree(temp)
        print("KITCHEN_OWNED_QA_RESOURCES_CLEANED")

if __name__ == "__main__":
    main()
