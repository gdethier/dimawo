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
package be.ulg.montefiore.dimawo.exec;

import be.ulg.montefiore.dimawo.NoOpJobFactory;
import dimawo.exec.WorkerParameters;
import dimawo.exec.WorkerProcess;
import dimawo.middleware.distributedAgent.logging.ConsoleLogger;
import dimawo.simulation.host.ProcessHandle;
import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.VirtualNetwork;
import dimawo.simulation.socket.SocketFactory;
import junit.framework.TestCase;

public class WorkerProcessTest extends TestCase {

	public void testSingleWorker() {
		WorkerParameters params = new WorkerParameters();
		params.verbLevel = 0;
		params.factClassName = NoOpJobFactory.class.getName();
		params.factParams = null;
		params.port = 50200;
		params.workDirName = "/tmp/noopjob/";
		params.maxNumOfChildren = 2;
		params.reliabilityThreshold = 2;
		try {
			WorkerProcess wp = new WorkerProcess(params,
					new ConsoleLogger(), new SocketFactory());
			wp.executeProcess();
		} catch (Exception e) {
			assertTrue("An exception occured: "+e, false);
		}
	}

	public void testMultipleWorkers() throws Throwable {
		VirtualNetwork net = new VirtualNetwork();
		
//		VirtualHost.enableLog();
		
		// Populating network
		VirtualHost host1 = new VirtualHost("host1", net);
		VirtualHost host2 = new VirtualHost("host2", net);
		VirtualHost host3 = new VirtualHost("host3", net);
		
		// Starting hosts
		host1.start();
		host2.start();
		host3.start();
		
		// Creating processes
		ProcessHandle procHandle1;
		WorkerParameters params1 = new WorkerParameters();
		params1.verbLevel = 0;
		params1.factClassName = NoOpJobFactory.class.getName();
		params1.factParams = null;
		params1.port = 50200;
		params1.workDirName = "/tmp/noopjob/";
		params1.maxNumOfChildren = 2;
		params1.reliabilityThreshold = 2;

		procHandle1 = host1.createHostProcess(params1);
		
		ProcessHandle procHandle2;
		WorkerParameters params2 = new WorkerParameters();
		params2.verbLevel = params1.verbLevel;
		params2.factClassName = params1.factClassName;
		params2.factParams = params1.factParams;
		params2.port = params1.port;
		params2.workDirName = params1.workDirName;
		params2.maxNumOfChildren = params1.maxNumOfChildren;
		params2.reliabilityThreshold = params1.reliabilityThreshold;
		params2.bootstrapHostName = "host1";
		params2.bootstrapPort = params1.port;

		procHandle2 = host2.createHostProcess(params2);
		
		ProcessHandle procHandle3;
		WorkerParameters params3 = new WorkerParameters();
		params3.verbLevel = params1.verbLevel;
		params3.factClassName = params1.factClassName;
		params3.factParams = params1.factParams;
		params3.port = params1.port;
		params3.workDirName = params1.workDirName;
		params3.maxNumOfChildren = params1.maxNumOfChildren;
		params3.reliabilityThreshold = params1.reliabilityThreshold;
		params3.bootstrapHostName = "host1";
		params3.bootstrapPort = params1.port;

		procHandle3 = host3.createHostProcess(params3);
		
		// Wait for processes' creation
		System.out.println("Waiting for creation of processes");
		procHandle1.waitForCreation();
		if(procHandle1.getErrorOnCreation() != null) {
			throw procHandle1.getErrorOnCreation();
		}
		
		procHandle2.waitForCreation();
		if(procHandle2.getErrorOnCreation() != null) {
			throw procHandle2.getErrorOnCreation();
		}
		
		procHandle3.waitForCreation();
		if(procHandle3.getErrorOnCreation() != null) {
			throw procHandle3.getErrorOnCreation();
		}

		// Wait for exit
		System.out.println("Waiting for exit of processes");
		procHandle1.waitForEndOfExecution();
		if(procHandle1.getErrorOnExecution() != null) {
			throw procHandle1.getErrorOnExecution();
		}
		
		procHandle2.waitForEndOfExecution();
		if(procHandle2.getErrorOnExecution() != null) {
			throw procHandle2.getErrorOnExecution();
		}
		
		procHandle3.waitForEndOfExecution();
		if(procHandle3.getErrorOnExecution() != null) {
			throw procHandle3.getErrorOnExecution();
		}
		
		// Stop hosts
		host1.stop();
		host2.stop();
		host3.stop();
	}
}
