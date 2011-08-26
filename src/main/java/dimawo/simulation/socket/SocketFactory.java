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
package dimawo.simulation.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import dimawo.simulation.host.HostAccess;


public class SocketFactory {

	private HostAccess host;
	
	
	public SocketFactory() {
		host = null;
	}

	public SocketFactory(HostAccess host) {
		this.host = host;
	}
	
	public String getHostName() throws UnknownHostException {
		if(host != null)
			return host.getHostName();
		else
			return InetAddress.getLocalHost().getHostName();
	}
	
	public SocketInterface newSocket() throws IOException {
		if(host != null)
			return host.newSocket();
		else
			return new RealSocket();
	}

	public ServerSocketInterface newServerSocket(int port) throws IOException {
		if(host != null)
			return host.newServerSocket(port);
		else
			return new RealServerSocket(port);
	}

	public void close() {
		if(host != null)
			host.close();
	}
	
}
