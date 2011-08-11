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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dimawo.middleware.distributedAgent.DAId;



public class ChordId implements Serializable, Comparable<ChordId> {
	private static final long serialVersionUID = 1L;

	public static final int NBITS = 160; // 20 bytes from SHA
	private BigInteger key;
	private static BigInteger MAX_KEY = new BigInteger("2").pow(NBITS);
	
	public ChordId(String key) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			this.key = new BigInteger(1, md.digest(key.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
	}

	public ChordId(DAId id) {
		this(id.toString());
	}

	/**
	 * Low exclusive, up inclusive.
	 * 
	 * @param lowChordId
	 * @param upChordId
	 * @return
	 * @throws ChordException 
	 */
	public boolean isInInterval(ChordId lowChordId, ChordId upChordId) {
		BigInteger low = lowChordId.key;
		BigInteger up = upChordId.key;
		
		int c = low.compareTo(up);
		if(c < 0) { // low <= up
			return low.compareTo(key) < 0 &&
				key.compareTo(up) <= 0;
		} else if(c > 0) {
			return (low.compareTo(key) < 0 &&
					key.compareTo(MAX_KEY) < 0) ||
					(BigInteger.ZERO.compareTo(key) <= 0 &&
							key.compareTo(up) <= 0);
		} else { // c == 0
			return true;
		}
	}
	
	public boolean equals(Object o) {
		if(! (o instanceof ChordId))
			return false;
		
		ChordId other = (ChordId) o;
		return key.equals(other.key);
	}
	
	@Override
	public String toString() {
		return "ChordId{"+key.toString()+"}";
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public int compareTo(ChordId o) {
		return key.compareTo(o.key);
	}
	
}
