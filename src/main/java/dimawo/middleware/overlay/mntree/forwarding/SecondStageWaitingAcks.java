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
package dimawo.middleware.overlay.mntree.forwarding;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SecondStageWaitingAcks {
	public static class Destinations {
		// dest[0] = parent, dest[1..n] = children
		// 0 means no ack or ack already received, 1 means still waiting
		private boolean[] dest;
		private int awaitedAcks;
		
		public Destinations(int maxNumOfChildren) {
			dest = new boolean[maxNumOfChildren + 1];
			awaitedAcks = 0;
		}
		
		public void setWaitAckFromParent() {
			dest[0] = true;
			++awaitedAcks;
		}
		
		public void setWaitAckFromChild(int childIndex) {
			dest[childIndex + 1] = true;
			++awaitedAcks;
		}
		
		public void ackFromParent() {
			if(dest[0]) {
				dest[0] = false;
				--awaitedAcks;
			}
		}
		
		public void ackFromChild(int childIndex) {
			if(dest[childIndex + 1]) {
				dest[childIndex + 1] = false;
				--awaitedAcks;
			}
		}
		
		public boolean waitsForAcks() {
			return awaitedAcks > 0;
		}

		public boolean waitsForParent() {
			return dest[0];
		}
		
		public boolean waitsForChild(int childIndex) {
			return dest[childIndex + 1];
		}
	}
	
	private TreeMap<MessageId, Destinations> map;
	
	
	public SecondStageWaitingAcks() {
		map = new TreeMap<MessageId, Destinations>();
	}
	
	public void waitForAcks(Destinations dest, MessageId id) {
		if(dest.waitsForAcks()) {
			map.put(id, dest);
		}
	}
	
	/**
	 * @param msgId
	 * @return True if all acks received
	 * @throws CacheException 
	 */
	public boolean ackFromParent(MessageId msgId) throws CacheException {
		Destinations e = map.get(msgId);
		if(e == null)
			throw new CacheException("No entry for id "+msgId);
		e.ackFromParent();
		if(! e.waitsForAcks()) {
			map.remove(msgId);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param msgId
	 * @return True if all acks received
	 * @throws CacheException 
	 */
	public boolean ackFromChild(int childIndex, MessageId msgId) throws CacheException {
		Destinations e = map.get(msgId);
		if(e == null)
			throw new CacheException("No entry for id "+msgId);
		e.ackFromChild(childIndex);
		if(! e.waitsForAcks()) {
			map.remove(msgId);
			return true;
		}
		
		return false;
	}

	public boolean isWaiting(MessageId msgId) {
		Destinations e = map.get(msgId);
		if(e == null)
			return false;
		
		return e.waitsForAcks();
	}

//	public void remove(MessageId msgId) {
//		map.remove(msgId);
//	}

	public boolean hasEntry(MessageId msgId) {
		return map.containsKey(msgId);
	}

	public void listWaitingForParent(LinkedList<MessageId> infos) {
		for(Entry<MessageId, Destinations> e : map.entrySet()) {
			if(e.getValue().waitsForParent()) {
				infos.add(e.getKey());
			}
		}
	}
	
	public void listWaitingForChild(int childIndex, LinkedList<MessageId> infos) {
		for(Entry<MessageId, Destinations> e : map.entrySet()) {
			if(e.getValue().waitsForChild(childIndex)) {
				infos.add(e.getKey());
			}
		}
	}
}
