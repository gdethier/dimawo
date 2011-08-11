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

import java.io.IOException;

import dimawo.DiMaWoException;
import dimawo.MasterWorkerFactory;
import dimawo.exec.NullDiscoveryService;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.logging.ConsoleLogger;
import dimawo.middleware.fileSystem.FileSystemAgentParameters;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.impl.central.CentralOverlay;
import dimawo.simulation.host.HostAccess;
import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.socket.SocketFactory;



public class CentralProcess extends SimulatedProcess {
	
	private DAId id;
	private String baseDir;
	private MasterWorkerFactory tFact;
	
	private CentralOverlay over;
	
	private JoinParameters joinParams;

	public CentralProcess(boolean init, String baseDir,
			MasterWorkerFactory tFact, HostAccess host, int port,
			JoinParameters joinParams) throws Exception {
		super(init);
		
		id = new DAId(host.getHostName(), port, System.currentTimeMillis());
		this.baseDir = baseDir;
		this.tFact = tFact;
		this.joinParams = joinParams;
		
		setNetworkStack(host);
	}

	@Override
	protected void configure() throws Exception {
		if(! isFirst()) {
			over.setJoinParameters(joinParams);
		}
	}

	@Override
	protected DistributedAgent initDA() throws Exception {
		return new DistributedAgent(id, baseDir, tFact,
				new NullDiscoveryService(),
				new ConsoleLogger(), new FileSystemAgentParameters());
	}

	@Override
	protected OverlayAgentInterface initOverlay(DistributedAgent da, HostAccess access)
	throws IOException {
		return new CentralOverlay(da, new SocketFactory(access));
	}

}
