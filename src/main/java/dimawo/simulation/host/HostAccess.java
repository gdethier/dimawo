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
package dimawo.simulation.host;

import java.io.IOException;

import dimawo.simulation.middleware.VirtualTask;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketInterface;



public class HostAccess {
	
	private int accessID;
	private boolean accessUp;
	private VirtualHost host;
	
	public HostAccess(int accessID, VirtualHost host) {
		this.accessID = accessID;
		this.host = host;
		accessUp = true;
	}

	public synchronized void close() {
		accessUp = false;
		host.signalClosedAccess(accessID);
	}

	public synchronized void signalEndOfTask(VirtualTask task) throws Exception {
		if(accessUp) {
			host.signalTaskCompleted(task);
		} else {
			throw new Exception("Host is down");
		}
	}

	public synchronized SocketInterface newSocket() throws IOException {
		if(accessUp) {
			return host.newSocket();
		} else {
			throw new IOException("Access closed");
		}
	}

	public synchronized ServerSocketInterface newServerSocket(int port) throws IOException {
		if(accessUp) {
			return host.newServerSocket(port);
		} else {
			throw new IOException("Access closed");
		}
	}

	public String getHostName() {
		return host.getHostName();
	}

}
