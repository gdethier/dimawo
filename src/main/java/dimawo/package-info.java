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
 * Contains helper classes for reflection as well as the core base classes of
 * a DiMaWo application.
 * <p>
 * The helper classes for reflection essentially simplifies the instantiation
 * of objects using reflection mechanism.
 * <p>
 * There are at least 3 classes or interfaces a DiMaWo the user must subclass:
 * <ul>
 * <li>{@link be.ulg.montefiore.dimawo.MasterAgent MasterAgent}: implements
 * the master process.</li>
 * <li>{@link be.ulg.montefiore.dimawo.WorkerAgent WorkerAgent}: implements
 * the worker process.</li>
 * <li>{@link be.ulg.montefiore.dimawo.MasterWorkerFactory MasterWorkerFactory}:
 * instantiates the two previous agents, possibly in function of given
 * parameters (depends on user's implementation).</li>
 * </ul>
 */
package dimawo;