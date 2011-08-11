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

public class SharedMapUpdateResult extends SharedMapResult {
	private String key;
	private Object updateData;
	private SharedMapEntry newEntry;
	
	public SharedMapUpdateResult(String key, Object updateData, SharedMapEntry e) {
		this.key = key;
		this.updateData = updateData;
		this.newEntry = e;
	}
	
	public String getKey() { 
		return key;
	}
	
	public Object getUpdateData() {
		return updateData;
	}
	
	public SharedMapEntry getEntry() {
		return newEntry;
	}
}
