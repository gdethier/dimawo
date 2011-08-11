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
package dimawo.middleware.overlay.impl.decentral.barriersync;

import java.util.TreeMap;
import java.util.TreeSet;

import dimawo.agents.ErrorHandler;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.barriersync.AbstractBarrierAgent;
import dimawo.middleware.barriersync.BarrierWaitCallBack;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.BarrierID;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.ComputerTreePosition;
import dimawo.middleware.overlay.impl.decentral.barriersync.events.SetComputerTreePosition;
import dimawo.middleware.overlay.impl.decentral.barriersync.messages.BarrierReachedByAllMessage;
import dimawo.middleware.overlay.impl.decentral.barriersync.messages.BarrierReachedBySubtreeMessage;



public class BarrierSyncAgent extends AbstractBarrierAgent implements MessageHandler {
	public static final String handlerId = "BarrierSyncAgentMsg";
	
	private CommunicatorInterface com;
	private TreeMap<BarrierID, SyncState> syncStates;
	private TreeMap<BarrierID, TreeSet<DAId>> reached;
	
	private ComputerTreePosition currentCtPos;

	public BarrierSyncAgent(ErrorHandler parent, CommunicatorInterface com) {
		super(parent, "BarrierSyncAgent");
		
		this.com = com;
		
		com.registerMessageHandler(handlerId, this);
		
		syncStates = new TreeMap<BarrierID, SyncState>();
		reached = new TreeMap<BarrierID, TreeSet<DAId>>();
	}

	@Override
	protected void destroyBarrier(BarrierID id) {
		syncStates.remove(id);
	}

	@Override
	protected void wait(BarrierID id, BarrierSyncCallBackInterface cbInt)
			throws Exception {
		agentPrintMessage("Barrier reached locally: "+id);
		SyncState state = syncStates.get(id);
		if(state != null) {
			cbInt.barrierWaitCB(new BarrierWaitCallBack(id,
					new Exception("Already waiting for barrier "+id)));
			return;
		}
		
		state = new SyncState(currentCtPos, cbInt);
		syncStates.put(id, state);
		
		TreeSet<DAId> set = reached.remove(id);
		if(set != null) {
			for(DAId daId : set) {
				state.reached(daId);
			}
		}

		DAId thisDaId = currentCtPos.getThisId();
		state.reached(thisDaId);
		if(state.allReached()) {
			signalSubtreeReached(id, state);
		}
	}
	
	private void signalSubtreeReached(BarrierID id, SyncState state) {
		agentPrintMessage("Barrier reached by subtree: "+id);
		ComputerTreePosition ctPos = state.getCtPosition();
		DAId parentId = ctPos.getParentId();
		if(parentId != null) {
			agentPrintMessage("Propagating to parent ("+parentId+")");
			com.sendDatagramMessage(new BarrierReachedBySubtreeMessage(
					parentId, id));
		} else {
			agentPrintMessage("Barrier "+id+" reached by all");
			SyncState syncState = syncStates.remove(id);
			syncState.getBarrierCB().barrierWaitCB(new BarrierWaitCallBack(id));

			for(int i = 0; i < ctPos.getNumOfChildren(); ++i) {
				DAId daId = ctPos.getChildId(i);
				com.sendDatagramMessage(new BarrierReachedByAllMessage(daId, id));
			}
		}
	}
	
	@Override
	protected void handleSpecializedMessage(Object o) throws Exception {
		if(o instanceof BarrierReachedByAllMessage) {
			handleBarrierReachedByAllMessage((BarrierReachedByAllMessage) o);
		} else if(o instanceof BarrierReachedBySubtreeMessage) {
			handleBarrierReachedBySubtreeMessage((BarrierReachedBySubtreeMessage) o);
		} else if(o instanceof SetComputerTreePosition) {
			handleSetComputerTreePosition((SetComputerTreePosition) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}
	
	private void handleSetComputerTreePosition(SetComputerTreePosition o) {
		currentCtPos = o.getPosition();
		agentPrintMessage("Updated CT position:");
		agentPrintMessage(currentCtPos.toString());
	}

	private void handleBarrierReachedByAllMessage(BarrierReachedByAllMessage o) {
		BarrierID id = o.getBarrierId();
		SyncState syncState = syncStates.remove(id);
		if(syncState == null) {
			agentPrintMessage("No wait to release for barrier "+id);
			return;
		}
		
		agentPrintMessage("Barrier "+id+" reached by all.");
		
		if(! syncState.allReached())
			throw new Error("Incoherence");
		
		ComputerTreePosition ctPos = syncState.getCtPosition();
		for(int i = 0; i < ctPos.getNumOfChildren(); ++i) {
			com.sendDatagramMessage(new BarrierReachedByAllMessage(
					ctPos.getChildId(i), id));
		}
		
		syncState.getBarrierCB().barrierWaitCB(new BarrierWaitCallBack(id));
	}
	
	private void handleBarrierReachedBySubtreeMessage(
			BarrierReachedBySubtreeMessage o) {
		BarrierID id = o.getBarrierId();
		agentPrintMessage("Barrier reached by child "+o.getSender()+": "+id);
		SyncState syncState = syncStates.get(id);
		if(syncState == null) {
			TreeSet<DAId> set = reached.get(id);
			if(set == null) {
				set = new TreeSet<DAId>();
				reached.put(id, set);
			}
			set.add(o.getSender());
		} else {
			syncState.reached(o.getSender());
			if(syncState.allReached()) {
				signalSubtreeReached(id, syncState);
			}
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
	public void submitIncomingMessage(Message msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void setComputerTreePosition(ComputerTreePosition pos) {
		try {
			submitMessage(new SetComputerTreePosition(pos));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
