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
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.SharedMapPutResult;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordId;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.PutDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.PutDataEntryResultMessage;
import dimawo.middleware.sharedMap.events.PutValue;

public class PutCallState extends CallState {
	private enum State {WAIT_INIT, WAIT_DEST,
		WAIT_INSERTION, WAIT_ROUTING_TRIG, FINISHED};
	
	private State state;
	private PutValue putCall;
	private ChordAgent chordAgent;
	
	private ChordId keyChordId;
	private DAId waitInsertFrom;

	public PutCallState(PutValue call, ChordAgent chordAgent) {
		super(chordAgent, call);
		this.putCall = call;
		this.chordAgent = chordAgent;
		
		state = State.WAIT_INIT;
	}

	@Override
	public void init() {
		printMessage("init");
		String key = putCall.getEntry().getKey();
		keyChordId = new ChordId(key);
		triggerRouting0();
	}

	/**
	 * 
	 */
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
			setDestination(chordAgent.getRoutingTable().getSuccEntry());
		} else {
			printMessage("Waiting next hop");
			state = State.WAIT_DEST;
			
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
			printMessage("Got destination ("+en+"), putting entry.");
			state = State.WAIT_INSERTION;
			waitInsertFrom = en.getDaId();
			chordAgent.sendMessage(
					new PutDataEntryMessage(en.getDaId(),
					chordAgent.getAgentChordId(), en.getChordId(),
					keyChordId, putCall.getEntry()));
			
			scheduleTO("put", en.getDaId(), putCall.getEntry().getKey());
		} else {
			printMessage("Ignored destination: current state is "+state.name());
		}
	}
	
	private void printMessage(String string) {
		chordAgent.printMessage("[PutCallState] "+string);
	}

	@Override
	public void handleMessage(CallStateMessage o) {
		if(o instanceof PutDataEntryResultMessage) {
			handlePutDataEntryResultMessage((PutDataEntryResultMessage) o);
		} else {
			printMessage("Ignored unknown message: "+o.getClass().getName());
		}
	}

	private void handlePutDataEntryResultMessage(PutDataEntryResultMessage o) {
		if(state.equals(State.WAIT_INSERTION)) {
			cancelTO();
			
			SharedMapEntry en = putCall.getEntry();
			if(en.equals(o.getEntry())) {
				
				if(o.isRoutingError()) {
					printMessage("Routing failed, restart.");
					state = State.WAIT_ROUTING_TRIG;
					chordAgent.scheduleNewCallRouting();
				} else {
					finishCall();
				}
			} else {
				printMessage("Ignored result: wrong key "+en.getKey()+
						" (awaited "+putCall.getEntry().getKey()+")");
			}
		} else {
			printMessage("Ignored result: current state is "+state.name());
		}
	}

	private void finishCall() {
		printMessage("finished");
		state = State.FINISHED;
		putCall.signalSuccess();
		
		SharedMapCallBackInterface cb = putCall.getCallBack();
		if(cb != null)
			cb.sharedMapPutCallBack(new SharedMapPutResult(putCall.getEntry()));
	}
	
	@Override
	public void signalBrokenDA(DAId daId) {
		if(state.equals(State.WAIT_INSERTION) && waitInsertFrom.equals(daId)) {
			cancelTO();
			triggerRouting0();
		}
	}

	@Override
	public void signalTO(RequestTO o) {
		if(state.equals(State.WAIT_INSERTION) &&
				"put".equals(o.getCallType()) &&
				waitInsertFrom.equals(o.getDestDaId()) &&
				putCall.getEntry().getKey().equals(o.getDestDaId())) {
			cancelTO(); // clean-up
			triggerRouting0();
		}
	}
}
