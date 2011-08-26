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

import dimawo.middleware.distributedAgent.logging.ConsoleLogger;
import dimawo.simulation.socket.SocketFactory;


public class GenericLauncher {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int nGenArgs = 8;
		if(args.length < nGenArgs) {
			System.out.println("Usage: <fact. class> <port> <working dir.> " +
					"<join host> <join port> " +
					"<mnMaxNumOfChild> <mnReliabilityThresh> <verb. level> " +
					" ...");
			System.exit(-1);
		}
		
		WorkerParameters params = new WorkerParameters();
		params.factClassName = args[0];
		params.port = Integer.parseInt(args[1]);
		params.workDirName = args[2];
		params.bootstrapHostName = args[3];
		params.bootstrapPort = Integer.parseInt(args[4]);
		params.maxNumOfChildren = Integer.parseInt(args[5]);
		params.reliabilityThreshold = Integer.parseInt(args[6]);
		params.verbLevel = Integer.parseInt(args[7]);
		params.factParams = new String[args.length - nGenArgs];
		System.arraycopy(args, nGenArgs, params.factParams, 0, args.length - nGenArgs);
		
		WorkerProcess proc = new WorkerProcess(params, new ConsoleLogger(), new SocketFactory());
		
		proc.executeProcess();
	}

}
