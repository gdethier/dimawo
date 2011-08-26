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
package be.ulg.montefiore.dimawo;

import dimawo.DiMaWoException;
import dimawo.MasterAgent;
import dimawo.MasterWorkerFactory;
import dimawo.WorkerAgent;
import dimawo.middleware.distributedAgent.DistributedAgent;

public class NoOpJobFactory implements MasterWorkerFactory {

	@Override
	public void setParameters(String[] factArgs) throws DiMaWoException {
		// SKIP
	}

	@Override
	public WorkerAgent getWorkerAgent(DistributedAgent distributedAgent)
			throws DiMaWoException {
		return new NoOpWorkerAgent(distributedAgent);
	}

	@Override
	public MasterAgent getMasterAgent(DistributedAgent distributedAgent)
			throws DiMaWoException {
		return new NoOpMasterAgent(distributedAgent, 1000);
	}

}
