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

/**
 * Abstract class to create specific data providers for given entity classes
 *
 * @param <T> Entity class for the implementing data provider
 */
@Getter
public abstract class GenericDataProvider<T> {

    private SessionFactory sessionFactory;

    /**
     * Create GenericDataProvider with given SessionFactory
     *
     * @param sessionFactory given SessionFactory
     */
    protected GenericDataProvider(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Create GenericDataProvider and create new HikariCP connection pool for the
     * required SessionFactory. Scans the classpath below given baseClass
     * for @Entity classes.
     *
     * @param hibernateConfig config for hibernate
     * @param baseClass       base class to start scanning for entity classes
     */
    protected GenericDataProvider(HibernateConfig hibernateConfig, Class<?> baseClass) {
        sessionFactory = new HibernateUtils(hibernateConfig, baseClass).createSessionFactory();
    }

    /**
     * Create GenericDataProvider and create new HikariCP connection pool for the
     * required SessionFactory. Uses the given entity classes.
     *
     * @param hibernateConfig config for hibernate
     * @param daoClasses      given entity classes
     */
    protected GenericDataProvider(HibernateConfig hibernateConfig, List<Class<?>> daoClasses) {
        sessionFactory = new HibernateUtils(hibernateConfig, daoClasses).createSessionFactory();
    }

    /**
     * Specifies the entity class of this data provider
     *
     * @return entity class
     */
    public abstract Class<T> getEntityClass();

    /**
     * Executes the given executor in transaction and returns the resulting entity
     * list.
     *
     * @param ite the executor
     * @return entity list returned by the executor
     */
    public List<T> getInTransaction(InTransactionExecutor<T> ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        List<T> ret = ite.executeInTransaction(session);
        transaction.commit();
        session.close();
        return ret;
    }

    /**
     * Executes the given executor in transaction and returns the amount of changed
     * rows.
     *
     * @param ite the executor
     * @return the amount of changed rows
     */
    public int execInTransaction(InTransactionExecutorVoid ite) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int changed = ite.executeInTransaction(session);
        transaction.commit();
        session.close();
        return changed;
    }

    /**
     * Finds all entitys.
     *
     * @return list of all entities
     */
    public List<T> findAll() {
        return findAllByPredicate(null);
    }

    /**
     * Find all entitys in transaction filtered by the given predicate provider
     *
     * @param predicateProvider filter for results
     * @return list of found entities
     */
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

    /**
     * Find unique entity in transaction filtered by the given predicate provider.
     * Returns empty if none or multiple results are found.
     *
     * @param predicateProvider filter for results
     * @return found entity
     */
    public Optional<T> findByPredicate(PredicateProvider<T> predicateProvider) {
        List<T> resultList = findAllByPredicate(predicateProvider);
        if (resultList.size() == 1) {
            return Optional.of(resultList.get(0));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Persists the given object
     *
     * @param t Object to persist
     */
    public void persist(T t) {
        execInTransaction(session -> {
            session.persist(t);
            return 1;
        });
    }

    /**
     * Deletes the given entity object
     *
     * @param t Entity to delete
     */
    public void delete(T t) {
        execInTransaction(session -> {
            session.remove(t);
            return 1;
        });
    }
}
