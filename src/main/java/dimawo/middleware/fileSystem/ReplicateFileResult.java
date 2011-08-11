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

import java.util.Set;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class ReplicateFileResult {
	private String fileUID;
	private DFSException error;

	private Set<DAId> success;
	private Set<DAId> failure;
	
	public ReplicateFileResult(String fileUID,
			Set<DAId> success,
			Set<DAId> failure,
			DFSException error) {
		this.fileUID = fileUID;
		this.success = success;
		this.failure = failure;
		this.error = error;
	}
	
	public String getFileUID() {
		return fileUID;
	}
	
	public Set<DAId> getSuccess() {
		return success;
	}
	
	public Set<DAId> getFailure() {
		return failure;
	}
	
	public DFSException getError() {
		return error;
	}
}
