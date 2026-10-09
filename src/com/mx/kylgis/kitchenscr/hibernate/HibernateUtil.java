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

package com.mx.kylgis.kitchenscr.hibernate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import com.mx.kylgis.kitchenscr.dto.Orders;

import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.utils.AltEncrypter;

public class HibernateUtil {

    private static SessionFactory sessionFactory = buildSessionFactory();
    private static ServiceRegistry serviceRegistry;

    private static SessionFactory buildSessionFactory() {

        AppConfig appConfig = AppConfig.getInstance();

        String sDBDriver = appConfig.getProperty("db.driver");
        String sDBURL = appConfig.getProperty("db.URL");
        String sDBUser = appConfig.getProperty("db.user");
        String sDBPassword = appConfig.getProperty("db.password");
        String sDBDialect = appConfig.getProperty("db.dialect");

        // Provisioned KylGis nodes keep the historical POS JDBC pieces
        // separate (db.URL + db.schema + db.options). Kitchen/Hibernate needs
        // one complete JDBC URL. Local legacy Kitchen configs already store a
        // complete db.URL, so only assemble the pieces in provisioned mode.
        if (appConfig.isProvisioned()) {
            String schema = appConfig.getProperty("db.schema");
            String options = appConfig.getProperty("db.options");
            sDBURL = valueOrEmpty(sDBURL)
                    + valueOrEmpty(schema)
                    + valueOrEmpty(options);
        }

        boolean useKylGisPosConfig = Boolean.parseBoolean(
                appConfig.getProperty("kylgis.pos.config.enabled"));

        if (useKylGisPosConfig) {
            String kylgisPosConfigPath =
                    appConfig.getProperty("kylgis.pos.config");

            if (kylgisPosConfigPath == null
                    || kylgisPosConfigPath.trim().isEmpty()) {
                throw new IllegalStateException(
                        "KylGis POS configuration is enabled but no properties file was specified.");
            }

            File kylgisPosFile = new File(kylgisPosConfigPath.trim());

            if (!kylgisPosFile.isFile()) {
                throw new IllegalStateException(
                        "KylGis POS properties file not found: "
                        + kylgisPosFile.getAbsolutePath());
            }

            Properties kylgisPosProps = new Properties();

            try (FileInputStream input =
                    new FileInputStream(kylgisPosFile)) {
                kylgisPosProps.load(input);
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Could not read KylGis POS properties file: "
                        + kylgisPosFile.getAbsolutePath(), ex);
            }

            sDBDriver = kylgisPosProps.getProperty("db.driver", sDBDriver);
            sDBUser = kylgisPosProps.getProperty("db.user", sDBUser);
            sDBPassword = kylgisPosProps.getProperty("db.password", sDBPassword);

            String baseURL = kylgisPosProps.getProperty("db.URL", "");
            String schema = kylgisPosProps.getProperty("db.schema", "");
            String options = kylgisPosProps.getProperty("db.options", "");

            if (baseURL.isEmpty()) {
                throw new IllegalStateException(
                        "KylGis POS properties file does not contain db.URL.");
            }

            sDBURL = baseURL + schema + options;

            System.out.println(
                    "Using KylGis POS configuration: "
                    + kylgisPosFile.getAbsolutePath());
            System.out.println(
                    "KylGis POS database URL: " + sDBURL);
            System.out.println(
                    "KylGis POS database user: " + sDBUser);
        }

        if (sDBUser != null && sDBPassword != null
                && sDBPassword.startsWith("crypt:")) {
            AltEncrypter cypher = new AltEncrypter("cypherkey" + sDBUser);
            sDBPassword = cypher.decrypt(sDBPassword.substring(6));
        }

        Configuration configuration = new Configuration();

        // Set up the database details ready for the connections
        configuration.setProperty(
                "hibernate.connection.driver_class", sDBDriver);
        configuration.setProperty(
                "hibernate.connection.url", sDBURL);
        configuration.setProperty(
                "hibernate.connection.username", sDBUser);
        configuration.setProperty(
                "hibernate.connection.password", sDBPassword);
        configuration.setProperty(
                "hibernate.dialect", sDBDialect);
        // Use c3p0 instead of Hibernate's minimal built-in pool. Kitchen keeps
        // only one or two physical connections and periodically validates idle
        // sockets with SELECT 1. This discards connections closed by MariaDB
        // (wait_timeout, restart or a transient network outage) without adding
        // a validation query to every 10-second screen refresh.
        configuration.setProperty(
                "hibernate.connection.provider_class",
                "org.hibernate.c3p0.internal.C3P0ConnectionProvider");
        configuration.setProperty("hibernate.c3p0.min_size",
                valueOrDefault(appConfig.getProperty("db.pool.min"), "1"));
        configuration.setProperty("hibernate.c3p0.max_size",
                valueOrDefault(appConfig.getProperty("db.pool.max"), "2"));
        configuration.setProperty("hibernate.c3p0.acquire_increment", "1");
        configuration.setProperty("hibernate.c3p0.timeout",
                valueOrDefault(appConfig.getProperty("db.pool.maxidle"), "300"));
        configuration.setProperty("hibernate.c3p0.idle_test_period",
                valueOrDefault(appConfig.getProperty("db.pool.idletestperiod"), "60"));
        configuration.setProperty("hibernate.c3p0.preferredTestQuery", "SELECT 1");
        configuration.setProperty("hibernate.c3p0.checkoutTimeout",
                valueOrDefault(appConfig.getProperty("db.pool.checkouttimeout"), "3000"));
        configuration.setProperty("hibernate.c3p0.acquireRetryAttempts", "2");
        configuration.setProperty("hibernate.c3p0.acquireRetryDelay", "1000");
        configuration.setProperty("hibernate.c3p0.breakAfterAcquireFailure", "false");

        // Bound MySQL network stalls as well as pool checkout time. These are
        // JDBC driver properties, not automatic SQL retries.
        if (sDBDriver != null && sDBDriver.toLowerCase().contains("mysql")) {
            configuration.setProperty("hibernate.connection.connectTimeout",
                    valueOrDefault(appConfig.getProperty("db.network.connecttimeout"), "5000"));
            configuration.setProperty("hibernate.connection.socketTimeout",
                    valueOrDefault(appConfig.getProperty("db.network.sockettimeout"), "5000"));
        }

        //configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("hibernate.show_sql",
                valueOrDefault(appConfig.getProperty("db.show_sql"), "false"));

        configuration.addAnnotatedClass(Orders.class);

        serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        try {
            return configuration.buildSessionFactory(serviceRegistry);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (serviceRegistry != null) {
                try {
                    StandardServiceRegistryBuilder.destroy(serviceRegistry);
                } catch (Exception ignored) {
                    // Preserve the original connection error.
                }
                serviceRegistry = null;
            }
            return null;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public static synchronized SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Opens and closes a short-lived session to verify that the configured
     * database is actually reachable without leaking a pooled connection.
     */
    public static synchronized boolean testConnection() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            return false;
        }

        Session testSession = null;
        try {
            testSession = sessionFactory.openSession();
            // Opening a Hibernate Session alone does not guarantee a JDBC
            // round-trip. Execute a harmless query so startup/configuration
            // checks prove that MariaDB is actually reachable.
            Object result = testSession.createSQLQuery("SELECT 1").uniqueResult();
            return result != null;
        } catch (Exception ex) {
            return false;
        } finally {
            if (testSession != null && testSession.isOpen()) {
                testSession.close();
            }
        }
    }

    /**
     * Rebuilds Hibernate after configuration changes or a transient startup
     * failure. The previous factory and registry are released first.
     */
    public static synchronized boolean rebuildSessionFactory() {
        shutdown();
        sessionFactory = buildSessionFactory();
        return testConnection();
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            try {
                if (!sessionFactory.isClosed()) {
                    sessionFactory.close();
                }
            } catch (Exception ignored) {
                // Continue releasing the registry.
            }
            sessionFactory = null;
        }

        if (serviceRegistry != null) {
            try {
                StandardServiceRegistryBuilder.destroy(serviceRegistry);
            } catch (Exception ignored) {
                // Nothing else to release here.
            }
            serviceRegistry = null;
        }
    }
}
