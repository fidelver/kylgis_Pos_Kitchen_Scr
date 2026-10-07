package com.mx.kylgis.kitchenscr.config.provisioning;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Resolves DEFAULT < MASTER < NODE < local bootstrap for KitchenScreen. */
public final class NodeProvisioner {
    private NodeProvisioner() { }

    public static ProvisioningResult resolve(File bootstrap, Properties defaults) throws IOException {
        Properties local = loadIfExists(bootstrap);
        String masterValue = trim(local.getProperty("config.master"));
        if (masterValue == null) {
            Properties raw = merge(defaults, local);
            return new ProvisioningResult(raw, raw, local, null, bootstrap, false);
        }

        File master = resolveRelative(bootstrap.getParentFile(), masterValue);
        if (!master.isFile()) throw new IOException("KylGis MASTER not found: " + master.getAbsolutePath());
        Properties masterProps = load(master);
        String nodeId = trim(local.getProperty("node.id"));
        if (nodeId == null) throw new IOException("Provisioned KitchenScreen requires node.id in " + bootstrap);
        String moduleKey = "node." + nodeId + ".module";
        String moduleReference = trim(masterProps.getProperty(moduleKey));
        if (moduleReference == null) {
            throw new IOException("KylGis MASTER does not define " + moduleKey);
        }
        File nodeFile = resolveRelative(master.getParentFile(), moduleReference);
        if (!nodeFile.isFile()) throw new IOException("KylGis node module not found: " + nodeFile.getAbsolutePath());
        Properties node = load(nodeFile);

        Properties raw = merge(defaults, masterProps, node, local);
        raw.setProperty("config.provisioned", "true");
        Properties secrets = loadSecrets(master, raw);
        Properties effective = SecretResolver.resolve(raw, secrets);
        applyKitchenDatabaseCompatibility(effective);
        return new ProvisioningResult(raw, effective, node, master, nodeFile, true);
    }

    private static Properties loadSecrets(File master, Properties raw) throws IOException {
        String value = trim(raw.getProperty("config.secrets"));
        File file = resolveRelative(master.getParentFile(), value == null ? "secrets.properties" : value);
        if (!file.isFile()) {
            for (String key : raw.stringPropertyNames()) {
                if (SecretResolver.containsReference(raw.getProperty(key))) {
                    throw new IOException("KylGis secrets file not found: " + file.getAbsolutePath());
                }
            }
            return new Properties();
        }
        return load(file);
    }

    private static void applyKitchenDatabaseCompatibility(Properties p) {
        String server = trim(p.getProperty("database.server"));
        String name = trim(p.getProperty("database.name"));
        if (server == null && name == null) return;
        String port = value(p, "database.port", "3306");
        String options = value(p, "database.options", "");
        String db = name == null ? "kylgis" : name;
        p.setProperty("db.URL", "jdbc:mysql://" + (server == null ? "localhost" : server) + ":" + port + "/" + db + options);
        copy(p, "database.user", "db.user");
        copy(p, "database.password", "db.password");
        copy(p, "database.driver", "db.driver");
        copy(p, "database.dialect", "db.dialect");
        if (p.getProperty("db.driver") == null) p.setProperty("db.driver", "com.mysql.jdbc.Driver");
        if (p.getProperty("db.dialect") == null) p.setProperty("db.dialect", "org.hibernate.dialect.MySQLDialect");
        if (p.getProperty("db.engine") == null) p.setProperty("db.engine", "MySql");
    }

    private static void copy(Properties p, String from, String to) {
        String v = p.getProperty(from); if (v != null) p.setProperty(to, v);
    }
    private static String value(Properties p, String key, String fallback) {
        String v = trim(p.getProperty(key)); return v == null ? fallback : v;
    }
    private static Properties merge(Properties... all) {
        Properties out = new Properties();
        for (Properties p : all) if (p != null) out.putAll(p);
        return out;
    }
    private static Properties loadIfExists(File file) throws IOException {
        return file != null && file.isFile() ? load(file) : new Properties();
    }
    private static Properties load(File file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file)) { p.load(in); }
        return p;
    }
    private static File resolveRelative(File base, String path) {
        File f = new File(path); return f.isAbsolute() ? f : new File(base, path);
    }
    private static String trim(String s) {
        if (s == null) return null; s = s.trim(); return s.isEmpty() ? null : s;
    }
}
