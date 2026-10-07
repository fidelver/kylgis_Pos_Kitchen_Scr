package com.mx.kylgis.kitchenscr.config.provisioning;

import java.io.File;
import java.util.Properties;

public final class ProvisioningResult {
    private final Properties raw;
    private final Properties effective;
    private final Properties node;
    private final File master;
    private final File nodeFile;
    private final boolean provisioned;

    public ProvisioningResult(Properties raw, Properties effective, Properties node,
            File master, File nodeFile, boolean provisioned) {
        this.raw = copy(raw);
        this.effective = copy(effective);
        this.node = copy(node);
        this.master = master;
        this.nodeFile = nodeFile;
        this.provisioned = provisioned;
    }

    public Properties getRawProperties() { return copy(raw); }
    public Properties getEffectiveProperties() { return copy(effective); }
    public Properties getNodeProperties() { return copy(node); }
    public File getMasterFile() { return master; }
    public File getNodeModuleFile() { return nodeFile; }
    public boolean isProvisioned() { return provisioned; }

    private static Properties copy(Properties p) {
        Properties c = new Properties();
        if (p != null) c.putAll(p);
        return c;
    }
}
