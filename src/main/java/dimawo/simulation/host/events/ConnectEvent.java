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
package dimawo.simulation.host.events;

import dimawo.simulation.socket.VirtualSocketAddress;


public class ConnectEvent extends NetworkEvent {
	
	private VirtualSocketAddress localAddr;
	private int remotePort; // port of the server socket
	private int sockPort; // Port of remotely created socket
	

	public ConnectEvent(VirtualSocketAddress endpoint, VirtualSocketAddress local) {
		super(endpoint.getHostName());
		this.remotePort = endpoint.getPort();
		this.localAddr = local;
	}

	public int getPort() {
		return remotePort;
	}

	public VirtualSocketAddress getSourceAddress() {
		return localAddr;
	}

	public void setSocketPort(int port) {
		this.sockPort = port;
	}
	
	public int getSocketPort() {
		return sockPort;
	}

}
