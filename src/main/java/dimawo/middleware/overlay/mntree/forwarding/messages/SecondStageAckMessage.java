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
package dimawo.middleware.overlay.mntree.forwarding.messages;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.mntree.MnId;
import dimawo.middleware.overlay.mntree.forwarding.MessageId;
import dimawo.middleware.overlay.mntree.forwarding.SourceMn;
import dimawo.middleware.overlay.mntree.forwarding.ReliableForwarder.ForwardType;
import dimawo.middleware.overlay.mntree.messages.MnTreeMessage;

public class SecondStageAckMessage extends MnTreeMessage
implements ReliableForwarderMessage {
	private SourceMn src;
	private MessageId ackedMsgId;
	private ForwardType type;

	public SecondStageAckMessage(DAId daTo, MnId from, MnId to,
			MessageId ackedMsgId, ForwardType type, SourceMn src) {
		super(daTo, from, to);
		this.ackedMsgId = ackedMsgId;
		this.type = type;
		this.src = src;
	}
	
	public MessageId getAckedMessageId() {
		return ackedMsgId;
	}
	
	public SourceMn getSource() {
		return src;
	}

	public ForwardType getAckedMessageType() {
		return type;
	}
}
