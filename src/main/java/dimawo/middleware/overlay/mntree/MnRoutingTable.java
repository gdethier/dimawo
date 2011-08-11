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
package dimawo.middleware.overlay.mntree;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class MnRoutingTable implements Serializable {
	private static final long serialVersionUID = 1L;

	private DAId mainPeer; // Note: mainPeer is also in peers set
	private TreeSet<DAId> peers;
	/** chronoly contains the DAIds of the peers in this routing table
	 * in chronological order of insertion */
	private LinkedList<DAId> chronology;
	
	public MnRoutingTable() {
		peers = new TreeSet<DAId>();
		chronology = new LinkedList<DAId>();
	}
	
	public void addMainPeer(DAId id) {
		this.mainPeer = id;
		if(! peers.isEmpty())
			throw new Error("Main peer must be set first");
		addPeer(id);
	}

	public void addPeer(DAId id) {
		if(peers.add(id))
			chronology.addFirst(id);
	}
	
	public boolean removePeer(DAId id) {
		if(peers.remove(id)) {
			chronology.removeFirstOccurrence(id);

			if(mainPeer.equals(id)) {
				if(! peers.isEmpty()) {
					mainPeer = peers.last(); // oldest peer
				} else
					mainPeer = null;
			}
			
			return true;
		}

		return false;
	}
	
	public boolean[] removePeers(DAId[] toRem) {
		boolean[] ret = new boolean[toRem.length];
		for(int i = 0; i < toRem.length; ++i) {
			ret[i] = removePeer(toRem[i]);
		}
		return ret;
	}

	public int size() {
		return peers.size();
	}

	public DAId getMainPeer() {
		return mainPeer;
	}

	public void fillWith(MnRoutingTable table) {
		mainPeer = table.mainPeer;
		peers.addAll(table.peers);
		chronology.addAll(table.chronology);
	}

	public boolean containsPeer(DAId daId) {
		return peers.contains(daId);
	}

	public void getPeers(Set<DAId> dest) {
		dest.addAll(peers);
	}
	
	public void getPeers(DAId[] dest) {
		if(dest.length != peers.size())
			throw new Error("Destination array size does not match");
		int i = 0;
		for(DAId id : peers) {
			dest[i] = id;
			++i;
		}
	}

	public void extractYoungestPeers(DAId[] newMnPeers, int from, int length) {
		if(length >= chronology.size())
			throw new Error("Not enough peers");

		int pos = from;
		for(Iterator<DAId> it = chronology.iterator();
		it.hasNext() && (pos - from) < length ;) {
			DAId daId = it.next();
			
			newMnPeers[pos] = daId;
			++pos;
			it.remove(); // remove from chronology list
			peers.remove(daId);
		}
	}

	public void addPeers(DAId[] newPeers) {
		for(int i = 0; i < newPeers.length; ++i) {
			if(peers.add(newPeers[i]))
				chronology.addFirst(newPeers[i]);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("mainPeer=").append(mainPeer.toString()).append('\n');
//		sb.append("chronologySize=").append(chronology.size()).append('\n');
//		sb.append("peersSize=").append(peers.size()).append('\n');
		for(Iterator<DAId> it = chronology.iterator(); it.hasNext();) {
			DAId id = it.next();
			sb.append(id.toString()).append('\n');
		}
		sb.append("end of routing table");
		return sb.toString();
	}

	public void reset(DAId mainPeer, DAId[] peers) {
		this.mainPeer = mainPeer;
		chronology.clear();
		this.peers.clear();
		
		for(DAId id : peers) {
			chronology.addLast(id);
			this.peers.add(id);
		}
	}
}
