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
package dimawo.simulation.host;

import java.util.concurrent.Semaphore;

import dimawo.exec.WorkerProcess;

/**
 * A ProcessHandle describes a {@link dimawo.exec.WorkerProcess} executed by
 * a virtual host. The process is instantiated by the host and may not be
 * immediately available.
 * <p> 
 * The state of a ProcessHandle instance is updated by
 * virtual host when following events occur: process is created, process ended
 * its execution. ProcessHandle class provides methods allowing to synchronize
 * with these events.
 * <p>
 * A ProcessHandle instance is obtained by submitting a request to a virtual
 * host (see
 * {@link dimawo.simulation.host.VirtualHost#createHostProcess(dimawo.exec.WorkerParameters)}).
 * A typical way of using a handle is first to wait the process is created
 * (see {@link #waitForCreation()}), check that no error occured, then wait
 * for the end of the execution of the process (see {@link #waitForEndOfExecution()}).
 * Again, one can check that no error occured during the execution of the
 * process. Of course, other operations may be interleaved with above calls.
 * 
 * @author Gerard Dethier
 */
public class ProcessHandle {
	/** The WorkerProcess instantiated upon process creation. */
	private WorkerProcess proc;
	/** The ID of the process. */
	private int procID;
	/** Errors that may occur upon creation and execution. */
	private Exception errorOnCreation, errorOnExecution;

	/** Semaphores used to synchronize with creation and end-of-execution
	 * events. */
	private Semaphore syncCreation, syncEndOfExecution;

	
	/**
	 * Instantiates a process handle. Initially, this handle is associated
	 * to no process.
	 */
	public ProcessHandle() {
		syncCreation = new Semaphore(0);
		syncEndOfExecution = new Semaphore(0);
	}

	/**
	 * Sets the process ID.
	 * @param id A process ID
	 */
	public void setProcId(int id) {
		this.procID = id;
	}

	/**
	 * Provides the WorkerProcess instantiated upon process creation
	 * 
	 * @return A WorkerProcess instance
	 */
	public WorkerProcess getWorkerProcess() {
		return proc;
	}

	/**
	 * Signals creation event. Threads blocked by a call to {@link #waitForCreation()}
	 * are released. 
	 * 
	 * @param error An error that occured during creation. If no error occured,
	 * null.
	 */
	public void signalProcessCreated(Exception error) {
		this.errorOnCreation = error;
		syncCreation.release();
	}

	/**
	 * Provides the error that occured during creation.
	 * 
	 * @return The error or null if no error occured.
	 */
	public Exception getErrorOnCreation() {
		return errorOnCreation;
	}

	/**
	 * Provides the ID of associated process. This value is undefined as long
	 * as creation event did not occur.
	 * 
	 * @return A process ID.
	 */
	public int getProcID() {
		return procID;
	}

	/**
	 * Signals end-of-execution event. Threads blocked by a call to
	 * {@link #waitForEndOfExecution()} are released. 
	 * 
	 * @param error An error that occured during execution. If no error occured,
	 * null.
	 */
	public void signalEndOfExecution(Exception error) {
		this.errorOnExecution = error;
		syncEndOfExecution.release();
	}
	
	/**
	 * Sets the WorkerProcess created with process.
	 * 
	 * @param proc A WorkerProcess instance.
	 */
	public void setWorkerProcess(WorkerProcess proc) {
		this.proc = proc;
	}

	/**
	 * Blocks the execution of current thread until creation event occurs.
	 */
	public void waitForCreation() {
		try {
			syncCreation.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		syncCreation.release();
	}

	/**
	 * Blocks the execution of current thread until end-of-execution event
	 * occurs.
	 */
	public void waitForEndOfExecution() {
		try {
			syncEndOfExecution.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		syncEndOfExecution.release();
	}

	/**
	 * Provides the error that occured during execution.
	 * 
	 * @return The error or null if no error occured.
	 */
	public Exception getErrorOnExecution() {
		return errorOnExecution;
	}

	/**
	 * Kills associated process. This method can only be called after creation
	 * event occured.
	 */
	public void killProcess() {
		proc.killProcess();
	}

}
