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

public class UpdateDataEntryResultMessage extends ChordAgentMessage implements
		CallStateMessage {
	private String key;
	private SharedMapEntry updated;
	private boolean isRoutingError;
	
	public UpdateDataEntryResultMessage(DAId to, ChordId chordFrom,
			ChordId chordTo, String key, SharedMapEntry updated, boolean isRoutingError) {
		super(to, chordFrom, chordTo);

		this.key = key;
		this.updated = updated;
		this.isRoutingError = isRoutingError;
	}

	public String getKey() {
		return key;
	}
	
	public SharedMapEntry getUpdated() {
		return updated;
	}

	public boolean isRoutingError() {
		return isRoutingError;
	}

}
