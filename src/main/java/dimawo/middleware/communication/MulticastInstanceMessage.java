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

import dimawo.middleware.communication.events.MulticastMessage;
import dimawo.middleware.distributedAgent.DAId;

public class MulticastInstanceMessage extends Message {

	private static final long serialVersionUID = 1L;
	
	private Message msg;
	private transient MulticastMessage multiMsg;


	public MulticastInstanceMessage(DAId to, MulticastMessage multiMsg) {
		super(to);
		this.multiMsg = multiMsg;
		this.msg = multiMsg.getMessage();
		
		if(this.msg instanceof MulticastInstanceMessage)
			throw new Error("A MulticastInstanceMessage cannot be sent through another one");
	}
	
	public Message getMessage() {
		return msg;
	}
	
	@Override
	public void setMessageSent(boolean success) {
		multiMsg.signalSent(success);
	}

}
