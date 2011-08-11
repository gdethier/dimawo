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
package dimawo;

import dimawo.middleware.distributedAgent.DistributedAgent;

/**
 * Interface of the Master/Worker factory. A Master/Factory is used by
 * Distributed Agent (DA) to instantiate the Master Agent and the Worker Agents
 * that implement a DiMaWo application.
 * 
 * @see dimawo.MasterAgent
 * @see dimawo.WorkerAgent
 * @see dimawo.middleware.distributedAgent.DistributedAgent
 * 
 * @author Gerard Dethier
 */
public interface MasterWorkerFactory {
	/**
	 * Sets the parameters of the factory. This method is generally called by
	 * a launcher to provide the program parameters given by the user when
	 * deploying the DiMaWo application. The parameters are provided as an
	 * array of strings. Each string represents a parameter and might be parsed
	 * by the factory if necessary (i.e. when the expected parameters is not
	 * a string).
	 * <p>
	 * This method should parse the parameters and check that there is no error
	 * (wrong number of arguments, bad arguments type, etc.),
	 * then call another setter which takes the parsed parameters as argument
	 * and effectively sets factory's parameters.
	 * 
	 * @param factArgs An array of strings representing the arguments of the
	 * factory.
	 * 
	 * @throws DiMaWoException If the wrong number of arguments is given or
	 * arguments are invalid.
	 * 
	 * @see dimawo.exec
	 */
	public void setParameters(String[] factArgs) throws DiMaWoException;
	
	
	/**
	 * Instantiates a Worker Agent (WA). This method is called by the
	 * Distributed Agent (DA) upon its initialization.
	 * 
	 * @param distributedAgent The DA that instantiates the WA.
	 * 
	 * @return The instantiated WA.
	 * 
	 * @throws DiMaWoException If an error occurred during WA's instantiation.
	 */
	public WorkerAgent getWorkerAgent(DistributedAgent distributedAgent) throws DiMaWoException;
	
	
	/**
	 * Instantiates a Master Agent (MA). This method is called by the
	 * Distributed Agent (DA) upon its initialization.
	 * 
	 * @param distributedAgent The DA that instantiates the MA.
	 * 
	 * @return The instantiated MA.
	 * 
	 * @throws DiMaWoException If an error occurred during MA's instantiation.
	 */
	public MasterAgent getMasterAgent(DistributedAgent distributedAgent) throws DiMaWoException;
}
