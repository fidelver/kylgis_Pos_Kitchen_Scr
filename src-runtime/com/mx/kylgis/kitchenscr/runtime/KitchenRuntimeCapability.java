/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
*/
package com.mx.kylgis.kitchenscr.runtime;

import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.runtime.CapabilityType;
import com.mx.kylgis.pos.runtime.RuntimeCapability;
import com.mx.kylgis.pos.runtime.RuntimeHandle;
import com.mx.kylgis.pos.runtime.RuntimeLaunchContext;
import java.io.File;

/** Exposes KitchenScreen as a modular KylGis runtime capability. */
public final class KitchenRuntimeCapability implements RuntimeCapability {

    @Override public NodeRole getRole() { return NodeRole.KITCHEN; }
    @Override public CapabilityType getType() { return CapabilityType.UI; }

    @Override
    public void validate(RuntimeLaunchContext context) {
        if (context.getNodeContext().hasRole(NodeRole.POS)) {
            throw new IllegalStateException(
                    "Kitchen and POS cannot share one JVM yet; use separate nodes or processes");
        }
        if (!javaFxAvailable()) {
            throw new IllegalStateException(
                    "Kitchen capability requires JavaFX. Use a Java 8 distribution with JavaFX compatible with this operating system and CPU architecture.");
        }
        File config = context.getConfig().getConfigFile();
        if (config == null || !config.isFile()) {
            throw new IllegalStateException("Kitchen capability requires an existing node bootstrap/config file");
        }
    }

    private static boolean javaFxAvailable() {
        try {
            Class.forName("javafx.application.Application", false,
                    KitchenRuntimeCapability.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return systemModulePresent("javafx.graphics");
        }
    }

    private static boolean systemModulePresent(String moduleName) {
        try {
            // Compile with Java 8, but recognize Java 9+ system modules at runtime.
            // ModuleFinder.ofSystem() inspects only trusted runtime modules and does
            // not load any KylGis bundle class before integrity verification.
            Class<?> finderType = Class.forName("java.lang.module.ModuleFinder");
            Object finder = finderType.getMethod("ofSystem").invoke(null);
            Object optional = finderType.getMethod("find", String.class).invoke(finder, moduleName);
            Object present = optional.getClass().getMethod("isPresent").invoke(optional);
            return Boolean.TRUE.equals(present);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public RuntimeHandle start(final RuntimeLaunchContext context, boolean daemon) {
        final String configPath = context.getConfig().getConfigFile().getAbsolutePath();
        AppConfig.installRuntimeConfiguration(context.getConfig().getConfigFile(),
                context.getConfig().getPropertiesSnapshot());
        System.setProperty("kylgis.config", configPath);

        Thread kitchenThread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Class<?> kitchen = Class.forName("com.mx.kylgis.kitchenscr.KitchenScr", true,
                            KitchenRuntimeCapability.class.getClassLoader());
                    kitchen.getMethod("main", String[].class).invoke(null,
                            (Object) new String[0]);
                } catch (Exception | LinkageError ex) {
                    throw new IllegalStateException("Cannot start Kitchen capability", ex);
                }
            }
        }, "kylgis-runtime-kitchen");
        kitchenThread.setContextClassLoader(KitchenRuntimeCapability.class.getClassLoader());
        kitchenThread.setDaemon(daemon);
        kitchenThread.start();

        return new RuntimeHandle() {
            @Override public void close() {
                try {
                    final Class<?> platform = Class.forName("javafx.application.Platform", false,
                            KitchenRuntimeCapability.class.getClassLoader());
                    Runnable exit = new Runnable() {
                        @Override public void run() {
                            try {
                                platform.getMethod("exit").invoke(null);
                            } catch (Exception ignored) { }
                        }
                    };
                    platform.getMethod("runLater", Runnable.class).invoke(null, exit);
                } catch (Exception | LinkageError ignored) {
                    // JavaFX toolkit is unavailable or already stopped.
                }
            }
        };
    }
}
