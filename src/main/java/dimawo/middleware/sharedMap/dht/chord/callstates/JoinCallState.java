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
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ChordJoinMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ChordJoinResultMessage;

public class JoinCallState extends CallState {
	private enum State {WAIT_INIT, FINISHED, WAIT_DEST};
	
	private State state;
	private DAId initContact, currentContact;
	private ChordAgent agent;
	
	public JoinCallState(DAId contact, ChordAgent agent) {
		super(agent, null);
		
		this.initContact = contact;
		this.currentContact = contact;
		this.agent = agent;
		
		state = State.WAIT_INIT;
	}

	@Override
	public void init() {
		printMessage("init");
		state = State.WAIT_DEST;
		agent.sendMessage(new ChordJoinMessage(currentContact, agent.getThisChordTableEntry()));
		
		scheduleTO("join", currentContact, null);
	}

	@Override
	public boolean isFinished() {
		return state.equals(State.FINISHED);
	}

	@Override
	public void setDestination(ChordTableEntry en) {
		printMessage("Ignored destination, state="+state.name());
	}

	@Override
	public void handleMessage(CallStateMessage o) throws Exception {
		if(o instanceof ChordJoinResultMessage) {
			handleChordJoinResultMessage((ChordJoinResultMessage) o);
		} else {
			printMessage("Ignored message "+o.getClass().getName());
		}
	}

	private void handleChordJoinResultMessage(ChordJoinResultMessage o) throws Exception {
		if(state.equals(State.WAIT_DEST)) {
			cancelTO();
			
			ChordTableEntry succOrNext = o.getNextHopOrSucc();
			if(o.isSuccessor()) {
				printMessage("finished");
				state = State.FINISHED;

				agent.initRoutingTable(succOrNext);
				agent.getRoutingTable().print(agent);

				agent.signalJoined();
				agent.scheduleStabilization();
			} else {
				currentContact = succOrNext.getDaId();
				agent.sendMessage(new ChordJoinMessage(succOrNext.getDaId(),
						agent.getThisChordTableEntry()));
				
				scheduleTO("join", currentContact, null);
			}
		} else {
			printMessage("Ignored join result, state="+state.name());
		}
	}

	private void printMessage(String string) {
		agent.printMessage("[JoinCallState] "+string);
	}

	@Override
	public void triggerRouting() {
		printMessage("Ignored routing trigger");
	}
	
	@Override
	public void signalBrokenDA(DAId daId) {
		if(currentContact.equals(daId)) {
			cancelTO();
			throw new Error("Contact failure");
		}
	}

	@Override
	public void signalTO(RequestTO o) {
		if(state.equals(State.WAIT_DEST) &&
				"join".equals(o.getCallType()) &&
				currentContact.equals(o.getDestDaId())) {
			cancelTO(); // clean-up
			
			currentContact = initContact;
			agent.sendMessage(new ChordJoinMessage(currentContact, agent.getThisChordTableEntry()));
			scheduleTO("join", currentContact, null);
		}
	}
}
