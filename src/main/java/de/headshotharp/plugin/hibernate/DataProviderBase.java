/**
 * Minecraft Plugin Hibernate
 * Copyright © 2022 headshotharp.de
 *
 * This file is part of Minecraft Plugin Hibernate.
 *
 * Minecraft Plugin Hibernate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Minecraft Plugin Hibernate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Minecraft Plugin Hibernate. If not, see <https://www.gnu.org/licenses/>.
 */
package de.headshotharp.plugin.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import de.headshotharp.plugin.hibernate.config.HibernateConfig;
import de.headshotharp.plugin.hibernate.dao.DataAccessObject;

public class DataProviderBase {

    private SessionFactory sessionFactory;

    public DataProviderBase(HibernateConfig hibernateConfig, Class<?> baseClass) {
        sessionFactory = new HibernateUtils(hibernateConfig, baseClass).createSessionFactory();
    }

    public DataProviderBase(HibernateConfig hibernateConfig, List<Class<? extends DataAccessObject>> daoClasses) {
        sessionFactory = new HibernateUtils(hibernateConfig, daoClasses).createSessionFactory();
    }

    public <T> T getInTransaction(InTransactionExecutor<T> ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        T ret = ite.executeInTransaction(session);
        transaction.commit();
        session.close();
        return ret;
    }

    public void execInTransaction(InTransactionExecutorVoid ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ite.executeInTransaction(session);
        transaction.commit();
        session.close();
    }

    public void persist(DataAccessObject o) {
        execInTransaction(session -> session.persist(o));
    }

    public void delete(DataAccessObject o) {
        execInTransaction(session -> session.remove(o));
    }
}
