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

import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.ComputerTreePosition;



public class SyncState {
	private TreeSet<DAId> awaitedPeers;
	private ComputerTreePosition ctPos;
	private BarrierSyncCallBackInterface cb;

	public SyncState(ComputerTreePosition ctPos,
			BarrierSyncCallBackInterface cb) {
		this.ctPos = ctPos;
		this.awaitedPeers = new TreeSet<DAId>();
		awaitedPeers.add(ctPos.getThisId());
		for(int i = 0; i < ctPos.getNumOfChildren(); ++i) {
			awaitedPeers.add(ctPos.getChildId(i));
		}
		this.cb = cb;
	}

	public BarrierSyncCallBackInterface getBarrierCB() {
		return cb;
	}

	public void reached(DAId daId) {
		awaitedPeers.remove(daId);
	}

	public boolean allReached() {
		return awaitedPeers.isEmpty();
	}

	public void setCallBack(BarrierSyncCallBackInterface cb) {
		this.cb = cb;
	}

	public ComputerTreePosition getCtPosition() {
		return ctPos;
	}
}
