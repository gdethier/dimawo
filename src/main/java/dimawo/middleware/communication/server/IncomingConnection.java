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
package dimawo.middleware.communication.server;

import java.net.InetSocketAddress;

import dimawo.simulation.socket.SocketInterface;




public class IncomingConnection {

	protected SocketInterface sock;
	protected String remoteHostName;
	protected Object msg;
	
	public IncomingConnection(Object m, SocketInterface s) {
		sock = s;
		msg = m;
	}
	
	public IncomingConnection(Object m, String remoteHostName) {

		this.remoteHostName = remoteHostName;
		msg = m;

	}
	
	public SocketInterface getSocket() {
		return sock;
	}
	
	public Object getMessage() {
		return msg;
	}

	public String getRemoteHostName() {
		
		if(sock != null) {
			
			InetSocketAddress addr =
				(InetSocketAddress) sock.getRemoteSocketAddress();
			return addr.getHostName();
			
		} else {
			
			return remoteHostName;
			
		}
		
	}

}
