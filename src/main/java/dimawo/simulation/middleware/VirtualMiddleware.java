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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import dimawo.agents.AbstractAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.simulation.cluster.VirtualCluster;
import dimawo.simulation.middleware.events.AddHost;
import dimawo.simulation.middleware.events.CompletedTask;
import dimawo.simulation.middleware.events.DeleteJob;
import dimawo.simulation.middleware.events.InterruptedTask;
import dimawo.simulation.middleware.events.RemoveHost;
import dimawo.simulation.middleware.events.WaitForJobEvent;




public class VirtualMiddleware extends AbstractAgent {
	private VirtualCluster cluster;
	
	private TreeSet<String> idle;
	private TreeSet<String> busy;

	private LinkedList<VirtualTask> pendingTasks;
	private TreeMap<Integer, VirtualJob> pendingJobs;

	private int nextJobID;


	public VirtualMiddleware(VirtualCluster cluster) {
		super(null, "VirtualMiddleware");
		
		this.cluster = cluster;
		
		idle = new TreeSet<String>();
		busy = new TreeSet<String>();
		
		pendingJobs = new TreeMap<Integer, VirtualJob>();

		pendingTasks = new LinkedList<VirtualTask>();
	}

	public void addHost(String hostName) {
		try {
			submitMessage(new AddHost(hostName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void removeHost(String hostName) {
		try {
			submitMessage(new RemoveHost(hostName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void submitInterruptedTask(VirtualTask task, String hostName) {
		try {
			submitMessage(new InterruptedTask(task, hostName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void submitCompletedTask(VirtualTask task, String hostName) {
		try {
			submitMessage(new CompletedTask(task, hostName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int submitJob(VirtualJobDescription jobDesc) throws InterruptedException {
		SubmittedJob ss = new SubmittedJob(jobDesc); 
		try {
			submitMessage(ss);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ss.waitOn();
		return ss.getJobId();
	}

	public void waitForJob(int jobId) throws InterruptedException {
		WaitForJobEvent wje = new WaitForJobEvent(jobId);
		try {
			submitMessage(wje);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		wje.waitOn();
	}

	@Override
	protected void exit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof AddHost) {
			handleAddHost((AddHost) o);
		} else if(o instanceof RemoveHost) {
			handleRemoveHost((RemoveHost) o);
		} else if(o instanceof InterruptedTask) {
			handleInterruptedTask((InterruptedTask) o);
		} else if(o instanceof CompletedTask) {
			handleCompletedTask((CompletedTask) o);
		} else if(o instanceof SubmittedJob) {
			handleSubmittedJob((SubmittedJob) o);
		} else if(o instanceof WaitForJobEvent) {
			handleWaitForJobEvent((WaitForJobEvent) o);
		} else if(o instanceof DeleteJob) {
			handleDeleteJob((DeleteJob) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleDeleteJob(DeleteJob o) {
		VirtualJob job = pendingJobs.remove(o.getJobId());
		if(job != null) {
			LinkedList<String> hosts = new LinkedList<String>();
			job.getUsedHosts(hosts);
			for(String h : hosts) {
				cluster.interruptProcessOnHost(h);
			}
		}
		// interrupted tasks are signaled by hosts later
	}

	private void handleWaitForJobEvent(WaitForJobEvent o) {
		int jobId = o.getJobId();
		VirtualJob job = pendingJobs.get(jobId);
		if(job == null) {
			o.signalSuccess();
		} else {
			job.queueWaitForJob(o);
		}
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
	}
	
	private void handleAddHost(AddHost addHost) throws Exception {
		String hostName = addHost.getHost();
		agentPrintMessage("Add host "+hostName);
		if(idle.contains(hostName) ||
			busy.contains(hostName)) {
			agentPrintMessage("Host "+hostName+" already added.");
			return;
		}

		idle.add(hostName);

		schedulePendingTasks();
	}
	
	private void handleRemoveHost(RemoveHost remHost) {
		String hostName = remHost.getHostName();
		agentPrintMessage("Remove host "+hostName);
		idle.remove(hostName);
		busy.remove(hostName);
	}

	private void handleInterruptedTask(InterruptedTask it) throws Exception {
		VirtualTask task = it.getTask();
		String hostName = it.getHostName();
		agentPrintMessage("Interrupted task on host "+hostName);

		int jobID = task.getJobID();
		int taskID = task.getTaskID();
		VirtualJob job = pendingJobs.get(jobID);
		if(job != null) {
			if(! job.isTaskCompleted(taskID)) {
				pendingTasks.addFirst(task);
				job.setTaskRunning(taskID, null);
			}
		}

		// If task was interrupted, virtual host is down.
		busy.remove(hostName);
		idle.remove(hostName);

		schedulePendingTasks();
	}
	
	private void handleCompletedTask(CompletedTask ct) throws Exception {
		VirtualTask task = ct.getTask();
		String hostName = ct.getHostName();
		agentPrintMessage("Completed task "+task.getTaskID()+" on host "+hostName);

		int jobID = task.getJobID();
		VirtualJob job = pendingJobs.get(jobID);
		if(job == null)
			return; // job already completed

		int taskID = task.getTaskID();
		job.setTaskCompleted(taskID);
		
		// Check if job completed
		if(job.isCompleted()) {
			agentPrintMessage("Job #"+job.getJobID()+" completed.");
			job.signalJobCompleted();
			pendingJobs.remove(jobID);
		}
		
		if(busy.remove(hostName)) {
			idle.add(hostName);
			schedulePendingTasks();
		}
	}
	
	private void handleSubmittedJob(SubmittedJob sj) throws Exception {
		agentPrintMessage("New job submitted");
		VirtualJobDescription jobDesc = sj.getJobDescription();

		int jobId = nextJobID++;
		VirtualJob job = new VirtualJob(jobId, jobDesc, this);
		pendingJobs.put(jobId, job);
		sj.setJobId(jobId);
		sj.signalSuccess();

		if(job.getNumOfTasks() > 0) {
			pendingTasks.addAll(job.getTasks());
			schedulePendingTasks();
		}
	}
	
	private void schedulePendingTasks() throws Exception {
		while( ! idle.isEmpty() && ! pendingTasks.isEmpty()) {
			VirtualTask task = pendingTasks.removeFirst();
			int taskID = task.getTaskID();
			int jobID = task.getJobID();
			VirtualJob job = pendingJobs.get(jobID);
			
			if(! job.isTaskRunning(taskID) && ! job.isTaskCompleted(taskID)) {
				Iterator<String> it = idle.iterator();
				String hostName = it.next();
				it.remove();

				agentPrintMessage("Scheduling task "+taskID+" from job "+
					job.getJobID()+" on host "+hostName);
				
				cluster.executeProcessOnHost(hostName, task);
				job.setTaskRunning(taskID, hostName);
				busy.add(hostName);
			}
		}
	}
	
	public void deleteJob(int jobId) {
		try {
			submitMessage(new DeleteJob(jobId));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void configure(MiddlewareDescription middleDesc) {
		Collection<String> hosts = middleDesc.getHosts();
		for(String host : hosts) {
			addHost(host);
		}
	}
	
}
