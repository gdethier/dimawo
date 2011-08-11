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
package dimawo.middleware.overlay.impl.central.barrier;

import java.util.TreeMap;

import dimawo.agents.ErrorHandler;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.barriersync.AbstractBarrierAgent;
import dimawo.middleware.barriersync.BarrierSyncAgentMessage;
import dimawo.middleware.barriersync.BarrierWaitCallBack;
import dimawo.middleware.overlay.BarrierID;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.ComputerTreePosition;
import dimawo.middleware.overlay.LeaderElectionInterface;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierAllReachedMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierReachedMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.DestroyCentralBarrierMessage;



public class CentralBarrierAgent extends AbstractBarrierAgent implements CentralBarrierAgentInterface {
	
	private TreeMap<BarrierID, BarrierSyncCallBackInterface> waitingReached;

	private LeaderElectionInterface lead;
	
	public CentralBarrierAgent(ErrorHandler parent,
			OverlayAgentInterface overInt) {
		super(parent, "CentralBarrierAgent");
		
		setPrintStream(overInt.getDA().getFilePrefix());
		
		waitingReached = new TreeMap<BarrierID, BarrierSyncCallBackInterface>();
		
		this.lead = overInt.getLeaderElectionInterface();
	}

	@Override
	protected void destroyBarrier(BarrierID id) {
		throw new Error("peer is not leader");
	}

	@Override
	protected void wait(BarrierID id, BarrierSyncCallBackInterface cbInt)
			throws Exception {
		if(waitingReached.containsKey(id)) {
			cbInt.barrierWaitCB(new BarrierWaitCallBack(id,
				new Exception("CB already set")));
		} else {
			agentPrintMessage("Waiting for barrier "+id);
			waitingReached.put(id, cbInt);
			lead.sendMessageToLeader(
					new CentralBarrierReachedMessage(id));
		}
	}

	private void handleCentralBarrierMessage(CentralBarrierMessage o) throws Exception {
		if(o instanceof DestroyCentralBarrierMessage) {
			handleDestroyCentralBarrierMessage((DestroyCentralBarrierMessage) o);
		} else if(o instanceof CentralBarrierAllReachedMessage) {
			handleCentralBarrierAllReachedMessage((CentralBarrierAllReachedMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleCentralBarrierAllReachedMessage(
			CentralBarrierAllReachedMessage o) {
		BarrierID id = o.getBarrierId();
		agentPrintMessage("Barrier "+id+" reached");
		BarrierSyncCallBackInterface cb = waitingReached.remove(id);
		if(cb != null) {
			cb.barrierWaitCB(new BarrierWaitCallBack(id));
		}
	}

	private void handleDestroyCentralBarrierMessage(
			DestroyCentralBarrierMessage o) {
		BarrierID id = o.getBarrierId();
		BarrierSyncCallBackInterface cb = waitingReached.remove(id);
		if(cb != null) {
			cb.barrierWaitCB(new BarrierWaitCallBack(id,
					new Exception("Barrier destroyed")));
		}
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
	}

	@Override
	public void submitCentralBarrierMessage(CentralBarrierMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handleSpecializedMessage(Object o) throws Exception {
		if(o instanceof CentralBarrierMessage) {
			handleCentralBarrierMessage((CentralBarrierMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	@Override
	public void setComputerTreePosition(ComputerTreePosition pos) {
		// SKIP
	}

}
