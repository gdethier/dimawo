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
package dimawo.simulation.middleware;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import dimawo.simulation.middleware.events.WaitForJobEvent;



public class VirtualJob {
	
	private VirtualJobDescription jobDesc;
	private LinkedList<VirtualTask> tasks;
	private int jobID;
	
	private TreeSet<Integer> completedTasks;
	private TreeMap<Integer, String> runningTasks; // task -> hostName
	private LinkedList<WaitForJobEvent> waiting;


	public VirtualJob(int jobID, VirtualJobDescription jobDesc,
			VirtualMiddleware middleware) {
		this.jobDesc = jobDesc;
		this.jobID = jobID;
		
		tasks = new LinkedList<VirtualTask>();
		int nextTaskId = 0;
		for(VirtualTaskDescription taskDesc : jobDesc.getTaskDescriptions()) {
			tasks.addLast(new VirtualTask(jobID, nextTaskId++, taskDesc,
					middleware));
		}
		
		runningTasks = new TreeMap<Integer, String>();
		completedTasks = new TreeSet<Integer>();
		waiting = new LinkedList<WaitForJobEvent>();
	}

	public int getJobID() {
		return jobID;
	}

	public int getNumOfTasks() {
		return jobDesc.getTaskDescriptions().size();
	}

	public LinkedList<VirtualTask> getTasks() {
		return tasks;
	}

	public boolean isTaskRunning(int taskID) {
		return runningTasks.containsKey(taskID);
	}

	public void setTaskRunning(int taskID, String hostName) {
		if(hostName != null)
			runningTasks.put(taskID, hostName);
		else
			runningTasks.remove(taskID);
	}

	public boolean isTaskCompleted(int taskID) {
		return completedTasks.contains(taskID);
	}

	public void setTaskCompleted(int taskID) {
		completedTasks.add(taskID);
		runningTasks.remove(taskID);
	}

	public boolean isCompleted() {
		return completedTasks.size() == tasks.size();
	}

	public void signalJobCompleted() {
		for(WaitForJobEvent w : waiting) {
			w.signalSuccess();
		}
	}

	public void queueWaitForJob(WaitForJobEvent o) {
		waiting.add(o);
	}

	public void getUsedHosts(Collection<String> dest) {
		dest.addAll(runningTasks.values());
	}

}
