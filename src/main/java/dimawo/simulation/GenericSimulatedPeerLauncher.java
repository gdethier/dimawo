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

import dimawo.MasterWorkerFactory;
import dimawo.Reflection;
import dimawo.agents.AbstractAgent;
import dimawo.exec.NullDiscoveryService;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.logging.NetworkLogger;
import dimawo.middleware.fileSystem.FileSystemAgentParameters;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.impl.decentral.DecentralOverlay;
import dimawo.simulation.socket.SocketFactory;

public class GenericSimulatedPeerLauncher extends GenericSimulatedLauncher {
	private String factClassName = null;
	private int port = -1;
	private String workDirName = null;
	private String ctrlHostName = null;
	private int ctrlPort = -1;
	private int maxNumOfChildren = 1;
	private int reliabilityThreshold = 1;
	private int verbLevel = 0;
	private String logSvrHost = null;
	private int logSvrPort = -1;
	private String[] factParams = null;
	
	private DistributedAgent da;
	
	public void setMasterWorkerFactoryClassName(String className) {
		this.factClassName = className;
	}
	
	public void setDaPort(int port) {
		this.port = port;
	}
	
	public void setDaWorkingDirectory(String workDir) {
		this.workDirName = workDir;
	}
	
	public void setMnTreeMaxNumOfChildren(int num) {
		this.maxNumOfChildren = num;
	}
	
	public void setMnTreeReliabilityThreshold(int thresh) {
		this.reliabilityThreshold = thresh;
	}
	
	public void setAgentsVerbosityLevel(int level) {
		this.verbLevel = level;
	}
	
	public void setDaLogServerHostNameAndPort(String name, int port) {
		this.logSvrHost = name;
		this.logSvrPort = port;
	}
	
	public void setCtrlHostNameAndPort(String name, int port) {
		this.ctrlHostName = name;
		this.ctrlPort = port;
	}
	
	public void setMasterWorkerFactoryArguments(String[] args) {
		this.factParams = args;
	}
	
	public void main() throws Exception {
		AbstractAgent.setDefaultVerbosityLevel(verbLevel);

		MasterWorkerFactory tFact = (MasterWorkerFactory) Reflection.newInstance(factClassName);
		tFact.setParameters(factParams);
		DAId id = new DAId(access.getHostName(), port,
				System.currentTimeMillis());
		da = new DistributedAgent(id, workDirName, tFact,
				new NullDiscoveryService(),
				new NetworkLogger(new DAId(logSvrHost, logSvrPort, 0)),
				new FileSystemAgentParameters());
		DecentralOverlay over = new DecentralOverlay(da,
				maxNumOfChildren, reliabilityThreshold,
				new SocketFactory(access));
		
		System.out.println("Joining overlay");
		over.setJoinParameters(new JoinParameters(ctrlHostName, ctrlPort));
		try {
			over.joinOverlay();
		} catch(Exception e) {
			over.leaveOverlay();
			throw e;
		}
		
		System.out.println("Running DA");
		da.start();
		da.join();
		
		System.out.println("DA closed.");
	}

	@Override
	public void kill() {
		da.killDA();
	}
}
