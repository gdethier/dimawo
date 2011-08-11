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

import java.util.Timer;
import java.util.TimerTask;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.sharedMap.dht.chord.events.RoutingTO;
import dimawo.middleware.sharedMap.dht.chord.messages.GetNextHopMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RoutingResultMessage;



public class ChordRoutingAlgorithm {
	private ChordAgent agent;
	
	private int toId;
	private Timer toTimer;
	
	private int nHops;
	private DAId nextHopFrom;
	private ChordId destForKey;
	private boolean routingInterrupted;
	
	public ChordRoutingAlgorithm(ChordAgent agent) {
		this.agent = agent;
	}
	
	public void searchForDestination(ChordId key) {
		if(nextHopFrom != null)
			throw new Error("A routing is already in progress");

		ChordTableEntry tabEn = agent.getRoutingTable().getNextHop(key);
		nextHopFrom = tabEn.getDaId();
		destForKey = key;
		nHops = 1;
		routingInterrupted = false;
		GetNextHopMessage msg = new GetNextHopMessage(nextHopFrom,
				agent.getAgentChordId(), tabEn.getChordId(), destForKey);
		msg.setCallBack(agent); // to be signaled about failure
		agent.sendMessage(msg);
		
		scheduleTO();
	}
	
	private void scheduleTO() {
		if(toTimer != null)
			throw new Error("TO timer already set");
		
		++toId;
		
		toTimer = new Timer(true);
		toTimer.schedule(new TimerTask() {
			public void run() {
				agent.submitRoutingTO(toId);
			}
		}, 3000);
	}
	
	private void cancelTO() {
		if(toTimer != null) {
			toTimer.cancel();
			toTimer = null;
		}
	}

	public void handleRoutingResultMessage(RoutingResultMessage o) {
		if(! o.getKey().equals(destForKey)) {
			printMessage("Ignored obsolete routing result");
			return;
		}
		
		cancelTO();
		
		ChordTableEntry en = o.getNextOrDest();
		if(o.isDestination()) {
			printMessage("Successor of "+destForKey+" is "+en+" (#hops="+nHops+")");
			nextHopFrom = null;
			agent.signalDestination(en);
		} else {
			++nHops;
			nextHopFrom = en.getDaId();
			printMessage("Next hop for "+destForKey+" is "+nextHopFrom);
			GetNextHopMessage msg = new GetNextHopMessage(nextHopFrom,
					agent.getAgentChordId(), en.getChordId(), destForKey);
			msg.setCallBack(agent); // to be signaled about failure
			agent.sendMessage(msg);
			
			scheduleTO();
		}
	}
	
	private void printMessage(String txt) {
		agent.agentPrintMessage("[ChordRoutingAlgorithm] "+txt);
	}

	public void signalBroken(DAId id) {
		if(id.equals(nextHopFrom)) {
			nextHopFrom = null;
			routingInterrupted = true;
			cancelTO();
		}
	}

	public void restartRouting() {
		if(routingInterrupted) {
			searchForDestination(destForKey);
		}
	}

	public void signalTO(RoutingTO o) {
		int id = o.getId();
		if(toId == id) {
			cancelTO(); // clean-up
			nextHopFrom = null;
			
			searchForDestination(destForKey);
		}
	}
}
