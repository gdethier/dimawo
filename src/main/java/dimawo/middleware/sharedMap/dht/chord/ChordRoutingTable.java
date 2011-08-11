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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import dimawo.agents.AbstractAgent;
import dimawo.middleware.distributedAgent.DAId;



public class ChordRoutingTable {
	private int maxSucc;

	private ChordTableEntry thisEn;
	private ChordTableEntry prec;
	
	private LinkedList<ChordTableEntry> succ;
	
//	private ChordTableEntry[] successors;
//	private ChordTableEntry[] fingers;
	
	public ChordRoutingTable(ChordTableEntry thisEn, int maxSucc) {
		this.maxSucc = maxSucc;
		
		this.thisEn = thisEn;
		prec = null;
		
		succ = new LinkedList<ChordTableEntry>();

//		successors = new ChordTableEntry[nSuccessors];
//		fingers = new ChordTableEntry[ChordId.NBITS];
	}

	public ChordTableEntry getPrecEntry() {
		return prec;
	}
	
	public boolean isDestination(ChordId id) throws BrokenRoutingTableException {
		if(prec == null)
			throw new BrokenRoutingTableException("No predecessor available");
		return id.isInInterval(prec.getChordId(), thisEn.getChordId());
	}

	public ChordTableEntry getNextHop(ChordId key) {
		// Simple routing
		return succ.getFirst();

		// Scalable routing
//		ChordId thisId = thisEn.getChordId();
//		for(int i = ChordId.NBITS - 1; i >= 0; --i) {
//			if(fingers[i] != null) {
//				ChordId f = fingers[i].getChordId();
//				if(f.isInInterval(thisId, key)) {
//					return fingers[i];
//				}
//			} else {
//				throw new BrokenRoutingTableException(i+"th finger missing");
//			}
//		}
//		
//		return null;
	}

	public boolean remove(DAId id) {
		if(prec != null && prec.getDaId().equals(id)) {
			prec = null;
			return true;
		}

		for(Iterator<ChordTableEntry> it = succ.iterator(); it.hasNext();) {
			ChordTableEntry e = it.next();
			if(e.getDaId().equals(id)) {
				it.remove();
				return true;
			}
		}

		return false;
	}

	public void thisIsInit() {
		prec = null;
		succ.add(thisEn);
	}

	public void setSuccessor(ChordTableEntry en) {
		succ.addFirst(en);
		while(succ.size() > maxSucc) {
			succ.removeLast();
		}
	}

	public ChordTableEntry getSuccEntry() {
		return succ.getFirst();
	}

	public void setPredecessor(ChordTableEntry e) {
		prec = e;
	}
	
	public void print(AbstractAgent agent) {
		agent.agentPrintMessage("Routing table of "+thisEn);
		agent.agentPrintMessage("Predecessor:");
		if(prec != null)
			agent.agentPrintMessage(prec.toString());
		else
			agent.agentPrintMessage("none");
		agent.agentPrintMessage("Successor:");
		if(succ != null)
			agent.agentPrintMessage(succ.toString());
		else
			agent.agentPrintMessage("none");
	}

	public boolean hasSuccessor() {
		return ! succ.isEmpty();
	}

	public boolean succIsDestination(ChordId key) {
		return key.isInInterval(thisEn.getChordId(), succ.getFirst().getChordId());
	}

	public boolean hasPredecessor() {
		return prec != null;
	}

	public boolean isBroken() {
		return succ.isEmpty();
	}

	public LinkedList<ChordTableEntry> getSuccessors() {
		return succ;
	}

	public Collection<ChordTableEntry> updateSuccessors(
			LinkedList<ChordTableEntry> newSucc) {
		TreeSet<ChordTableEntry> set1 = new TreeSet<ChordTableEntry>();
		set1.addAll(newSucc);
		set1.removeAll(succ);
		
		succ = newSucc;
		TreeSet<ChordTableEntry> succSet = new TreeSet<ChordTableEntry>();
		for(Iterator<ChordTableEntry> it = succ.iterator(); it.hasNext();) {
			ChordTableEntry e = it.next();
			if(! succSet.contains(e)) {
				succSet.add(e);
			} else {
				it.remove();
			}
		}
		return set1;
	}
}
