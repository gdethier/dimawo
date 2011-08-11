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
package dimawo.middleware.communication;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import dimawo.middleware.distributedAgent.DAId;



public class QueuedDatagramMessages {
	
	private TreeMap<DAId, LinkedList<Message>> queues;

	
	public QueuedDatagramMessages() {
		queues = new TreeMap<DAId, LinkedList<Message>>();
	}

	/**
	 * Inserts datagram message into queue associated to destination.
	 * 
	 * @param m A Datagram Message
	 * @return True if messages are already queued for destination,
	 * false otherwise.
	 */
	public boolean queueDatagramMessage(Message m) {
		LinkedList<Message> q = queues.get(m.getRecipient());
		if(q != null) {
			q.addLast(m);
			return true;
		} else {
			q = new LinkedList<Message>();
			q.addLast(m);
			queues.put(m.getRecipient(), q);
			return false;
		}
	}

	public LinkedList<Message> removeQueue(DAId daId) {
		return queues.remove(daId);
	}

	public boolean isEmpty() {
		return queues.isEmpty();
	}

	public boolean hasQueue(DAId daId) {
		return queues.containsKey(daId);
	}

	public void rejectAll() {
		Iterator<LinkedList<Message>> queuesIt = queues.values().iterator();
		while(queuesIt.hasNext()) {
			LinkedList<Message> queue = queuesIt.next();
			Iterator<Message> msgIt = queue.iterator();
			while(msgIt.hasNext()) {
				Message msg = msgIt.next();
				msg.setMessageSent(false);
			}
		}
		queues.clear();
	}

}
