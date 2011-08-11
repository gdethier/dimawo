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

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.fileSystem.fileTransfer.downloader.events.DownloadRequest;



public class DownloadCallBacks {
	
	private TreeMap<DAId, TreeMap<String, LinkedList<DownloadRequest>>> queue;
	
	public DownloadCallBacks() {
		queue = new TreeMap<DAId, TreeMap<String, LinkedList<DownloadRequest>>>();
	}
	

	public LinkedList<DownloadRequest> dequeueRequests(DAId serverId,
			String fileUID) {
		if(serverId == null)
			throw new NullPointerException();
		if(fileUID == null)
			throw new NullPointerException();
		
		LinkedList<DownloadRequest> list = new LinkedList<DownloadRequest>();
		
		TreeMap<String, LinkedList<DownloadRequest>> serverQueue = queue.get(serverId);
		if(serverQueue == null)
			return list;
		
		LinkedList<DownloadRequest> fileQueue = serverQueue.remove(fileUID);
		if(fileQueue != null)
			list.addAll(fileQueue);
		
		return list;
	}

	public void queueRequest(DAId sourceDAID, String fileUID, DownloadRequest dr) {
		TreeMap<String, LinkedList<DownloadRequest>> serverQueue = queue.get(sourceDAID);
		if(serverQueue == null) {
			serverQueue = new TreeMap<String, LinkedList<DownloadRequest>>();
			queue.put(sourceDAID, serverQueue);
		}
		
		LinkedList<DownloadRequest> fileQueue = serverQueue.get(fileUID);
		if(fileQueue == null) {
			fileQueue = new LinkedList<DownloadRequest>();
			serverQueue.put(fileUID, fileQueue);
		}
		
		fileQueue.add(dr);
	}

}
