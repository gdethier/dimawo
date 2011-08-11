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

import java.util.List;

import dimawo.simulation.cluster.ClusterDescription;
import dimawo.simulation.cluster.VirtualCluster;
import dimawo.simulation.middleware.MiddlewareDescription;
import dimawo.simulation.middleware.VirtualJobDescription;
import dimawo.simulation.middleware.VirtualMiddleware;
import dimawo.simulation.middleware.VirtualTask;
import dimawo.simulation.net.NetworkException;



public class Simulation {
	private VirtualCluster cluster;
	private VirtualMiddleware middleware;

	public Simulation(ClusterDescription clusterDesc,
			MiddlewareDescription middleDesc) throws ClusterException, NetworkException {
		cluster = new VirtualCluster();
		middleware = new VirtualMiddleware(cluster);

		cluster.configure(clusterDesc);
		middleware.configure(middleDesc);
	}

	public void runSimulation(DeploymentDescription deployDesc) throws Exception {
		middleware.start();
		
		List<ExecCommand> procs = deployDesc.getProcesses();
		VirtualTask[] runningProcs = new VirtualTask[procs.size()];
		int i = 0;
		for(ExecCommand cmd : procs) {
			String hostName = cmd.getHostName();
			System.out.println("Running a process on host "+hostName);
			runningProcs[i] = new VirtualTask(cmd.getProcessDescription());
			cluster.executeProcessOnHost(hostName, runningProcs[i]);
			++i;
		}

		List<VirtualJobDescription> jobs = deployDesc.getJobs();
		int[] awaitedJobs = new int[jobs.size()];
		i = 0;
		for(VirtualJobDescription job : jobs) {
			System.out.println("Submitting job "+job.getJobDescId()+" to middleware.");
			awaitedJobs[i] = middleware.submitJob(job);
			System.out.println("Received job ID "+awaitedJobs[i]);
			++i;
		}

		for(i = 0; i < awaitedJobs.length; ++i) {
			System.out.println("Waiting for job with ID "+awaitedJobs[i]);
			middleware.waitForJob(awaitedJobs[i]);
		}
		
		for(i = 0; i < runningProcs.length; ++i) {
			System.out.println("Waiting for proc "+(i + 1)+"/"+runningProcs.length);
			runningProcs[i].join();
		}
		
		middleware.stop();
	}
}
