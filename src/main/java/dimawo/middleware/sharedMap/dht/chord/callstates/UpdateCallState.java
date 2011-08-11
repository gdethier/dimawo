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

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapUpdateResult;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordId;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.UpdateDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.UpdateDataEntryResultMessage;
import dimawo.middleware.sharedMap.events.UpdateValue;

public class UpdateCallState extends CallState {
	private enum State {WAIT_INIT, WAIT_DEST,
		WAIT_UPDATE, WAIT_ROUTING_TRIG, FINISHED};
	
	private State state;
	private UpdateValue updateCall;
	private ChordAgent chordAgent;
	
	private ChordId keyChordId;
	private DAId waitUpdateFrom;

	public UpdateCallState(UpdateValue call, ChordAgent agent) {
		super(agent, call);
		this.updateCall = call;
		this.chordAgent = agent;
		
		state = State.WAIT_INIT;
	}

	@Override
	public void init() {
		String key = updateCall.getKey();
		printMessage("init update "+key);
		keyChordId = new ChordId(key);
		triggerRouting0();
	}
	
	@Override
	public void triggerRouting() {
		if(! state.equals(State.WAIT_ROUTING_TRIG)) {
			printMessage("Ignored routing: current state is "+state.name());
			return;
		}

		triggerRouting0();
	}

	/**
	 * 
	 */
	private void triggerRouting0() {
		state = State.WAIT_DEST;
		if(chordAgent.succIsDestination(keyChordId)) {
			printMessage("Successor is destination");
			setDestination(chordAgent.getRoutingTable().getSuccEntry());
		} else {
			printMessage("Start routing");
			chordAgent.getRoutingAlgorithm().searchForDestination(keyChordId);
			// Wait for next hop (and maybe destination).
		}
	}

	@Override
	public boolean isFinished() {
		return state.equals(State.FINISHED);
	}

	@Override
	public void setDestination(ChordTableEntry en) {
		if(state.equals(State.WAIT_DEST)) {
			remoteUpdate(en);
		} else {
			printMessage("Ignored destination: current state is "+state.name());
		}
	}

	/**
	 * @param en
	 */
	private void remoteUpdate(ChordTableEntry en) {
		state = State.WAIT_UPDATE;
		waitUpdateFrom = en.getDaId();
		printMessage("Sending update request to "+en.getDaId());
		chordAgent.sendMessage(
				new UpdateDataEntryMessage(en.getDaId(),
				chordAgent.getAgentChordId(), en.getChordId(),
				keyChordId, updateCall.getKey(), updateCall.getUpdateData(),
				updateCall.getEntry()));
		scheduleTO("update", en.getDaId(), updateCall.getKey());
	}
	
	private void printMessage(String string) {
		chordAgent.printMessage("[UpdateCallState] "+string);
	}

	@Override
	public void handleMessage(CallStateMessage o) throws Exception {
		if(o instanceof UpdateDataEntryResultMessage) {
			handleUpdateDataEntryResultMessage((UpdateDataEntryResultMessage) o);
		} else {
			printMessage("Ignored unknown message: "+o.getClass().getName());
		}
	}
	
	private void handleUpdateDataEntryResultMessage(UpdateDataEntryResultMessage o) {
		if(state.equals(State.WAIT_UPDATE)) {
			cancelTO();
			
			String key = updateCall.getKey();
			if(key.equals(o.getKey())) {
				
				if(o.isRoutingError()) {
					printMessage("Routing failed, restart.");
					state = State.WAIT_ROUTING_TRIG;
					chordAgent.scheduleNewCallRouting();
				} else {
					finishCall();
				}
			} else {
				printMessage("Ignored result: wrong key "+o.getKey()+
						" (awaited "+key+")");
			}
		} else {
			printMessage("Ignored result: current state is "+state.name());
		}
	}
	
	private void finishCall() {
		printMessage("finished");
		state = State.FINISHED;
		updateCall.signalSuccess();
		
		SharedMapCallBackInterface cb = updateCall.getCallBack();
		if(cb != null)
			cb.sharedMapUpdateCallBack(new SharedMapUpdateResult(
					updateCall.getKey(), updateCall.getUpdateData(),
					updateCall.getEntry()));
	}
	
	@Override
	public void signalBrokenDA(DAId daId) {
		if(state.equals(State.WAIT_UPDATE) && waitUpdateFrom.equals(daId)) {
			cancelTO();
			triggerRouting0();
		}
	}

	@Override
	public void signalTO(RequestTO o) {
		if(state.equals(State.WAIT_UPDATE) &&
				"update".equals(o.getCallType()) &&
				waitUpdateFrom.equals(o.getDestDaId()) &&
				updateCall.getKey().equals(o.getDestDaId())) {
			cancelTO(); // clean-up
			triggerRouting0();
		}
	}
}
