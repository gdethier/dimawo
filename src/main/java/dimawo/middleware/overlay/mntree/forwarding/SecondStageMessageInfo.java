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

import java.io.Serializable;

import dimawo.middleware.communication.Message;
import dimawo.middleware.overlay.mntree.forwarding.ReliableForwarder.ForwardType;



public class SecondStageMessageInfo implements Serializable {
//	private SecondStageMessageId msgId;
	private MessageId msgId;
	private ForwardType type;
	private SourceMn src;
	private Message m;
	
	public SecondStageMessageInfo(MessageId msgId, ForwardType type, SourceMn src, Message m) {
		this.msgId = msgId;
		this.type = type;
		this.src = src;
		if(src == null)
			throw new Error("No source given");
		this.m = m;
	}
	
	public MessageId getMessageId() {
		return msgId;
	}
	
	public ForwardType getForwardType() {
		return type;
	}
	
	public SourceMn getSource() {
		return src;
	}
	
	public Message getMessage() {
		return m;
	}
}
