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

import dimawo.middleware.distributedAgent.DAId;



public class MessageId implements Serializable, Comparable<MessageId> {
	private DAId daId;
	private int seqNum;
	
	public MessageId(DAId daId, int seqNum) {
		this.daId = daId;
		this.seqNum = seqNum;
	}

	@Override
	public int compareTo(MessageId o) {
		int c = seqNum - o.seqNum;
		if(c == 0) {
			c = daId.compareTo(o.daId);
		}
		return c;
	}
	
	public String toString() {
		return daId.toString() + ":" + seqNum;
	}
}
