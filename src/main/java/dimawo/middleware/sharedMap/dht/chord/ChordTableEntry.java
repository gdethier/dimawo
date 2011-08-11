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
package dimawo.middleware.sharedMap.dht.chord;

import java.io.Serializable;

import dimawo.middleware.distributedAgent.DAId;



public class ChordTableEntry implements Serializable, Comparable<ChordTableEntry> {
	private static final long serialVersionUID = 1L;
	private ChordId chordId;
	private DAId daId;

	public ChordTableEntry(DAId daId, ChordId chordId) {
		this.daId = daId;
		this.chordId = chordId;
	}

	public ChordId getChordId() {
		return chordId;
	}

	public DAId getDaId() {
		return daId;
	}
	
	public String toString() {
		return daId+"/"+chordId;
	}

	@Override
	public int compareTo(ChordTableEntry o) {
		return chordId.compareTo(o.chordId);
	}
	
	public int hashCode() {
		return chordId.hashCode();
	}
	
	public boolean equals(Object o) {
		if( ! (o instanceof ChordTableEntry))
			return false;
		
		ChordTableEntry e = (ChordTableEntry) o;
		return chordId.equals(e.chordId);
	}
}
