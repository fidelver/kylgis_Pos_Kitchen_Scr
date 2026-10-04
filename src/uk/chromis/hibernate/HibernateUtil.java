/*
 Chromis POS  - The New Face of Open Source POS
 Copyright (c) 2015 (John Lewis) Chromis.co.uk

 http://www.chromis.co.uk

 kitchen Screen v1.5

 This file is part of chromis & its associated programs

 chromis is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chromis is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with chromis.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.chromis.hibernate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import uk.chromis.dto.Orders;

import uk.chromis.forms.AppConfig;
import uk.chromis.utils.AltEncrypter;

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

        boolean useUnicentaConfig = Boolean.parseBoolean(
                appConfig.getProperty("unicenta.config.enabled"));

        if (useUnicentaConfig) {
            String unicentaConfigPath =
                    appConfig.getProperty("unicenta.config");

            if (unicentaConfigPath == null
                    || unicentaConfigPath.trim().isEmpty()) {
                throw new IllegalStateException(
                        "uniCenta configuration is enabled but no properties file was specified.");
            }

            File unicentaFile = new File(unicentaConfigPath.trim());

            if (!unicentaFile.isFile()) {
                throw new IllegalStateException(
                        "uniCenta properties file not found: "
                        + unicentaFile.getAbsolutePath());
            }

            Properties unicentaProps = new Properties();

            try (FileInputStream input =
                    new FileInputStream(unicentaFile)) {
                unicentaProps.load(input);
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Could not read uniCenta properties file: "
                        + unicentaFile.getAbsolutePath(), ex);
            }

            sDBDriver = unicentaProps.getProperty("db.driver", sDBDriver);
            sDBUser = unicentaProps.getProperty("db.user", sDBUser);
            sDBPassword = unicentaProps.getProperty("db.password", sDBPassword);

            String baseURL = unicentaProps.getProperty("db.URL", "");
            String schema = unicentaProps.getProperty("db.schema", "");
            String options = unicentaProps.getProperty("db.options", "");

            if (baseURL.isEmpty()) {
                throw new IllegalStateException(
                        "uniCenta properties file does not contain db.URL.");
            }

            sDBURL = baseURL + schema + options;

            System.out.println(
                    "Using uniCenta configuration: "
                    + unicentaFile.getAbsolutePath());
            System.out.println(
                    "uniCenta database URL: " + sDBURL);
            System.out.println(
                    "uniCenta database user: " + sDBUser);
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
        /*
         // Set up connection pooling to use c3p0 rather than hibernates built in pooling
         configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
         // configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider");
       
         configuration.setProperty("hibernate.c3p0.initialPoolSize", "5");
         configuration.setProperty("hibernate.c3p0.min", "5");
         configuration.setProperty("hibernate.c3p0.max", "10");
         configuration.setProperty("hibernate.c3p0.timeout", "5000");
         configuration.setProperty("hibernate.c3p0.max_statements", "30");
         configuration.setProperty("hibernate.c3p0.idle_test_period", "300");
         configuration.setProperty("hibernate.c3p0.aquire_increment", "2");
         */
        //configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("hibernate.show_sql", "true");
        configuration.setProperty("hibernate.connection.pool_size", "5");

        configuration.addAnnotatedClass(Orders.class);

        serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        try {
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Exception ex) {
            return null;
        }

        return sessionFactory;

    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
