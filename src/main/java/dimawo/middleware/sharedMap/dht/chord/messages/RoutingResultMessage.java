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
import dimawo.middleware.sharedMap.dht.chord.ChordId;
import dimawo.middleware.sharedMap.dht.chord.ChordTableEntry;

public class RoutingResultMessage extends ChordAgentMessage {
	private ChordId key;
	private boolean isDest;
	private ChordTableEntry nextOrDest;

	public RoutingResultMessage(DAId to, ChordId chordFrom, ChordId chordTo,
			ChordId key, boolean isDest, ChordTableEntry nextOrDest) {
		super(to, chordFrom, chordTo);

		this.key = key;
		this.isDest = isDest;
		this.nextOrDest = nextOrDest;
	}
	
	public boolean isDestination() {
		return isDest;
	}

	public ChordTableEntry getNextOrDest() {
		return nextOrDest;
	}

	public ChordId getKey() {
		return key;
	}

}
