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
package dimawo.middleware.fileSystem.fileTransfer.downloader;

import java.util.LinkedList;
import java.util.TreeMap;

import dimawo.middleware.fileSystem.fileTransfer.downloader.events.DownloadRequest;



public class WaitingLocationDownloads {
	private class Entry {
		int numReq = 0;
		LinkedList<DownloadRequest> queue = new LinkedList<DownloadRequest>();
	}
	
	private TreeMap<String, Entry> waitingLocationRequests;

	
	public WaitingLocationDownloads() {
		waitingLocationRequests = new TreeMap<String, Entry>();
	}

	/**
	 * 
	 * @param dr
	 * @return True if location is already awaited for this fileUID.
	 */
	public boolean queueRequest(DownloadRequest dr) {
		
		String fileUID = dr.getFileUID();
		Entry e = waitingLocationRequests.get(fileUID);
		if(e == null) {
			e = new Entry();
			waitingLocationRequests.put(fileUID, e);
			
			e.queue.add(dr);
			return false;
		} else {

			e.queue.add(dr);
			return true;
		
		}
	}
	
	public boolean queueRequests(String fileUID, LinkedList<DownloadRequest> reqs) {
		Entry e = waitingLocationRequests.get(fileUID);
		if(e == null) {
			e = new Entry();
			waitingLocationRequests.put(fileUID, e);
			e.queue.addAll(reqs);
			return false;
		} else {
			e.queue.addAll(reqs);
			return true;
		
		}
	}


	public LinkedList<DownloadRequest> remove(String fileUID) {
		Entry e = waitingLocationRequests.remove(fileUID);
		if(e != null)
			return e.queue;
		else
			return null;
	}

	public boolean newGetRetry(String fileUID, int maxRetries) {
		Entry e = waitingLocationRequests.get(fileUID);
		if(e == null)
			throw new Error("No waiting location for "+fileUID);
		
		++e.numReq;
		if(e.numReq > maxRetries) {
			return false;
		}
		
		return true;
	}

	public boolean containsQueue(String fileUID) {
		return waitingLocationRequests.containsKey(fileUID);
	}

}
