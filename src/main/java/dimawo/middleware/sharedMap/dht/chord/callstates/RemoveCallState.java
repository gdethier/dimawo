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
import dimawo.middleware.overlay.SharedMapRemoveResult;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordId;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RemoveDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RemoveDataEntryResultMessage;
import dimawo.middleware.sharedMap.events.RemoveValue;

public class RemoveCallState extends CallState {
	private enum State {WAIT_INIT, WAIT_DEST, WAIT_REMOVAL, WAIT_REPLICATION, WAIT_ROUTING_TRIG, FINISHED};
	
	private State state;
	private RemoveValue remCall;
	private ChordAgent chordAgent;
	
	private ChordId keyChordId;
	private DAId waitRemoveFrom;

	public RemoveCallState(RemoveValue call, ChordAgent chordAgent) {
		super(chordAgent, call);
		this.remCall = call;
		this.chordAgent = chordAgent;
		
		state = State.WAIT_INIT;
	}

	@Override
	public void init() {
		printMessage("init");
		String key = remCall.getKey();
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
			setDestination(chordAgent.getRoutingTable().getSuccEntry());
		} else {
			chordAgent.getRoutingAlgorithm().searchForDestination(keyChordId);
			// Wait for next hop (and maybe destination).
		}
	}

	private void remoteRemove(ChordTableEntry en) {
		state = State.WAIT_REMOVAL;
		waitRemoveFrom = en.getDaId();
		chordAgent.sendMessage(
				new RemoveDataEntryMessage(en.getDaId(),
				chordAgent.getAgentChordId(), en.getChordId(),
				keyChordId, remCall.getKey()));
		scheduleTO("remove", en.getDaId(), remCall.getKey());
	}
	
	private void printMessage(String string) {
		chordAgent.printMessage("[RemoveCallState] "+string);
	}

	@Override
	public boolean isFinished() {
		return state.equals(State.FINISHED);
	}

	@Override
	public void setDestination(ChordTableEntry en) {
		if(state.equals(State.WAIT_DEST)) {
			remoteRemove(en);
		} else {
			printMessage("Ignored destination: current state is "+state.name());
		}
	}

	@Override
	public void handleMessage(CallStateMessage o) throws Exception {
		if(o instanceof RemoveDataEntryResultMessage) {
			handleRemoveDataEntryResultMessage((RemoveDataEntryResultMessage) o);
		} else {
			printMessage("Ignored unknown message: "+o.getClass().getName());
		}
	}

	private void handleRemoveDataEntryResultMessage(
			RemoveDataEntryResultMessage o) {
		if(state.equals(State.WAIT_REMOVAL)) {
			cancelTO();
			
			String key = remCall.getKey();
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
		remCall.signalSuccess();
		
		SharedMapCallBackInterface cb = remCall.getCallBack();
		if(cb != null)
			cb.sharedMapRemoveCallBack(new SharedMapRemoveResult(remCall.getKey()));
	}
	
	@Override
	public void signalBrokenDA(DAId daId) {
		if(state.equals(State.WAIT_REMOVAL) && waitRemoveFrom.equals(daId)) {
			cancelTO();
			triggerRouting0();
		}
	}

	@Override
	public void signalTO(RequestTO o) {
		if(state.equals(State.WAIT_REMOVAL) &&
				"remove".equals(o.getCallType()) &&
				waitRemoveFrom.equals(o.getDestDaId()) &&
				remCall.getKey().equals(o.getDestDaId())) {
			cancelTO(); // clean-up
			triggerRouting0();
		}
	}
}
