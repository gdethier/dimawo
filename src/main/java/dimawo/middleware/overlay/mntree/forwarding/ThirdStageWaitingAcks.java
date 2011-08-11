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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class ThirdStageWaitingAcks {
	public static class Destinations {
		private TreeSet<DAId> dest;
//		private int dest;
		
		public Destinations() {
			dest = new TreeSet<DAId>();
//			dest = 0;
		}
		
		public void waitFrom(DAId id) {
			dest.add(id);
//			++dest;
		}
		
		public boolean ackFrom(DAId id) {
			return dest.remove(id);
//			--dest;
		}
		
		public boolean waitsForAcks() {
			return ! dest.isEmpty();
//			return dest != 0;
		}

		public boolean isWaitingFor(DAId daId) {
			return dest.contains(daId);
		}
	}
	
	private TreeMap<MessageId, Destinations> map;
	
	public ThirdStageWaitingAcks() {
		map = new TreeMap<MessageId, Destinations>();
	}
	
	public void waitForAcks(Destinations dest, MessageId msgId) {
		if(! dest.waitsForAcks())
			return;
		if(map.put(msgId, dest) != null)
			throw new Error("Entry already present for message "+msgId);
	}

	/**
	 * @param msgId
	 * @param sender
	 * @return True if all acks were received
	 */
	public boolean ack(MessageId msgId, DAId sender) {
		Destinations dest = map.get(msgId);
		if(dest == null)
			return false;
		dest.ackFrom(sender);
		if(! dest.waitsForAcks()) {
			map.remove(msgId);
			return true;
		}
		
		return false;
	}

	public boolean isWaiting(MessageId msgId) {
		Destinations dest = map.get(msgId);
		if(dest == null)
			return false;
		return dest.waitsForAcks();
	}

	public boolean remove(MessageId msgId) {
		return map.remove(msgId) != null;
	}

	public void listWaitingAcks(DAId daId, LinkedList<MessageId> thirdAckMessages) {
		for(Iterator<Entry<MessageId, Destinations>> it = map.entrySet().iterator();
		it.hasNext();) {
			Entry<MessageId, Destinations> e = it.next();
			Destinations dest = e.getValue();
			if(dest.isWaitingFor(daId)) {
				thirdAckMessages.add(e.getKey());
			}
		}
	}
}
