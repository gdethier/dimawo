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

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import dimawo.middleware.distributedAgent.DAId;




public class RemoteFilesDB {

	protected Map<String, FileLocation> files;
	
	public RemoteFilesDB() {
		files = new TreeMap<String, FileLocation>();
	}
	
	public void replaceEntry(FileLocation loc) throws FileDBException {
		files.put(loc.getFileUID(), loc);
	}
	
	
	public synchronized void addFile(String fileUID, FileLocation location)
	throws FileDBException {
		if(files.containsKey(fileUID)) {
			throw new FileDBException("File already present "+fileUID);
		}
		
		files.put(fileUID, location);
	}
	
	public void updateFile(String fileUID, FileLocation loc) {
		FileLocation thisLoc = files.get(fileUID);
		if(thisLoc == null) {
			files.put(fileUID, loc);
		} else {
			thisLoc.addRemoteFileLocations(loc.getFileRemoteLocations());
		}
	}
	
	public synchronized FileLocation addRemoteFileLocation(String fileUID, DAId daId) {
		FileLocation fl = files.get(fileUID);
		if(fl == null) {
			fl = new FileLocation(fileUID);
			files.put(fileUID, fl);
		}

		fl.addRemoteFileLocation(daId);
		return fl;
	}
	
	public synchronized void removeRemoteFileLocation(String fileUID, DAId daId) throws FileNotFoundException {
		FileLocation fl = files.get(fileUID);
		if(fl == null) {
			throw new FileNotFoundException(fileUID);
		}

		fl.removeRemoteFileLocation(daId);
		if(fl.isEmpty()) {
			// No locations are associated with this fileUID
			files.remove(fileUID);
		}
	}

	public synchronized void removeLocation(String fileUID) {
		files.remove(fileUID);
	}

	public LinkedList<String> removeFromFilesLocations(DAId daId) {
		LinkedList<String> list = new LinkedList<String>();
		Iterator<Entry<String, FileLocation>> it = files.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, FileLocation> e = it.next();
			FileLocation fl = e.getValue();
			if(fl.removeRemoteFileLocation(daId)) {
				list.add(e.getKey());
			}
			if(fl.isEmpty()) {
				it.remove();
			}
		}
		return list;
	}

	public FileLocation getLocation(String fileUID) {
		return files.get(fileUID);
	}
	
}
