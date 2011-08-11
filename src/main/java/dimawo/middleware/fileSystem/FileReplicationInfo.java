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
package dimawo.middleware.fileSystem;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.faultdetection.FaultDetectionAgent;



public class FileReplicationInfo {
	private FileSystemAgent fsPeer;
	private String fileUID;
	
	private TreeSet<DAId> requestedReplications;
	private TreeSet<DAId> success;
	private TreeSet<DAId> failures;
	private LinkedList<CallBackReplicationInfo> cbInfos;
	
	public FileReplicationInfo(String fileUID, FileSystemAgent fsPeer) {
		this.fsPeer = fsPeer;
		this.fileUID = fileUID;

		requestedReplications = new TreeSet<DAId>();
		success = new TreeSet<DAId>();
		failures = new TreeSet<DAId>();
		cbInfos = new LinkedList<CallBackReplicationInfo>();
	}

	public void addCallBack(String fileUID, FileSystemCallBack cb, Set<DAId> repDest,
			Set<DAId> toDownload) {
		TreeSet<DAId> todo = new TreeSet<DAId>();
		TreeSet<DAId> succ = new TreeSet<DAId>();
		TreeSet<DAId> fail = new TreeSet<DAId>();
		
		for(DAId id : repDest) {
			if(requestedReplications.add(id)) {
				if(success.contains(id)) {
					succ.add(id);
				} else if(failures.contains(id)) {
					fail.add(id);
				} else {
					todo.add(id);
				}
			} else {
				todo.add(id);
				toDownload.add(id);
			}
		}
		
		if(todo.isEmpty()) {
			ReplicateFileResult res = new ReplicateFileResult(fileUID, succ, fail, null);
			cb.replicateFileCB(res);
			return;
		}
		
		requestedReplications.addAll(todo);
		
		CallBackReplicationInfo inf = new CallBackReplicationInfo(cb, todo, succ, fail);
		cbInfos.add(inf);
	}

	public void signalFailure(DAId daId, FaultDetectionAgent ping) {
		failures.add(daId);
		
		for(Iterator<CallBackReplicationInfo> it = cbInfos.iterator(); it.hasNext();) {
			CallBackReplicationInfo inf = it.next();

			if(inf.failed(daId))
				ping.unregister(daId, fsPeer);

			if(! inf.replicationsArePending()) {
				it.remove();
				
				ReplicateFileResult res = new ReplicateFileResult(fileUID,
						inf.getSuccess(),
						inf.getFailure(),
						null);
				inf.getCallBack().replicateFileCB(res);
			}
		}
	}

	public boolean cbsArePending() {
		return ! cbInfos.isEmpty();
	}

	public void signalSuccess(DAId sourceDaId) {
		success.add(sourceDaId);
		
		for(Iterator<CallBackReplicationInfo> it = cbInfos.iterator(); it.hasNext();) {
			CallBackReplicationInfo inf = it.next();

			inf.succeeded(sourceDaId);
			if(! inf.replicationsArePending()) {
				it.remove();
				
				ReplicateFileResult res = new ReplicateFileResult(fileUID,
						inf.getSuccess(),
						inf.getFailure(),
						null);
				inf.getCallBack().replicateFileCB(res);
			}
		}
	}
}
