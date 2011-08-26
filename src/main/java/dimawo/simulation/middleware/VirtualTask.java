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

import java.util.concurrent.Semaphore;

import dimawo.exec.WorkerProcess;
import dimawo.simulation.host.HostAccess;


public class VirtualTask {
	
	private int jobID, taskID;
	private VirtualMiddleware middleware;
	
	private HostAccess access;
	private VirtualTaskDescription taskDesc;
	private WorkerProcess proc;
	private Thread procThread;
	
	private Semaphore started;
	
	public VirtualTask(VirtualTaskDescription taskDesc) {
		this.jobID = -1;
		this.taskID = -1;
		middleware = null;

		this.taskDesc = taskDesc;
		
		started = new Semaphore(0);
	}
	
	public VirtualTask(int jobID, int taskID, VirtualTaskDescription taskDesc,
			VirtualMiddleware middleware) {
		this.jobID = jobID;
		this.taskID = taskID;
		this.middleware = middleware;
		
		this.taskDesc = taskDesc;
		
		started = new Semaphore(0);
	}

	public int getJobID() {
		return jobID;
	}

	public int getTaskID() {
		return taskID;
	}

	public void kill() {
		access.close();
		proc.killProcess();
	}

	public void start(HostAccess access) throws Exception {
		this.access = access;
		proc = taskDesc.newProcess(access);

		if(procThread != null)
			throw new Exception("Task already executed");

		procThread = new Thread() {
			public void run() {
				try {
					proc.executeProcess();
					VirtualTask.this.signalEndOfProcess();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		procThread.start();
		
		started.release();
	}

	public void join() {
		try {
			started.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			procThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		access.close();
	}

	public void signalEndOfProcess() throws Exception {
		access.signalEndOfTask(this);
	}

	public void signalInterrupted(String hostname) {
		if(middleware != null) {
			middleware.submitInterruptedTask(this, hostname);
		}
	}

	public void signalCompleted(String hostname) {
		if(middleware != null) {
			middleware.submitCompletedTask(this, hostname);
		}
	}

}
