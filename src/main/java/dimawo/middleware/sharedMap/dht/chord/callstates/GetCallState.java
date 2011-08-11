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
import dimawo.middleware.overlay.SharedMapGetResult;
import dimawo.middleware.overlay.SharedMapValue;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordId;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetDataEntryResultMessage;
import dimawo.middleware.sharedMap.events.GetValue;

public class GetCallState extends CallState {
	private enum State {WAIT_INIT, WAIT_DEST, WAIT_DATA, WAIT_ROUTING_TRIG,
		FINISHED};

	private State state;
	private GetValue getCall;
	private ChordAgent chordAgent;
	
	private ChordId getChordId;
	private DAId waitDataFrom;

	public GetCallState(GetValue call, ChordAgent chordAgent) {
		super(chordAgent, call);
		
		getCall = call;
		this.chordAgent = chordAgent;
		
		getChordId = new ChordId(getCall.getKey());
		state = State.WAIT_INIT;
	}

	@Override
	public void init() {
		String key = getCall.getKey();
		printMessage("init get "+key);
		ChordId id = new ChordId(key);
//		boolean search = ! (chordAgent.getRoutingTable().hasPredecessor() &&
//			id.isInInterval(chordAgent.getRoutingTable().getPrecEntry().getChordId(),
//					chordAgent.getAgentChordId()));
		boolean search = true;
		if(! search) {
			printMessage("Entry found locally");
			finishCall(key, chordAgent.getLocalEntry(getChordId, key));
		} else {
			triggerRouting0();
		}
	}

	/**
	 * @param key
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
		printMessage("Routing triggered");
		state = State.WAIT_DEST;
		if(chordAgent.succIsDestination(getChordId)) {
			setDestination(chordAgent.getRoutingTable().getSuccEntry());
		} else {
			chordAgent.getRoutingAlgorithm().searchForDestination(getChordId);
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
			state = State.WAIT_DATA;
			waitDataFrom = en.getDaId();
			chordAgent.sendMessage(new GetDataEntryMessage(waitDataFrom,
					chordAgent.getAgentChordId(), en.getChordId(),
					getChordId, getCall.getKey()));
			
			scheduleTO("get", waitDataFrom, getCall.getKey());
		} else {
			printMessage("Ignored destination: current state is "+state.name());
		}
	}

	private void printMessage(String string) {
		chordAgent.printMessage("[GetCallState] "+string);
	}

	@Override
	public void handleMessage(CallStateMessage o) {
		if(o instanceof GetDataEntryResultMessage) {
			handleGetDataEntryResultMessage((GetDataEntryResultMessage) o);
		} else {
			printMessage("Ignored unknown message: "+o.getClass().getName());
		}
	}

	private void handleGetDataEntryResultMessage(GetDataEntryResultMessage o) {
		if(state.equals(State.WAIT_DATA)) {
			cancelTO();
			
			String key = getCall.getKey();
			if(key.equals(o.getKey())) {
				
				if(o.isRoutingError()) {
					printMessage("Routing failed, restart.");
					state = State.WAIT_ROUTING_TRIG;
					chordAgent.scheduleNewCallRouting();
					return;
				}
				
				SharedMapValue val = o.getValue();
				SharedMapEntry res = null;
				if(val != null) {
					res = new SharedMapEntry(o.getKey(), o.getValue());
				}
				
				finishCall(key, res);
			} else {
				printMessage("Ignored result: wrong key "+o.getKey()+
						" (awaited "+getCall.getKey()+")");
			}
		} else {
			printMessage("Ignored result: current state is "+state.name());
		}
	}

	/**
	 * @param key
	 * @param res
	 */
	private void finishCall(String key, SharedMapEntry res) {
		printMessage("finished");
		state = State.FINISHED;
		getCall.setResult(res);
		getCall.signalSuccess();
		
		SharedMapCallBackInterface cb = getCall.getCallBack();
		if(cb != null)
			cb.sharedMapGetCallBack(new SharedMapGetResult(key, res));
	}

	@Override
	public void signalBrokenDA(DAId daId) {
		if(state.equals(State.WAIT_DATA) && waitDataFrom.equals(daId)) {
			cancelTO();
			triggerRouting0();
		}
	}

	@Override
	public void signalTO(RequestTO o) {
		if(state.equals(State.WAIT_DATA) &&
				"get".equals(o.getCallType()) &&
				waitDataFrom.equals(o.getDestDaId()) &&
				getCall.getKey().equals(o.getDestDaId())) {
			cancelTO(); // clean-up
			triggerRouting0();
		}
	}

}
