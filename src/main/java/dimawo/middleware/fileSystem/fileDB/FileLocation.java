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

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.SharedMapValue;
import dimawo.middleware.overlay.Updatable;
import dimawo.middleware.overlay.Update;



public class FileLocation
implements SharedMapValue, Updatable {

	private static final long serialVersionUID = 1L;

	private String fileUID;
	protected TreeSet<DAId> remoteFileLocations;
	
	public FileLocation(String fileUID) {
		
		this.fileUID = fileUID;
		remoteFileLocations = new TreeSet<DAId>();
	}

	public FileLocation(String fileUID, TreeSet<DAId> locations) {
		
		this.fileUID = fileUID;
		remoteFileLocations = locations;

	}

	public void setFileLocations(Collection<DAId> locations) {
		remoteFileLocations.clear();
		this.remoteFileLocations.addAll(locations);
	}
	
	public TreeSet<DAId> getFileRemoteLocations() {
		return remoteFileLocations;
	}

	public void addRemoteFileLocation(DAId daId) {
		remoteFileLocations.add(daId);
	}
	
	public void addRemoteFileLocations(Collection<DAId> newLoc) {
		remoteFileLocations.addAll(newLoc);
	}

	public boolean removeRemoteFileLocation(DAId daId) {
		return remoteFileLocations.remove(daId);
	}
	
	@Override
	public boolean update(Update v) {
		if(! (v instanceof FileLocationUpdateValue)) {
			return false;
		}
		
		FileLocationUpdateValue up = (FileLocationUpdateValue) v;
		if(! up.getFileUID().equals(fileUID))
			return false;
		
		Collection<DAId> toRemove = up.getLocationsToRemove();
		Collection<DAId> toAdd = up.getLocationsToAdd();
		
		if(toAdd != null)
			remoteFileLocations.addAll(toAdd);
		if(toRemove != null)
			remoteFileLocations.removeAll(toRemove);

		if(remoteFileLocations.isEmpty()) {
			System.err.println("No more locations available for file "+fileUID);
		}
		
		return true;
	}
	
	@Override
	public FileLocation clone() {

		FileLocation loc = new FileLocation(fileUID);
		loc.addRemoteFileLocations(remoteFileLocations);

		return loc;

	}

	public String getFileUID() {
		return fileUID;
	}

	public boolean isEmpty() {
		return remoteFileLocations.isEmpty();
	}

	public DAId getRandomLocation() {
		
		if(remoteFileLocations.size() == 0)
			throw new Error("No location available");
		
		DAId first = remoteFileLocations.first();
		DAId last = remoteFileLocations.last();
		
		if(first.equals(last))
			return first;
		
		Random r = new Random(System.currentTimeMillis());
		int pos = r.nextInt(remoteFileLocations.size());
		
		Iterator<DAId> it = remoteFileLocations.iterator();
		DAId id = it.next();
		for(int i = 0; i < pos; ++i) {
			if(! it.hasNext())
				break;
			id = it.next();			
		}
		
		return id;

	}

	public void removeRemoteFileLocations(Collection<DAId> serversBlackList) {
		remoteFileLocations.removeAll(serversBlackList);
	}

}
