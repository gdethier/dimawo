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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

public class MessageHandlersRegistry {
	private HashMap<Object, LinkedList<MessageHandler>> registry;
//	private TreeMap<Comparable, LinkedList<MessageHandler>> registry;

	public MessageHandlersRegistry() {
		registry = new HashMap<Object, LinkedList<MessageHandler>>();
//		registry = new TreeMap<Comparable, LinkedList<MessageHandler>>();
	}

	public synchronized void register(Object handlerId, MessageHandler handler) {
		LinkedList<MessageHandler> handlers = registry.get(handlerId);
		if(handlers == null) {
			handlers = new LinkedList<MessageHandler>();
			registry.put(handlerId, handlers);
		}
		
		handlers.add(handler);
	}
	
	public synchronized void unregister(Object handlerId, MessageHandler handler) {
		LinkedList<MessageHandler> handlers = registry.get(handlerId);
		if(handlers != null) {
			for(Iterator<MessageHandler> it = handlers.iterator(); it.hasNext();) {
				MessageHandler hand = it.next();
				if(hand.equals(handler)) {
					it.remove();
					return;
				}
			}
		}
	}

	public boolean submitMessageToHandlers(Message msg) {
		Object handlerId = msg.getHandlerId();
		if(handlerId == null)
			return false;

		synchronized(this) {
			LinkedList<MessageHandler> handlers = registry.get(handlerId);
			if(handlers == null)
				return false;
			for(MessageHandler mh : handlers) {
				mh.submitIncomingMessage(msg);
			}
			return true;
		}
	}
}
