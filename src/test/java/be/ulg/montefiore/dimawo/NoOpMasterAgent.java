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

import java.util.Timer;
import java.util.TimerTask;

import dimawo.MasterAgent;
import dimawo.master.messages.MasterMessage;
import dimawo.master.messages.WorkerExitMessage;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;

public class NoOpMasterAgent extends MasterAgent {
	private long ttl;

	public NoOpMasterAgent(DistributedAgent hostingDa, long ttl) {
		super(hostingDa, "NoOpMasterAgent");
		
		this.ttl = ttl;
	}

	@Override
	protected void handleUserDefinedAgentMessage(MasterMessage msg)
			throws Exception {
		// SKIP
	}

	@Override
	protected void handleWorkerExit(WorkerExitMessage msg) throws Exception {
		// SKIP
	}

	@Override
	protected void onExit() {
		// SKIP
	}

	@Override
	protected void onStartup() throws Throwable {
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				NoOpMasterAgent.this.signalTimeOut();
			}
		}, ttl);
	}

	protected void signalTimeOut() {
		try {
			submitMessage(new Object());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onTopologyChange(DAId subject, ChangeType type)
			throws Exception {
		// SKIP
	}

	@Override
	protected void handleMasterEvent(Object o) throws Exception {
		agentPrintMessage("Shutting down NoOp job.");
		shutdown();
	}

}
