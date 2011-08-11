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
package dimawo.middleware.sharedMap.dht.chord.messages;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.sharedMap.dht.chord.ChordId;

public class PutDataEntryResultMessage extends ChordAgentMessage implements
		CallStateMessage {
	private static final long serialVersionUID = 1L;
	
	private SharedMapEntry entry;
	private boolean isRoutingError;

	public PutDataEntryResultMessage(DAId to, ChordId chordFrom, ChordId chordTo,
			SharedMapEntry added, boolean isRoutingError) {
		super(to, chordFrom, chordTo);

		this.entry = added;
		this.isRoutingError = isRoutingError;
	}
	
	public SharedMapEntry getEntry() {
		return entry;
	}
	
	public boolean isRoutingError() {
		return isRoutingError;
	}

}
