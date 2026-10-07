/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
 Portions Copyright (c) 2015 John Lewis / Chromis

 Based on Chromis Kitchen Screen. Upstream attribution is retained under the
 GNU General Public License, version 3 or (at your option) any later version.

 KylGis POS Monitor de Cocina is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 KylGis POS Monitor de Cocina is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with KylGis POS Monitor de Cocina. If not, see <http://www.gnu.org/licenses/>.
 */

package com.mx.kylgis.kitchenscr.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author John
 */
public class AppConfig {

    private static AppConfig instance = null;
    private final Properties m_propsconfig;
    private final File configFile;
    private static final Logger logger = Logger.getLogger("com.mx.kylgis.kitchenscr.forms.AppConfig");

    protected AppConfig(File configFile) {
        this.configFile = configFile;
        m_propsconfig = new Properties();
        load();
        logger.log(Level.INFO, "Reading configuration file: {0}", configFile.getAbsolutePath());
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig(new File(System.getProperty("user.home"), AppLocal.APP_ID + ".properties"));
        }
        return instance;
    }

    private File getDefaultConfig() {
        return new File(new File(System.getProperty("user.home")), AppLocal.APP_ID + ".properties");
    }

    private File getLegacyConfig() {
        return new File(new File(System.getProperty("user.home")), AppLocal.LEGACY_APP_ID + ".properties");
    }

    private File getChromisLegacyConfig() {
        return new File(new File(System.getProperty("user.home")), AppLocal.LEGACY_CHROMIS_APP_ID + ".properties");
    }

    public String getDirPath() {
        String dirname = System.getProperty("dirname.path");
        return (dirname == null ? "./" : dirname);
    }

    public void setProperty(String sKey, String sValue) {
        if (sValue == null) {
            m_propsconfig.remove(sKey);
        } else {
            m_propsconfig.setProperty(sKey, sValue);
        }
    }

    public String getProperty(String sKey) {
        return m_propsconfig.getProperty(sKey);
    }

    public boolean delete() {
        loadDefault();
        return configFile.delete();
    }

    public void load() {
        loadDefault();
        File source = configFile;
        if (!source.isFile()) {
            File previousKylGisConfig = getLegacyConfig();
            File chromisConfig = getChromisLegacyConfig();
            if (previousKylGisConfig.isFile()) {
                source = previousKylGisConfig;
            } else if (chromisConfig.isFile()) {
                source = chromisConfig;
            }
        }
        boolean migratingLegacyConfig = !source.equals(configFile) && source.isFile();
        try (InputStream in = new FileInputStream(source)) {
            m_propsconfig.load(in);
            migrateLegacyProperties();

            if (migratingLegacyConfig) {
                logger.log(Level.INFO,
                        "Migrando configuración anterior del monitor de cocina a: {0}",
                        configFile.getAbsolutePath());
                save();
            }
        } catch (IOException e) {
            migrateLegacyProperties();
        }
    }

    private void migrateLegacyProperties() {
        migrateProperty("unicenta.config.enabled", "kylgis.pos.config.enabled");
        migrateProperty("unicenta.config", "kylgis.pos.config");
        m_propsconfig.remove("unicenta.config.enabled");
        m_propsconfig.remove("unicenta.config");
    }

    private void migrateProperty(String legacyKey, String newKey) {
        if (m_propsconfig.getProperty(newKey) == null
                && m_propsconfig.getProperty(legacyKey) != null) {
            m_propsconfig.setProperty(newKey, m_propsconfig.getProperty(legacyKey));
        }
    }

    public void save() throws IOException {
        OutputStream out = new FileOutputStream(configFile);
        if (out != null) {
            m_propsconfig.store(out, AppLocal.APP_NAME + ". Archivo de configuración.");
            out.close();
        }
    }

    private void loadDefault() {
        m_propsconfig.setProperty("db.engine", "MySql");
        m_propsconfig.setProperty("db.driver", "com.mysql.jdbc.Driver");
        m_propsconfig.setProperty("db.URL", "jdbc:mysql://localhost:3306/kylgis");
        m_propsconfig.setProperty("db.user", "");
        m_propsconfig.setProperty("db.password", "");
        m_propsconfig.setProperty("screen.displaynumber", "1");
        m_propsconfig.setProperty("db.dialect", "org.hibernate.dialect.MySQLDialect");
    
    }

}
