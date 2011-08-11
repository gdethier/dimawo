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
package dimawo.middleware.overlay;

import java.io.Serializable;



public class SharedMapEntry implements Serializable, Updatable, Comparable<SharedMapEntry> {

	private static final long serialVersionUID = 1L;

	private String key;
	private SharedMapValue val;

	public SharedMapEntry(String key, SharedMapValue val) {
		this.key = key;
		this.val = val;
	}
	
	public String getKey() {
		return key;
	}
	
	public SharedMapValue getValue() {
		return val;
	}

	public boolean update(Update updateData) {
		if(val instanceof Updatable) {
			Updatable up = (Updatable) val;
			return up.update(updateData);
		}
		
		return false;
	}
	
	public int hashCode() {
		return key.hashCode();
	}
	
	public SharedMapEntry clone() {
		return new SharedMapEntry(key, val.clone());
	}
	
	public boolean equals(Object o) {
		if(! (o instanceof SharedMapEntry))
			return false;
		
		SharedMapEntry e = (SharedMapEntry) o;
		return e.key.equals(key);
	}

	@Override
	public int compareTo(SharedMapEntry o) {
		return key.compareTo(o.key);
	}
}
