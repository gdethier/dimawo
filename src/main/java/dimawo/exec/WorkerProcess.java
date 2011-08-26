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
package dimawo.exec;

import dimawo.MasterWorkerFactory;
import dimawo.Reflection;
import dimawo.agents.AbstractAgent;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.logging.LoggerInterface;
import dimawo.middleware.fileSystem.FileSystemAgentParameters;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.impl.decentral.DecentralOverlay;
import dimawo.simulation.socket.SocketFactory;

/**
 * A WorkerProcess object implements the behavior of a DiMaWo worker. Generally,
 * there is one instance per computer in a cluster. However, this restriction
 * may be ignored for test and/or simulation purposes.
 * <p>
 * The simplest way of using
 * this class is to get parameters from a user (for example, through command
 * line arguments), instantiate this class
 * with them and finally execute the WorkerProcess (with
 * {@link #executeProcess()}). This is implemented by 2 launchers:
 * {@link dimawo.exec.GenericBootstrapLauncher}
 * and {@link dimawo.exec.GenericLauncher} instantiating and executing
 * respectively a bootstrap worker and a simple worker.
 * <p>
 * This class enables the execution of a worker as part of the workflow of a
 * more complex application. For example, an application may generate input
 * data, execute worker processes on several computers and post-process output
 * data after all workers finished their execution.
 * 
 * @author Gerard Dethier
 */
public class WorkerProcess {
	/** The Distributed Agent executed by this process. */
	private DistributedAgent da;
	/** The Overlay Agent connecting this process to the workers network. */
	private DecentralOverlay over;
	
	/** Parameters of the worker to execute. */
	private WorkerParameters params;
	/** Logger interface that will be used by DA instantiated by this
	 * process. */
	private LoggerInterface logger;
	/** Socket factory used by this worker. */
	private SocketFactory sFact;
	
	/** A flag indicating if the process has already been killed. */
	private boolean isKilled;
	/** A flag indicating if the process has already been executed. */
	private boolean isExecuted;
	/** A flag indicating if the process has already finished its execution. */
	private boolean isFinished;

	/**
	 * Instantiates a Bootstrap worker process. The bootstrap worker is the
	 * first worker to be executed. Next workers connect to it or another
	 * worker in order to join the worker processes' network.
	 * 
	 * @param params The parameters of the worker.
	 * @param logger A logger.
	 * @param sFact A socket factory.
	 */
	public WorkerProcess(WorkerParameters params,
			LoggerInterface logger,
			SocketFactory sFact) {
		this.params = params;
		this.logger = logger;
		this.sFact = sFact;
	}
	

	/**
	 * Executes the worker process. This method blocks until the worker
	 * finished its execution.
	 * 
	 * @throws Exception If an error occured.
	 */
	public void executeProcess() throws Exception {
		synchronized(this) {
			if(isFinished || isKilled)
				throw new Exception("This process has already been executed.");
			if(isExecuted)
				throw new Exception("This process is executing.");
			isExecuted = true;
		}

		AbstractAgent.setDefaultVerbosityLevel(params.verbLevel);

		// Instantiating core agents.
		MasterWorkerFactory tFact = (MasterWorkerFactory) 
				Reflection.newInstance(params.factClassName);
		tFact.setParameters(params.factParams);
		DAId id = new DAId(sFact.getHostName(), params.port,
				System.currentTimeMillis());
		da = new DistributedAgent(id, params.workDirName, tFact,
				new NullDiscoveryService(),
				logger,
				new FileSystemAgentParameters());
		over = new DecentralOverlay(da, params.maxNumOfChildren,
				params.reliabilityThreshold,
				sFact);

		// Connecting worker to overlay
		if(params.bootstrapHostName == null) {
			System.out.println("Bootstrapping overlay");
			try {
				over.initOverlay();
			} catch(Exception e) {
				over.leaveOverlay();
				throw e;
			}
		} else {
			System.out.println("Joining overlay");
			over.setJoinParameters(new JoinParameters(params.bootstrapHostName,
					params.bootstrapPort));
			try {
				over.joinOverlay();
			} catch(Exception e) {
				over.leaveOverlay();
				throw e;
			}
		}

		// Executing worker
		System.out.println("Overlay joined, executing...");

		da.start();
		da.join();
		
		synchronized(this) {
			isFinished = true;
		}

		System.out.println("Worker process finished its execution.");
	}
	
	/**
	 * Interrupts the execution of this process.
	 */
	public synchronized void killProcess() {
		if(isKilled || isFinished)
			return;
		isKilled = true;

		da.killDA();
	}
}

