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

package com.mx.kylgis.kitchenscr.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import com.mx.kylgis.kitchenscr.dto.Orders;
import com.mx.kylgis.kitchenscr.forms.AppConfig;
import com.mx.kylgis.kitchenscr.hibernate.HibernateUtil;

public class DataLogicKitchen {

    private Session openSession() {
        SessionFactory factory = HibernateUtil.getSessionFactory();
        if (factory == null || factory.isClosed()) {
            throw new IllegalStateException("No hay una conexión disponible con la base de datos.");
        }
        return factory.openSession();
    }

    private void closeSession(Session currentSession) {
        if (currentSession != null && currentSession.isOpen()) {
            currentSession.close();
        }
    }

    private void rollback(Transaction transaction) {
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (Exception ignored) {
                // Preserve the original database exception.
            }
        }
    }

    private void ensureWriteAllowed() {
        if (Boolean.parseBoolean(AppConfig.getInstance().getProperty("monitor.readonly"))) {
            throw new IllegalStateException(
                    "Monitor de Cocina en modo solo lectura: operación de escritura bloqueada.");
        }
    }

    public List<String> readDistinctOrders() {
        String sqlQuery;
        if (Boolean.valueOf(AppConfig.getInstance().getProperty("screen.allorders"))) {
            sqlQuery = "SELECT DISTINCT ORDERID, ORDERTIME FROM orders ORDER BY ORDERTIME ";
        } else {
            sqlQuery = "SELECT DISTINCT ORDERID, ORDERTIME FROM orders WHERE DISPLAYID = "
                    + Integer.parseInt(AppConfig.getInstance().getProperty("screen.displaynumber"))
                    + " ORDER BY ORDERTIME";
        }

        Session readSession = openSession();
        try {
            SQLQuery currentQuery = readSession.createSQLQuery(sqlQuery);
            currentQuery.addScalar("ORDERID");
            List results = currentQuery.list();
            return new ArrayList<String>(new LinkedHashSet<String>(results));
        } finally {
            closeSession(readSession);
        }
    }

    /**
     * Load all orders for the current display in a single query.
     * Orders are returned in their original insertion order inside each send.
     */
    public List<Orders> selectAllOrders() {
        String sqlQuery;
        if (Boolean.valueOf(AppConfig.getInstance().getProperty("screen.allorders"))) {
            sqlQuery = "SELECT * FROM orders ORDER BY ORDERTIME, ID ";
        } else {
            sqlQuery = "SELECT * FROM orders WHERE DISPLAYID = "
                    + Integer.parseInt(AppConfig.getInstance().getProperty("screen.displaynumber"))
                    + " ORDER BY ORDERTIME, ID ";
        }

        Session readSession = openSession();
        try {
            SQLQuery currentQuery = readSession.createSQLQuery(sqlQuery);
            currentQuery.addEntity(Orders.class);
            return currentQuery.list();
        } finally {
            closeSession(readSession);
        }
    }

    public void removeOrder(java.sql.Timestamp completetime) {
        ensureWriteAllowed();
        Session writeSession = null;
        Transaction transaction = null;
        try {
            writeSession = openSession();
            transaction = writeSession.beginTransaction();

            String sql = "DELETE FROM orders WHERE COMPLETETIME = :completetime "
                    + "AND DISPLAYID = :display";
            SQLQuery currentQuery = writeSession.createSQLQuery(sql);
            currentQuery.setParameter("completetime",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                            .format(completetime));
            currentQuery.setParameter("display",
                    Integer.parseInt(AppConfig.getInstance().getProperty("screen.displaynumber")));
            currentQuery.executeUpdate();
            transaction.commit();
        } catch (RuntimeException ex) {
            rollback(transaction);
            throw ex;
        } finally {
            closeSession(writeSession);
        }
    }

    public void removeAllOrders() {
        ensureWriteAllowed();
        Session writeSession = null;
        Transaction transaction = null;
        try {
            writeSession = openSession();
            transaction = writeSession.beginTransaction();
            Query currentQuery = writeSession.createQuery("DELETE FROM ORDERS ");
            currentQuery.executeUpdate();
            transaction.commit();
        } catch (RuntimeException ex) {
            rollback(transaction);
            throw ex;
        } finally {
            closeSession(writeSession);
        }
    }

    /**
     * Remove all orders for current display only.
     */
    public void removeAllOrdersDisplay() {
        ensureWriteAllowed();
        Session writeSession = null;
        Transaction transaction = null;
        try {
            writeSession = openSession();
            transaction = writeSession.beginTransaction();
            Query currentQuery = writeSession.createQuery(
                    "DELETE FROM ORDERS WHERE DISPLAYID = :display");
            currentQuery.setParameter("display",
                    Integer.parseInt(AppConfig.getInstance().getProperty("screen.displaynumber")));
            currentQuery.executeUpdate();
            transaction.commit();
        } catch (RuntimeException ex) {
            rollback(transaction);
            throw ex;
        } finally {
            closeSession(writeSession);
        }
    }

    public List<Orders> selectByOrderId(String orderid) {
        String sqlQuery;
        if (Boolean.valueOf(AppConfig.getInstance().getProperty("screen.allorders"))) {
            sqlQuery = "SELECT * FROM orders WHERE ORDERID = :orderid ORDER BY ID ";
        } else {
            sqlQuery = "SELECT * FROM orders WHERE ORDERID = :orderid AND DISPLAYID = :display ORDER BY ID ";
        }

        Session readSession = openSession();
        try {
            SQLQuery currentQuery = readSession.createSQLQuery(sqlQuery);
            currentQuery.setParameter("orderid", orderid);
            if (!Boolean.valueOf(AppConfig.getInstance().getProperty("screen.allorders"))) {
                currentQuery.setParameter("display",
                        Integer.parseInt(AppConfig.getInstance().getProperty("screen.displaynumber")));
            }
            currentQuery.addEntity(Orders.class);
            return currentQuery.list();
        } finally {
            closeSession(readSession);
        }
    }

    /* N Deppe Sept 2015 - Added to be able to create new order records for recall function */
    public void createOrder(Orders orderData) {
        ensureWriteAllowed();
        Session writeSession = null;
        Transaction transaction = null;
        try {
            writeSession = openSession();
            transaction = writeSession.beginTransaction();

            String sqlQuery = "INSERT INTO orders (ORDERID, QTY, DETAILS, ATTRIBUTES, NOTES, TICKETID, ORDERTIME, DISPLAYID, AUXILIARY, COMPLETETIME)"
                    + " VALUES ( :orderid, :qty, :details, :attributes, :notes, :ticketid, :ordertime, :displayid, :auxiliaryid, :completetime )";
            Query currentQuery = writeSession.createSQLQuery(sqlQuery);
            currentQuery.setParameter("orderid", orderData.getOrderid());
            currentQuery.setParameter("qty", orderData.getQty());
            currentQuery.setParameter("details", orderData.getDetails());
            currentQuery.setParameter("attributes", orderData.getAttributes());
            currentQuery.setParameter("notes", orderData.getNotes());
            currentQuery.setParameter("ticketid", orderData.getTicketid());
            currentQuery.setParameter("ordertime", orderData.getOrdertime());
            currentQuery.setParameter("displayid", orderData.getDisplayid());
            currentQuery.setParameter("auxiliaryid", orderData.getAuxiliary());
            currentQuery.setParameter("completetime",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                            .format(orderData.getCompletetime()));
            currentQuery.executeUpdate();
            transaction.commit();
        } catch (RuntimeException ex) {
            rollback(transaction);
            throw ex;
        } finally {
            closeSession(writeSession);
        }
    }
}
