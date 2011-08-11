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
package dimawo.simulation;

import java.util.LinkedList;
import java.util.List;

import dimawo.simulation.middleware.VirtualJobDescription;



public class DeploymentDescription {
	private LinkedList<ExecCommand> processes;
	private LinkedList<VirtualJobDescription> jobs;
	
	public DeploymentDescription() {
		processes = new LinkedList<ExecCommand>();
		jobs = new LinkedList<VirtualJobDescription>();
	}
	
	public void addProcess(ExecCommand cmd) {
		processes.add(cmd);
	}
	
	public void addJob(VirtualJobDescription job) {
		jobs.add(job);
	}

	public List<ExecCommand> getProcesses() {
		return processes;
	}
	
	public List<VirtualJobDescription> getJobs() {
		return jobs;
	}
}
