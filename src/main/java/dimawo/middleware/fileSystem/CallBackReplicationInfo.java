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

import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class CallBackReplicationInfo {

	protected TreeSet<DAId> todo;
	protected TreeSet<DAId> success;
	protected TreeSet<DAId> failure;

	protected FileSystemCallBack cb;
	
	public CallBackReplicationInfo(FileSystemCallBack cb, TreeSet<DAId> replicaLocations) {
		this.cb = cb;
		this.todo = replicaLocations;

		success = new TreeSet<DAId>();
		failure = new TreeSet<DAId>();
	}
	
	public CallBackReplicationInfo(FileSystemCallBack cb, TreeSet<DAId> todo,
			TreeSet<DAId> succ, TreeSet<DAId> fail) {
		this.cb = cb;
		this.todo = todo;
		this.success = succ;
		this.failure = fail;
	}

	public boolean replicationsArePending() {
		return todo.size() > 0;
	}

	public void succeeded(DAId id) {
		if(todo.remove(id))
			success.add(id);
	}
	
	public boolean failed(DAId id) {
		if(todo.remove(id)) {
			failure.add(id);
			return true;
		}
		return false;
	}

	public FileSystemCallBack getCallBack() {
		return cb;
	}

	public TreeSet<DAId> getSuccess() {
		return success;
	}

	public TreeSet<DAId> getFailure() {
		return failure;
	}
}
