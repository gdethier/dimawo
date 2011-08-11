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
package dimawo.middleware.overlay.mntree.messages;

import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.OverlayMessage;
import dimawo.middleware.overlay.mntree.MnId;

public class MnTreeMessage extends Message implements OverlayMessage {
	private static final long serialVersionUID = 1L;
	private MnId from, to;
	
	protected MnTreeMessage() {}
	
	protected MnTreeMessage(DAId daTo) {
		super(daTo);
	}
	
	protected MnTreeMessage(DAId daTo, MnId from, MnId to) {
		super(daTo);

		this.from = from;
		this.to = to;
	}
	
	protected MnTreeMessage(MnId from, MnId to) {
		this.from = from;
		this.to = to;
	}

	public MnId getSenderMn() {
		return from;
	}
	
	public MnId getRecipientMn() {
		return to;
	}
	
	public void setSenderMn(MnId from) {
		this.from = from;
	}
	
	public void setRecipientMn(MnId to) {
		this.to = to;
	}
}
