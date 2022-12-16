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
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;

import de.headshotharp.plugin.hibernate.config.HibernateConfig;
import jakarta.persistence.Entity;

public class HibernateUtils {

    private HibernateConfig hibernateConfig;
    private List<Class<?>> daoClasses;

    public HibernateUtils(HibernateConfig hibernateConfig, Class<?> baseClass) {
        this.hibernateConfig = hibernateConfig;
        daoClasses = scanEntitys(baseClass);
    }

    public HibernateUtils(HibernateConfig hibernateConfig, List<Class<?>> daoClasses) {
        this.hibernateConfig = hibernateConfig;
        this.daoClasses = daoClasses;
    }

    private List<Class<?>> scanEntitys(Class<?> baseClass) {
        Reflections reflections = new Reflections(baseClass.getPackageName());
        return reflections.getTypesAnnotatedWith(Entity.class).stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers())).toList();
    }

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
        properties.put(AvailableSettings.CONNECTION_PROVIDER, "org.hibernate.connection.C3P0ConnectionProvider");
        properties.put(AvailableSettings.C3P0_MIN_SIZE, "5");
        properties.put(AvailableSettings.C3P0_MAX_SIZE, "20");
        properties.put(AvailableSettings.C3P0_ACQUIRE_INCREMENT, "5");
        properties.put(AvailableSettings.C3P0_TIMEOUT, "600");
        // boilerplate
        configuration.setProperties(properties);
        // configure entity classes
        daoClasses.forEach(configuration::addAnnotatedClass);
        // setup session factory
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();
        return configuration.buildSessionFactory(serviceRegistry);
    }
}
