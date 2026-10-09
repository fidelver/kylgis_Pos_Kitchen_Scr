/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
 Portions Copyright (c) 2015 John Lewis / Chromis

 Based on Chromis Kitchen Screen. Upstream attribution is retained under the
 GNU General Public License, version 3 or (at your option) any later version.
 */
package com.mx.kylgis.kitchenscr.forms;

import com.mx.kylgis.kitchenscr.config.provisioning.NodeProvisioner;
import com.mx.kylgis.kitchenscr.config.provisioning.ProvisioningResult;
import com.mx.kylgis.kitchenscr.config.provisioning.SecretResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppConfig {

    private static volatile AppConfig instance = null;
    private Properties m_propsconfig = new Properties();
    private final File configFile;
    private ProvisioningResult provisioningResult;
    private boolean externalRuntimeConfiguration;
    private final Set<String> dirtyProperties = new LinkedHashSet<>();
    private static final Logger logger = Logger.getLogger("com.mx.kylgis.kitchenscr.forms.AppConfig");

    protected AppConfig(File configFile) {
        this.configFile = configFile;
        load();
        logger.log(Level.INFO, "Reading configuration file: {0}", configFile.getAbsolutePath());
    }

    private AppConfig(File configFile, Properties effectiveRuntimeProperties) {
        if (configFile == null || effectiveRuntimeProperties == null) {
            throw new IllegalArgumentException("configFile and effectiveRuntimeProperties are required");
        }
        this.configFile = configFile;
        this.externalRuntimeConfiguration = true;
        this.m_propsconfig = copy(effectiveRuntimeProperties);
        migrateLegacyProperties();
        dirtyProperties.clear();
        logger.log(Level.INFO,
                "KitchenScreen using in-memory configuration supplied by KylGis runtime: {0}",
                configFile.getAbsolutePath());
    }

    public static synchronized void installRuntimeConfiguration(File configFile,
            Properties effectiveRuntimeProperties) {
        instance = new AppConfig(configFile, effectiveRuntimeProperties);
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    String explicit = System.getProperty("kylgis.config");
                    File file = explicit == null || explicit.trim().isEmpty()
                            ? new File(System.getProperty("user.home"), AppLocal.APP_ID + ".properties")
                            : new File(explicit.trim());
                    instance = new AppConfig(file);
                }
            }
        }
        return instance;
    }

    public static synchronized void resetForTests() {
        instance = null;
    }

    public File getConfigFile() { return configFile; }
    public boolean isProvisioned() {
        return externalRuntimeConfiguration
                || (provisioningResult != null && provisioningResult.isProvisioned());
    }
    public boolean isRuntimeManaged() { return externalRuntimeConfiguration; }

    public boolean isMasterNode() {
        String roles = getProperty("node.roles");
        if (roles == null) {
            return false;
        }
        for (String token : roles.split("[,;\\s]+")) {
            if ("master".equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    public File getMasterConfigFile() {
        return provisioningResult == null ? null : provisioningResult.getMasterFile();
    }
    public File getNodeModuleFile() {
        return provisioningResult == null ? null : provisioningResult.getNodeModuleFile();
    }

    private File getLegacyConfig() {
        return new File(System.getProperty("user.home"), AppLocal.LEGACY_APP_ID + ".properties");
    }

    private File getChromisLegacyConfig() {
        return new File(System.getProperty("user.home"), AppLocal.LEGACY_CHROMIS_APP_ID + ".properties");
    }

    public String getDirPath() {
        String dirname = System.getProperty("dirname.path");
        return dirname == null ? "./" : dirname;
    }

    public void setProperty(String key, String value) {
        if (value == null) m_propsconfig.remove(key); else m_propsconfig.setProperty(key, value);
        dirtyProperties.add(key);
    }

    public String getProperty(String key) { return m_propsconfig.getProperty(key); }

    public boolean delete() {
        if (isProvisioned()) {
            logger.log(Level.WARNING, "Factory reset disabled for provisioned KitchenScreen node: {0}", configFile);
            return false;
        }
        loadDefault();
        dirtyProperties.clear();
        return configFile.delete();
    }

    public final void load() {
        loadDefault();
        Properties defaults = copy(m_propsconfig);

        File source = configFile;
        if (!source.isFile()) {
            File previous = getLegacyConfig();
            File chromis = getChromisLegacyConfig();
            if (previous.isFile()) source = previous;
            else if (chromis.isFile()) source = chromis;
        }

        boolean migrating = !source.equals(configFile) && source.isFile();
        try {
            if (migrating) {
                Properties legacy = loadFile(source);
                m_propsconfig = merge(defaults, legacy);
                migrateLegacyProperties();
                dirtyProperties.clear();
                logger.log(Level.INFO, "Migrando configuración anterior del monitor de cocina a: {0}", configFile);
                save();
                return;
            }

            provisioningResult = NodeProvisioner.resolve(configFile, defaults);
            m_propsconfig = provisioningResult.getEffectiveProperties();
            migrateLegacyProperties();
            dirtyProperties.clear();
            if (isProvisioned()) {
                logger.log(Level.INFO, "KitchenScreen provisioned using MASTER {0} and node module {1}",
                        new Object[]{getMasterConfigFile(), getNodeModuleFile()});
            }
        } catch (IOException ex) {
            provisioningResult = null;
            dirtyProperties.clear();
            throw new IllegalStateException("Cannot resolve KitchenScreen configuration: " + configFile, ex);
        }
    }

    private void migrateLegacyProperties() {
        migrateProperty("unicenta.config.enabled", "kylgis.pos.config.enabled");
        migrateProperty("unicenta.config", "kylgis.pos.config");
        m_propsconfig.remove("unicenta.config.enabled");
        m_propsconfig.remove("unicenta.config");
    }

    private void migrateProperty(String legacyKey, String newKey) {
        if (m_propsconfig.getProperty(newKey) == null && m_propsconfig.getProperty(legacyKey) != null) {
            m_propsconfig.setProperty(newKey, m_propsconfig.getProperty(legacyKey));
        }
    }

    public void save() throws IOException {
        if (externalRuntimeConfiguration) {
            throw new IOException("KitchenScreen runtime-managed configuration is read-only; update the KylGis MASTER instead");
        }
        if (isProvisioned()) {
            saveNodeOverrides();
            return;
        }
        try (OutputStream out = new FileOutputStream(configFile)) {
            m_propsconfig.store(out, AppLocal.APP_NAME + ". Archivo de configuración.");
        }
        dirtyProperties.clear();
    }

    private void saveNodeOverrides() throws IOException {
        if (dirtyProperties.isEmpty()) return;
        Properties raw = provisioningResult.getRawProperties();
        Properties node = provisioningResult.getNodeProperties();
        for (String key : dirtyProperties) {
            String rawValue = raw.getProperty(key);
            String value = m_propsconfig.getProperty(key);
            if (SecretResolver.containsReference(rawValue) && !SecretResolver.containsReference(value)) {
                throw new IOException("Refusing to persist resolved secret for property: " + key);
            }
            if (value == null) node.remove(key); else node.setProperty(key, value);
        }
        try (OutputStream out = new FileOutputStream(getNodeModuleFile())) {
            node.store(out, AppLocal.APP_NAME + ". Node module.");
        }
        load();
    }

    private void loadDefault() {
        m_propsconfig = new Properties();
        m_propsconfig.setProperty("db.engine", "MySql");
        m_propsconfig.setProperty("db.driver", "com.mysql.jdbc.Driver");
        m_propsconfig.setProperty("db.URL", "jdbc:mysql://localhost:3306/kylgis");
        m_propsconfig.setProperty("db.user", "");
        m_propsconfig.setProperty("db.password", "");
        m_propsconfig.setProperty("screen.displaynumber", "1");
        m_propsconfig.setProperty("screen.refreshinterval", "10000");
        m_propsconfig.setProperty("screen.refreshbackoffmax", "60000");
        m_propsconfig.setProperty("db.show_sql", "false");
        m_propsconfig.setProperty("monitor.readonly", "false");
        m_propsconfig.setProperty("db.pool.min", "1");
        m_propsconfig.setProperty("db.pool.max", "2");
        m_propsconfig.setProperty("db.pool.maxidle", "300");
        m_propsconfig.setProperty("db.pool.idletestperiod", "60");
        m_propsconfig.setProperty("db.pool.checkouttimeout", "3000");
        m_propsconfig.setProperty("db.network.connecttimeout", "5000");
        m_propsconfig.setProperty("db.network.sockettimeout", "5000");
        m_propsconfig.setProperty("db.dialect", "org.hibernate.dialect.MySQLDialect");
    }

    private static Properties loadFile(File file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file)) { p.load(in); }
        return p;
    }

    private static Properties copy(Properties source) {
        Properties p = new Properties(); if (source != null) p.putAll(source); return p;
    }

    private static Properties merge(Properties... all) {
        Properties p = new Properties(); for (Properties x : all) if (x != null) p.putAll(x); return p;
    }
}
