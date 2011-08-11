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
 * This package contains the core classes of DiMaWo's simulator.
 * The simulator allows to execute a DiMaWo based application on a virtual
 * cluster. A
 * {@link dimawo.simulation.cluster.ClusterDescription cluster description}
 * and a
 * {@link dimawo.simulation.middleware.MiddlewareDescription middleware description}
 * must be provided
 * by the user. A simulation can then be executed, using
 * {@link dimawo.simulation.Simulation Simulation class},
 * given a particular
 * {@link dimawo.simulation.DeploymentDescription deployment description}
 * (which process on which host, jobs to be submitted to the
 * middleware), also provided by the user. 
 */
package dimawo.simulation;