/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
*/
package com.mx.kylgis.kitchenscr.runtime;

import com.mx.kylgis.kitchenscr.KitchenScr;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.runtime.CapabilityType;
import com.mx.kylgis.pos.runtime.RuntimeCapability;
import com.mx.kylgis.pos.runtime.RuntimeHandle;
import com.mx.kylgis.pos.runtime.RuntimeLaunchContext;
import java.io.File;
import javafx.application.Platform;

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
        File config = context.getConfig().getConfigFile();
        if (config == null || !config.isFile()) {
            throw new IllegalStateException("Kitchen capability requires an existing node bootstrap/config file");
        }
    }

    @Override
    public RuntimeHandle start(final RuntimeLaunchContext context, boolean daemon) {
        final String configPath = context.getConfig().getConfigFile().getAbsolutePath();
        System.setProperty("kylgis.config", configPath);

        Thread kitchenThread = new Thread(new Runnable() {
            @Override public void run() {
                KitchenScr.main(new String[0]);
            }
        }, "kylgis-runtime-kitchen");
        kitchenThread.setContextClassLoader(KitchenRuntimeCapability.class.getClassLoader());
        kitchenThread.setDaemon(daemon);
        kitchenThread.start();

        return new RuntimeHandle() {
            @Override public void close() {
                try {
                    Platform.runLater(new Runnable() {
                        @Override public void run() { Platform.exit(); }
                    });
                } catch (IllegalStateException ignored) {
                    // JavaFX toolkit already stopped.
                }
            }
        };
    }
}
