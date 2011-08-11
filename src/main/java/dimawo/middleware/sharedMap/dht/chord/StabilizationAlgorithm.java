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
package dimawo.middleware.sharedMap.dht.chord;

import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.sharedMap.dht.chord.messages.GetPredecessorMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetPredecessorResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.StabilizationAlgorithmMessage;

public class StabilizationAlgorithm {
	private ChordAgent agent;
	private DAId waitingFrom;
	
	public StabilizationAlgorithm(ChordAgent agent) {
		this.agent = agent;
	}
	
	public void trigger() {
		ChordTableEntry thisEn = agent.getThisChordTableEntry();
		ChordTableEntry succ = agent.getRoutingTable().getSuccEntry();
		waitingFrom = succ.getDaId();
		agent.sendMessage(new GetPredecessorMessage(waitingFrom,
				thisEn.getChordId(), succ.getChordId()));
	}

	private void printMessage(String string) {
		agent.printMessage("[StabilizationAlgorithm] "+string);
	}

	public void handleStabilizationAlgorithmMessage(StabilizationAlgorithmMessage o) throws UnknownAgentMessage {
		if(o instanceof GetPredecessorResultMessage) {
			handleGetPredecessorResultMessage((GetPredecessorResultMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleGetPredecessorResultMessage(GetPredecessorResultMessage o) {
		waitingFrom = null;
		
		ChordTableEntry succPrec = o.getPredecessor();
		ChordTableEntry thisEn = agent.getThisChordTableEntry();
		ChordRoutingTable table = agent.getRoutingTable();
		ChordTableEntry succEn = table.getSuccEntry();
		
		if(succPrec != null &&
			! succPrec.equals(succEn) &&
			succPrec.getChordId().isInInterval(thisEn.getChordId(), succEn.getChordId())) {
			agent.updateSuccessor(succPrec);
		}
		
		agent.sendMessage(new SetPredecessorMessage(succEn.getDaId(),
				thisEn.getChordId(), succEn.getChordId()));
	}
}
