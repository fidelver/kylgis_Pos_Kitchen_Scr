Kitchen Java 21 - candidato aislado (2026-10-19)
Base 21c0b4edfa3186f7e90ca731fdca4974cff0410f. Java21 Temurin y JavaFX 23.0.2 Linux x64.

Construir: tools/build-java21-fedora.sh con KYLGIS_JAVA21_HOME opcional. Modulos Linux javafx-base/graphics/controls/fxml 23.0.2 deben estar en caché Maven. Salida qa-java21/dist: jar, fx, lib, Run.sh.

Windows: recompilar y empaquetar nativamente en Windows x64 con JDK21 y JavaFX Windows; no copiar runtime Linux.

QA: compilacion Java21 --release 21 (18 fuentes) exitosa; jlink JavaFX Linux exitoso; validacion grafica FXML y BD requieren fixture LAB aislado; no certificado Windows ni ordenes.

Fedora FX smoke: compile tools/FxRuntimeSmoke.java with FX modulepath and execute with GDK_BACKEND=x11 and portable runtime; expected FX_SMOKE_OK and rc=0, no DB access.

QA 2026-10-19: Hibernate4/Javassist con Orders detectó InaccessibleObjectException en ClassLoader.defineClass; prueba con --add-opens=java.base/java.lang=ALL-UNNAMED pasó sin ese error. La opción se agrega a Run.sh y Run.bat como workaround temporal. JDBC MySQL 5.1.26 solo se cargó sin conexión real; ORM y C3P0 con MariaDB aislada siguen pendientes.
