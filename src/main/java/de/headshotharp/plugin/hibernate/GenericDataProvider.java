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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import de.headshotharp.plugin.hibernate.config.HibernateConfig;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;

@Getter
public abstract class GenericDataProvider<T> {

    private SessionFactory sessionFactory;

    protected GenericDataProvider(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected GenericDataProvider(HibernateConfig hibernateConfig, Class<?> baseClass) {
        sessionFactory = new HibernateUtils(hibernateConfig, baseClass).createSessionFactory();
    }

    protected GenericDataProvider(HibernateConfig hibernateConfig, List<Class<?>> daoClasses) {
        sessionFactory = new HibernateUtils(hibernateConfig, daoClasses).createSessionFactory();
    }

    public abstract Class<T> getEntityClass();

    public List<T> getInTransaction(InTransactionExecutor<T> ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        List<T> ret = ite.executeInTransaction(session);
        transaction.commit();
        session.close();
        return ret;
    }

    public int execInTransaction(InTransactionExecutorVoid ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int changed = ite.executeInTransaction(session);
        transaction.commit();
        session.close();
        return changed;
    }

    public List<T> findAllByPredicate() {
        return findAllByPredicate(null);
    }

    public List<T> findAllByPredicate(PredicateProvider<T> predicateProvider) {
        return getInTransaction(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> criteria = builder.createQuery(getEntityClass());
            Root<T> root = criteria.from(getEntityClass());
            // set predicates
            List<Predicate> predicates = new LinkedList<>();
            if (predicateProvider != null) {
                predicateProvider.addPredicates(builder, criteria, root, predicates);
            }
            criteria.where(builder.and(predicates.toArray(new Predicate[0])));
            return session.createQuery(criteria).getResultList();
        });
    }

    public Optional<T> findByPredicate() {
        return findByPredicate(null);
    }

    public Optional<T> findByPredicate(PredicateProvider<T> predicateProvider) {
        List<T> resultList = findAllByPredicate(predicateProvider);
        if (resultList.size() == 1) {
            return Optional.of(resultList.get(0));
        } else {
            return Optional.empty();
        }
    }

    public void persist(T o) {
        execInTransaction(session -> {
            session.persist(o);
            return 1;
        });
    }

    public void delete(T o) {
        execInTransaction(session -> {
            session.remove(o);
            return 1;
        });
    }
}
