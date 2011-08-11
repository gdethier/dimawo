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
package dimawo.middleware.fileSystem.fileDB;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.Update;



public class FileLocationUpdateValue implements Serializable, Update {

	private static final long serialVersionUID = 1L;

	private String fileUID;
	private LinkedList<DAId> toAdd, toRemove;
	
	
	public FileLocationUpdateValue(String fileUID) {
		this.fileUID = fileUID;
	}

	public void setLocationsToRemove(LinkedList<DAId> l) {
		toRemove = l;
	}

	public Collection<DAId> getLocationsToRemove() {
		return toRemove;
	}


	public void setLocationsToAdd(LinkedList<DAId> l) {
		toAdd = l;
	}

	public Collection<DAId> getLocationsToAdd() {
		return toAdd;
	}
	
	public void addLocationToAdd(DAId daId) {
		if(toAdd == null)
			toAdd = new LinkedList<DAId>();
		toAdd.addLast(daId);
	}
	
	public void addLocationToRemove(DAId daId) {
		if(toRemove == null)
			toRemove = new LinkedList<DAId>();
		toRemove.addLast(daId);
	}

	public Object getFileUID() {
		return fileUID;
	}

}
