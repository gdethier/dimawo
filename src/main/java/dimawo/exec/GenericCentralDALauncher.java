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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import dimawo.MasterWorkerFactory;
import dimawo.Reflection;
import dimawo.agents.AbstractAgent;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.logging.ConsoleLogger;
import dimawo.middleware.fileSystem.FileSystemAgentParameters;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.impl.central.CentralOverlay;
import dimawo.simulation.socket.SocketFactory;





public class GenericCentralDALauncher {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		int nGenArgs = 6;
		if(args.length < nGenArgs) {
			System.out.println("Usage: <fact. class> <port> <working dir.> <ctrl host> <ctrl port> <verb. level> ...");
			System.exit(-1);
		}
		
		String factClassName = args[0];
		int port = Integer.parseInt(args[1]);
		String workDirName = args[2];
		String ctrlHostName = args[3];
		int ctrlPort = Integer.parseInt(args[4]);
		int verbLevel = Integer.parseInt(args[5]);
		String[] factParams = new String[args.length - nGenArgs];
		System.arraycopy(args, nGenArgs, factParams, 0, args.length - nGenArgs);
		
		AbstractAgent.setDefaultVerbosityLevel(verbLevel);
		
		MasterWorkerFactory tFact = (MasterWorkerFactory) Reflection.newInstance(factClassName);
		tFact.setParameters(factParams);
		DAId id = new DAId(InetAddress.getLocalHost().getHostName(), port,
				System.currentTimeMillis());
		DistributedAgent da = new DistributedAgent(id, workDirName, tFact,
				new NullDiscoveryService(),
				new ConsoleLogger(), new FileSystemAgentParameters());
		CentralOverlay over = new CentralOverlay(da, new SocketFactory());
		
		System.out.println("Joining central overlay");
		over.setJoinParameters(new JoinParameters(ctrlHostName, ctrlPort));
		over.joinOverlay();
		
		System.out.println("Running DA");
		da.start();
		da.join();
		System.out.println("DA closed.");

	}

}
