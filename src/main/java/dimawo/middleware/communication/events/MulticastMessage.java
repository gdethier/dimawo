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
package dimawo.middleware.communication.events;

import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MulticastInstanceMessage;
import dimawo.middleware.distributedAgent.DAId;

public class MulticastMessage {

	private static final long serialVersionUID = 1L;

	private DAId[] toIds;
	private Message msg;

	private int nInstances;
	private int successes;
	private int failures;

	
	public MulticastMessage(DAId[] toIds, Message msg) {
		this.toIds = toIds;
		this.msg = msg;

		nInstances = toIds.length;
	}
	
	public void setToIds(DAId[] toIds) {
		this.toIds = toIds;
	}

	public DAId[] getToIds() {
		return toIds;
	}
	
	public Message getMessage() {
		return msg;
	}
	
//	public MOSCallBack getErrorCallBack() {
//		return msg.getCallBack();
//	}

	public synchronized void signalSent(boolean success) {
		
		if(success)
			++successes;
		else
			++failures;
		
//		System.out.println("Broadcasted message: "+msg.getClass().getName());
//		System.out.println((successes+failures)+"/"+nInstances+" messages sent.");

		if(successes + failures == nInstances) {
//			System.out.println(successes+" were successfully sent, "+failures+" not.");
			msg.setMessageSent(true);
		}
	}

	public MulticastInstanceMessage getMulticastInstanceMessage(DAId to) {
		return new MulticastInstanceMessage(to, this);
	}
}
