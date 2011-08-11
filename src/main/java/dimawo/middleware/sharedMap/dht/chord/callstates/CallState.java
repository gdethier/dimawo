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
package dimawo.middleware.sharedMap.dht.chord.callstates;

import java.util.Timer;
import java.util.TimerTask;

import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;



public abstract class CallState {
	private ChordAgent chordAgent;
	private AsynchronousCall call;
	private Timer toTimer;

	public CallState(ChordAgent chordAgent, AsynchronousCall call) {
		this.chordAgent = chordAgent;
		this.call = call;
	}
	
	public AsynchronousCall getCall() {
		return call;
	}
	
	protected void scheduleTO(final String callType, final DAId dest, final String key) {
		if(toTimer != null)
			throw new Error("TO timer already set");
		
		toTimer = new Timer(true);
		toTimer.schedule(new TimerTask() {
			public void run() {
				chordAgent.submitCallRequestTO(callType, dest, key);
			}
		}, 3000);
	}
	
	protected void cancelTO() {
		if(toTimer != null) {
			toTimer.cancel();
			toTimer = null;
		}
	}

	public abstract void init();
	public abstract boolean isFinished();
	public abstract void setDestination(ChordTableEntry en);
	public abstract void handleMessage(CallStateMessage o) throws Exception;
	public abstract void triggerRouting();
	public abstract void signalBrokenDA(DAId daId);
	public abstract void signalTO(RequestTO o);
}
