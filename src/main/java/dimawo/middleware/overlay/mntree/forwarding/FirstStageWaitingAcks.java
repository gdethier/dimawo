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
import java.util.Map.Entry;
import java.util.TreeMap;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.mntree.forwarding.messages.FirstStageAckMessage;



public class FirstStageWaitingAcks {
	private TreeMap<MessageId, FirstStageAckMessage> map;
	
	
	public FirstStageWaitingAcks() {
		map = new TreeMap<MessageId, FirstStageAckMessage>();
	}
	
	public void waitAck(MessageId msgId, FirstStageAckMessage fsm) {
		map.put(msgId, fsm);
	}
	
	public FirstStageAckMessage ack(MessageId msgId) {
		return map.remove(msgId);
	}

	public void removeReferences(DAId daId) {
		for(Iterator<Entry<MessageId, FirstStageAckMessage>> it = map.entrySet().iterator();
		it.hasNext();) {
			Entry<MessageId, FirstStageAckMessage> e = it.next();
			FirstStageAckMessage fsm = e.getValue();
			if(fsm.getRecipient().equals(daId)) {
				it.remove();
			}
		}
	}

}
