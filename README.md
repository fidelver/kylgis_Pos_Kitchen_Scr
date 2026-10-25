# KylGis POS Monitor de Cocina

**KylGis POS Monitor de Cocina** es la pantalla de cocina integrada con KylGis POS. El proyecto mantiene su repositorio independiente y utiliza el namespace `com.mx.kylgis.kitchenscr`.

## Identidad

- Producto: **KylGis POS Monitor de Cocina**
- Versión KylGis: **1.0.0**
- Sitio del producto: `https://pos.kylgis.com`
- Clase principal: `com.mx.kylgis.kitchenscr.KitchenScr`
- Artefacto: `KylGis_POS_Monitor_de_Cocina_Java21.jar`
- Configuración local: `~/kylgis-pos-monitor-cocina.properties`

## Integración con KylGis POS

Kitchen Screen puede reutilizar directamente un archivo `.properties` de KylGis POS. Las claves actuales son `kylgis.pos.config.enabled` y `kylgis.pos.config`. Las instalaciones anteriores que todavía tengan `~/kylgis-kitchen-screen.properties`, `~/chromis.properties` o las claves `unicenta.config.*` se migran automáticamente por compatibilidad.

## Compilación y ejecución

La ruta oficial utiliza **JDK 21** y **JavaFX modular 23.0.2**.

Para compilar con Ant:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk
CORE_JAR=/ruta/inmutable/kylgispos.jar
ant -f build.xml -Dkylgis.core.jar="$CORE_JAR" clean jar
./dist-java21/Run.sh
```

Para generar el paquete portable con runtime `jlink` en Fedora:

```bash
CORE_SHA256=$(sha256sum "$CORE_JAR" | awk '{print $1}')
KYLGIS_JAVA21_HOME=/usr/lib/jvm/java-21-temurin-jdk \
CORE_JAR="$CORE_JAR" CORE_SHA256="$CORE_SHA256" \
bash tools/build-java21-fedora.sh
```

El script produce `qa-java21/dist` con el JAR, JavaFX modular, dependencias y un runtime Java 21 aislado. En Windows se debe ejecutar `tools/build-java21-windows.ps1` con JDK 21 y JavaFX Windows.

## Autoría y licencia

El código base procede de **Chromis Kitchen Screen**, de John Lewis / Chromis. KylGis conserva la atribución upstream y la licencia GNU GPL v3 o posterior. Las modificaciones de KylGis de 2026 incluyen la integración con KylGis POS, agrupación por `COMPLETETIME`, correcciones de completado/recuperación de comandas, conservación del orden real de productos y auxiliares, acceso de configuración F12, migración de namespace y rebranding.

Consulte `AUTHORS_AND_CONTRIBUTIONS.md` para el detalle de atribución.
