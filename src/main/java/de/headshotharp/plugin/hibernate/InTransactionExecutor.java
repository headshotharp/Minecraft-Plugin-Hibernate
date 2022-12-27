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

@FunctionalInterface
public interface InTransactionExecutor<T> {

    public List<T> executeInTransaction(Session session);
}
