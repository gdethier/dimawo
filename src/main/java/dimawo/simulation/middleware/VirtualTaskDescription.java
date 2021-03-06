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

import dimawo.exec.WorkerParameters;
import dimawo.exec.WorkerProcess;
import dimawo.middleware.distributedAgent.logging.NetworkLogger;
import dimawo.simulation.host.HostAccess;
import dimawo.simulation.socket.SocketFactory;

public class VirtualTaskDescription {
	private String name;

	private WorkerParameters params;

	private String logSvrHost;
	private int logSvrPort;


	public VirtualTaskDescription(String name, WorkerParameters params,
			String logSvrHost, int logSvrPort) {
		this.name = name;

		this.params = params;

		this.logSvrHost = logSvrHost;
		this.logSvrPort = logSvrPort;
	}

	public String getName() {
		return name;
	}

	public WorkerProcess newProcess(HostAccess access) throws Exception {
		return new WorkerProcess(params,
				new NetworkLogger(logSvrHost, logSvrPort),
				new SocketFactory(access));
	}
}
