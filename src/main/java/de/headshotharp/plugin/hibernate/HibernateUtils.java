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

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;

import de.headshotharp.plugin.hibernate.config.HibernateConfig;
import jakarta.persistence.Entity;

/**
 * Utitlity class to configure hibernate session factory. Can optionally scan
 * the classpath for hibernate entities annotated with
 * {@link jakarta.persistence.Entity @Entity}. Creates a connection pool using
 * HikariCP.
 */
public class HibernateUtils {

    private HibernateConfig hibernateConfig;
    private List<Class<?>> entityClasses;

    /**
     * Creates hibernate utility object and scans for entity classes in the package
     * given by the base class.
     *
     * @param hibernateConfig connection configuration
     * @param baseClass       base class to start entity scanning from
     */
    public HibernateUtils(HibernateConfig hibernateConfig, Class<?> baseClass) {
        this.hibernateConfig = hibernateConfig;
        entityClasses = scanEntitys(baseClass);
    }

    /**
     *
     * @param hibernateConfig connection configuration
     * @param entityClasses   list of entity classes
     */
    public HibernateUtils(HibernateConfig hibernateConfig, List<Class<?>> entityClasses) {
        this.hibernateConfig = hibernateConfig;
        this.entityClasses = entityClasses;
    }

    private List<Class<?>> scanEntitys(Class<?> baseClass) {
        Reflections reflections = new Reflections(baseClass.getPackageName());
        return reflections.getTypesAnnotatedWith(Entity.class).stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers())).toList();
    }

    /**
     * Creates a new session factory including a hikari connection pool.
     *
     * @return the created session factory
     */
    public SessionFactory createSessionFactory() {
        if (hibernateConfig == null) {
            throw new IllegalStateException("HibernateUtils has no config");
        }
        // boilerplate
        Configuration configuration = new Configuration();
        Properties properties = new Properties();
        // config mapper
        properties.put(AvailableSettings.DRIVER, hibernateConfig.getDriver());
        properties.put(AvailableSettings.URL, hibernateConfig.getUrl());
        properties.put(AvailableSettings.USER, hibernateConfig.getUsername());
        properties.put(AvailableSettings.PASS, hibernateConfig.getPassword());
        properties.put(AvailableSettings.DIALECT, hibernateConfig.getDialect());
        properties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
        properties.put(AvailableSettings.CONNECTION_PROVIDER, HikariCPConnectionProvider.class.getName());
        // boilerplate
        configuration.setProperties(properties);
        // configure entity classes
        entityClasses.forEach(configuration::addAnnotatedClass);
        // setup session factory
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();
        return configuration.buildSessionFactory(serviceRegistry);
    }
}
