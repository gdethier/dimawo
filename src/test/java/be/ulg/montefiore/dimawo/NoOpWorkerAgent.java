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

import java.io.Serializable;

import dimawo.WorkerAgent;
import dimawo.WorkerMessage;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.mntree.MnPeerState;

public class NoOpWorkerAgent extends WorkerAgent {

	public NoOpWorkerAgent(DistributedAgent da) {
		super(da, "NoOpWorkerAgent");
	}

	@Override
	protected void onLocalTopologyChange(MnPeerState newState) throws Exception {
		// SKIP
	}

	@Override
	protected void handleWorkerEvent(Object o) throws Exception {
		// SKIP
	}

	@Override
	protected void handleWorkerMessage(WorkerMessage o) throws Exception {
		// SKIP
	}

	@Override
	protected Serializable preWorkerExit() {
		return null;
	}

	@Override
	protected void init() throws Throwable {
		// SKIP
	}

}
