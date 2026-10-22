# Kitchen — batería funcional reproducible con CORE de recuperación
Rama `collab/kitchen-java21-modernizacion`, base `cc24f8e04672fa4e17e3ad4726b44f8defc5770b`.

Se añadió `tools/run_core13_functional_qa.py`. Compila Java21/JavaFX, valida SHA CORE y rechaza target mutable; crea MariaDB efímera en red Podman interna y puerto únicamente loopback 33319, con esquema kitchen_sintetica. Rechaza un listener preexistente. Cada GUI corre en JVM separada con plazo; se comprueba PID recolectado y ausencia de sockets tras salir. Limpia únicamente contenedor, red y árbol temporal propios.

Ejecución propia:
`python3 -B tools/run_core13_functional_qa.py --core-jar /tmp/kylgis-core-payment-context-abe3d9166396fa0db74d1dcdc329f73966825f59cfe7945af54656efcb7645bf.jar --core-sha256 abe3d9166396fa0db74d1dcdc329f73966825f59cfe7945af54656efcb7645bf`.

**RC0**, KITCHEN_CORE_FUNCTIONAL_QA_PASS y KITCHEN_OWNED_QA_RESOURCES_CLEANED. Ocho paneles reales (tercero con auxiliar), botones JavaFX reales Terminar/Recuperar con lectura de BD, F12/configuración/retorno, evento cierre X y salida real. Inyección de fallo gráfico devuelve RC1 obligatorio. No es una inspección física ni certificación Windows/multipantalla.

Monitor recompilado SHA `ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098` (reproducible frente a candidato anterior). CORE actualizado SHA arriba. Log original `/tmp/kylgis-kitchen-core-context-20261021.log`. Ningún .17/.18, producción, main, artefacto operativo ni configuración de POS alterados. Dependencias Hibernate4/Javassist y workaround add-opens continúan; cambiar ORM es una fase separada con regresión.
