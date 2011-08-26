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

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.logging.NetworkLogger;
import dimawo.simulation.GenericSimulatedLauncher;
import dimawo.simulation.socket.SocketFactory;

public class GenericSimulatedBootstrapPeerLauncher extends GenericSimulatedLauncher {
	private WorkerParameters params = new WorkerParameters();
	private String logSvrHost = null;
	private int logSvrPort = -1;
	
	private WorkerProcess proc;


	public void setMasterWorkerFactoryClassName(String className) {
		this.params.factClassName = className;
	}
	
	public void setDaPort(int port) {
		this.params.port = port;
	}
	
	public void setDaWorkingDirectory(String workDir) {
		this.params.workDirName = workDir;
	}
	
	public void setMnTreeMaxNumOfChildren(int num) {
		this.params.maxNumOfChildren = num;
	}
	
	public void setMnTreeReliabilityThreshold(int thresh) {
		this.params.reliabilityThreshold = thresh;
	}
	
	public void setAgentsVerbosityLevel(int level) {
		this.params.verbLevel = level;
	}
	
	public void setDaLogServerHostNameAndPort(String name, int port) {
		this.logSvrHost = name;
		this.logSvrPort = port;
	}
	
	public void setMasterWorkerFactoryArguments(String[] args) {
		this.params.factParams = args;
	}

	public void main() throws Exception {
		proc = new WorkerProcess(params,
				new NetworkLogger(new DAId(logSvrHost, logSvrPort, 0)),
				new SocketFactory(access));
		
		proc.executeProcess();
	}

	@Override
	public void kill() {
		proc.killProcess();
	}
}
