# KylGis POS Monitor de Cocina

**KylGis POS Monitor de Cocina** es la pantalla de cocina integrada con KylGis POS. El proyecto mantiene su repositorio independiente y utiliza el namespace `com.mx.kylgis.kitchenscr`.

## Identidad

- Producto: **KylGis POS Monitor de Cocina**
- Versión KylGis: **1.0.0**
- Sitio del producto: `https://pos.kylgis.com`
- Clase principal: `com.mx.kylgis.kitchenscr.KitchenScr`
- Artefacto: `dist/KylGis_POS_Monitor_de_Cocina.jar`
- Configuración local: `~/kylgis-pos-monitor-cocina.properties`

## Integración con KylGis POS

Kitchen Screen puede reutilizar directamente un archivo `.properties` de KylGis POS. Las claves actuales son `kylgis.pos.config.enabled` y `kylgis.pos.config`. Las instalaciones anteriores que todavía tengan `~/kylgis-kitchen-screen.properties` o `~/chromis.properties` o las claves `unicenta.config.*` se migran automáticamente por compatibilidad.

## Compilación

Requiere Java 8 con JavaFX. En el entorno de desarrollo actual:

```bash
export JAVA_HOME=/usr/lib/jvm/bellsoft-java8-full.x86_64
export PATH="$JAVA_HOME/bin:$PATH"
ant -f nbbuild.xml clean jar
```

La aplicación no debe necesitar servidores ni repositorios de Chromis o uniCenta para compilarse: las dependencias necesarias están actualmente en `lib/`.

## Autoría y licencia

El código base procede de **Chromis Kitchen Screen**, de John Lewis / Chromis. KylGis conserva la atribución upstream y la licencia GNU GPL v3 o posterior. Las modificaciones de KylGis de 2026 incluyen la integración con KylGis POS, agrupación por `COMPLETETIME`, correcciones de completado/recuperación de comandas, conservación del orden real de productos y auxiliares, acceso de configuración F12, migración de namespace y rebranding.

Consulte `AUTHORS_AND_CONTRIBUTIONS.md` para el detalle de atribución.
