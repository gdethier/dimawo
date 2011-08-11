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
package dimawo.middleware.overlay.faultdetection;

import java.util.HashMap;

import dimawo.middleware.distributedAgent.DAId;



public class TargetInfo {
	private DAId target;

	private long lastPing;
	private boolean acked;
	private HashMap<FaultDetectionServiceCallBackInterface, Integer> registered;
	
	public TargetInfo(DAId dest) {
		this.target = dest;

		lastPing = -1;
		acked = false;
		registered = new HashMap<FaultDetectionServiceCallBackInterface, Integer>();
	}
	
	public boolean isLastPingAcked() {
		return acked;
	}
	
	public void setLastPingAcked(boolean b) {
		acked = b;
	}
	
	public boolean hasTimedOut(long timeout) {
		if(lastPing == -1 || acked)
			return false;
		
		long now = System.currentTimeMillis();
		return (now - lastPing) > timeout;
	}
	
	public void signalTimeout() {
		for(FaultDetectionServiceCallBackInterface cb : registered.keySet()) {
			cb.signalTargetTimeout(new DetectorEvent(target, false));
		}
	}
	
	public void signalFailure() {
		for(FaultDetectionServiceCallBackInterface cb : registered.keySet()) {
			cb.signalTargetTimeout(new DetectorEvent(target, true));
		}
	}

	public void addCB(FaultDetectionServiceCallBackInterface cb) {
		Integer refCount = registered.get(cb);
		if(refCount == null) {
			refCount = new Integer(0);
		}

		registered.put(cb, new Integer(refCount + 1));
	}

	public void unregister(FaultDetectionServiceCallBackInterface cb) {
		Integer refCount = registered.get(cb);
		if(refCount == null)
			return;
		refCount = refCount - 1;
		if(refCount == 0) {
			registered.remove(cb);
		} else {
			registered.put(cb, refCount);
		}
	}

	public boolean noCbRegistered() {
		return registered.isEmpty();
	}
	
	public void ping() {
		acked = false;
		lastPing = System.currentTimeMillis();
	}

	public void ackLastPing() {
		acked = true;
	}
}
