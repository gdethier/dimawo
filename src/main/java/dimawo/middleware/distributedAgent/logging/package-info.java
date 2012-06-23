/*
 * #%L
 * DiMaWo
 * %%
 * Copyright (C) 2011 DiMaWo Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
/**
 * Provides several implementations of a simple logging system that can be
 * used by Distributed Agent. Two implementations are provided:
 * <ul>
 * <li>A {@link dimawo.middleware.distributedAgent.logging.ConsoleLogger console logger}
 * which prints all messages on the standard output.</li>
 * <li>A {@link dimawo.middleware.distributedAgent.logging.NetworkLogger network logger}
 * which sends all messages to a remote host which handles them.</li>
 * </ul>
 */
package dimawo.middleware.distributedAgent.logging;
