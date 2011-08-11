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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import dimawo.agents.ErrorHandler;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.barriersync.AbstractBarrierAgent;
import dimawo.middleware.barriersync.BarrierSyncAgentMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.BarrierID;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.ComputerTreePosition;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierReachedMessage;



public class LeaderCentralBarrierAgent extends AbstractBarrierAgent implements MOSCallBack, CentralBarrierAgentInterface {
	
	private HashMap<BarrierID, SyncState> syncStates;
	private TreeSet<DAId> das;
	
	private CommunicatorInterface com;

	public LeaderCentralBarrierAgent(ErrorHandler parent, DistributedAgent da, CommunicatorInterface com) {
		super(parent, "LeaderCentralBarrier");
		
		setPrintStream(da.getFilePrefix());
		
		syncStates = new HashMap<BarrierID, SyncState>();
		das = new TreeSet<DAId>();
		
		this.com = com;
	}

	@Override
	protected void destroyBarrier(BarrierID id) {
		SyncState state = syncStates.remove(id);
		if(state != null) {
			state.signalDestruction();
		}
	}

	@Override
	protected void wait(BarrierID id, BarrierSyncCallBackInterface cbInt)
			throws Exception {
		SyncState state = syncStates.get(id);
		if(state == null) {
			state = new SyncState(com, id, das);
			syncStates.put(id, state);
		}
		
		agentPrintMessage("Barrier "+id+" reached by leader");

		state.leaderHasReached(cbInt);
		if(state.tryAllReached()) {
			syncStates.remove(id);
		}
	}

	protected void handleCentralBarrierMessage(CentralBarrierMessage o) throws Exception {
		if(o instanceof CentralBarrierReachedMessage) {
			handleCentralBarrierReachedMessage((CentralBarrierReachedMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
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
	public void signalBroken(BrokenDA bda) {
		try {
			submitMessage(bda);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success) {
			try {
				submitMessage(new BrokenDA(m.getRecipient()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handleCentralBarrierReachedMessage(CentralBarrierReachedMessage msg) {
		BarrierID id = msg.getBarrierId();
		agentPrintMessage("Barrier "+id+" reached by a DA");
		
		SyncState state = syncStates.get(id);
		if(state == null) {
			state = new SyncState(com, id, das);
			syncStates.put(id, state);
		}
		
		state.peerHasReached();
		if(state.tryAllReached()) {
			agentPrintMessage("All DAs reached barrier "+id);
			syncStates.remove(id);
		}
	}

	public void submitNewDA(DAId id) {
		try {
			submitMessage(id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handleSpecializedMessage(Object o) throws Exception {
		if(o instanceof DAId) {
			 handleDAId((DAId) o);
		} else if(o instanceof BrokenDA) {
			 handleBrokenDA((BrokenDA) o);
		} else if(o instanceof CentralBarrierMessage) {
			 handleCentralBarrierMessage((CentralBarrierMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleBrokenDA(BrokenDA o) {
		DAId id = o.getDAId();
		if(das.remove(id)) {
			for(Iterator<Entry<BarrierID, SyncState>> it = syncStates.entrySet().iterator();
			it.hasNext();) {
				Entry<BarrierID, SyncState> e = it.next();
				SyncState s = e.getValue();
				s.peerHasReached();
				
				if(s.tryAllReached())
					it.remove();
			}
		}
	}

	private void handleDAId(DAId o) {
		das.add(o);
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
	public void setComputerTreePosition(ComputerTreePosition pos) {
		// SKIP
	}

}
