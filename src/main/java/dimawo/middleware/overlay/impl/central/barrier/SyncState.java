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

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import dimawo.middleware.barriersync.BarrierWaitCallBack;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.BarrierID;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierAllReachedMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierReachedMessage;
import dimawo.middleware.overlay.impl.central.barrier.messages.DestroyCentralBarrierMessage;



public class SyncState {
	
	private BarrierID id;
	private CommunicatorInterface com;
	
	private boolean leaderHasReached;
	private int awaitedReached;
	
	private BarrierSyncCallBackInterface leaderCbInt;
	private TreeSet<DAId> das;
	
	public SyncState(CommunicatorInterface com, BarrierID id, Collection<DAId> das) {
		this.id = id;
		this.com = com;
		leaderHasReached = false;
		awaitedReached = das.size();
		
		this.das = new TreeSet<DAId>();
		this.das.addAll(das);
	}

	public void leaderHasReached(BarrierSyncCallBackInterface cbInt) {
		leaderHasReached = true;
		leaderCbInt = cbInt;
	}
	
	public void peerHasReached() {
		--awaitedReached;
	}

	public boolean tryAllReached() {
		if(leaderHasReached && awaitedReached == 0) {
			leaderCbInt.barrierWaitCB(new BarrierWaitCallBack(id));
			for(DAId daId : das) {
				com.sendDatagramMessage(new CentralBarrierAllReachedMessage(daId, id));
			}
			return true;
		}
		return false;
	}
	
	public void signalDestruction() {
		// Signal destruction to all DAs
		for(DAId daId : das) {
			DestroyCentralBarrierMessage cbm = new DestroyCentralBarrierMessage(daId, id);
			com.sendDatagramMessage(cbm);
		}
	}

}
