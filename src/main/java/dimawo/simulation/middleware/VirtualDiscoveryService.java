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

import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import dimawo.middleware.distributedAgent.DiscoveryServiceInterface;



public abstract class VirtualDiscoveryService implements DiscoveryServiceInterface {

	private VirtualMiddleware middleware;
	private int nextJobDescID;
	private TreeMap<Integer, VirtualJobDescription> submittedJobs;
	private Semaphore jobsCompleted;

	public VirtualDiscoveryService(VirtualMiddleware middleware) {
		this.middleware = middleware;
		nextJobDescID = 0;
		submittedJobs = new TreeMap<Integer, VirtualJobDescription>();
		jobsCompleted = new Semaphore(0);
	}

	@Override
	public synchronized void requestResources(int count) throws Exception {
		System.out.println("Requesting "+count+" resources.");
		VirtualJobDescription job = new VirtualJobDescription(nextJobDescID);
		fillJob(count, job);
		middleware.submitJob(job);
		submittedJobs.put(nextJobDescID, job);
		++nextJobDescID;
	}
	
	@Override
	public void waitSubmittedJobs() throws InterruptedException {
		synchronized(this) {
			if(submittedJobs.isEmpty())
				return;
		}
		jobsCompleted.acquire();
		jobsCompleted.release();
	}
	
	public synchronized void signalCompletedJob(int jobDescID) {
		System.out.println("Job "+jobDescID+" completed.");
		submittedJobs.remove(jobDescID);
		if(submittedJobs.isEmpty()) {
			jobsCompleted.release();
		}
	}

	protected abstract void fillJob(int count, VirtualJobDescription job);

}
