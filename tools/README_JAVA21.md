# Kitchen Java 21

La ruta Java 21 es la compilación oficial de KylGis POS Monitor de Cocina.

- JDK: Temurin 21.
- JavaFX: 23.0.2 modular.
- Compilación: `javac --release 21`.
- Distribución portable: JAR + dependencias + JavaFX modular + runtime generado con `jlink`.
- Fedora: `tools/build-java21-fedora.sh`.
- Windows: `tools/build-java21-windows.ps1`.

Los scripts requieren un JAR de KylGis POS inmutable y su SHA-256 mediante
`CORE_JAR` y `CORE_SHA256`; rechazan artefactos tomados directamente de
`target/`.

La QA funcional aislada validó compilación, ocho paneles de órdenes, botones
Completar/Recuperar, configuración F12, cierre con X, liberación de sockets,
apagado de JVM y una prueba negativa de fallos inyectados. Resultado:
`KITCHEN_CORE_FUNCTIONAL_QA_PASS`.

La ruta de compilación y ejecución utiliza únicamente JavaFX modular.
