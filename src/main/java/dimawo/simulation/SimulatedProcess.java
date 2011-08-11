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
package dimawo.simulation;

import dimawo.agents.AgentException;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayException;
import dimawo.simulation.host.HostAccess;
import dimawo.simulation.middleware.VirtualTask;

public abstract class SimulatedProcess {
	private HostAccess access;
	private VirtualTask task;
	protected OverlayAgentInterface overInt;
	private DistributedAgent da;
	
	private boolean init;
	private Thread procThread;
	
	public SimulatedProcess(boolean init) {
		this.init = init;
	}
	
	public void setNetworkStack(HostAccess access) {
		this.access = access;
	}
	
	public void setTask(VirtualTask task) {
		this.task = task;
	}
	
	protected HostAccess getNetworkStack() {
		return access;
	}
	
	public void start() {
		procThread = new Thread() {
			public void run() {
				SimulatedProcess.this.run();
				try {
					SimulatedProcess.this.signalEnd();
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
		};
		procThread.start();
	}
	
	protected void signalEnd() throws Exception {
		if(task != null)
			task.signalEndOfProcess();
	}

	public void join() throws InterruptedException, OverlayException {
		if(procThread != null)
			procThread.join();
	}
	
	protected boolean isFirst() {
		return init;
	}
	
	public void kill() {
		try {
			overInt.leaveOverlay();
		} catch (OverlayException e) {
			e.printStackTrace();
		}
		da.killDA();
		// causes procThread to terminate its execution
	}
	
	protected void run() {
		try {
			da = initDA();
			overInt = initOverlay(da, access);
			configure();
			if(init)
				overInt.initOverlay();
			else
				overInt.joinOverlay();
		} catch (Exception e) {
			try {
				if(overInt != null)
					overInt.leaveOverlay();
			} catch (OverlayException e1) {
			}
			e.printStackTrace();
			return;
		}

		System.out.println("Running DA");
		try {
			da.start();
			da.join();
		} catch (AgentException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract DistributedAgent initDA() throws Exception;
	protected abstract OverlayAgentInterface initOverlay(DistributedAgent da, HostAccess access) throws Exception;
	protected abstract void configure() throws Exception;
}
