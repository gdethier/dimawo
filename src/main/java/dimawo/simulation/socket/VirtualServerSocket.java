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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.host.events.ConnectEvent;
import dimawo.simulation.net.NetworkException;




public class VirtualServerSocket implements ServerSocketInterface {
	
	private VirtualHost host;
	
	private boolean isOpen;
	private int localPort;
	private int acceptTO;
	
	private LinkedBlockingQueue<ConnectEvent> pendingConnections;
	
	
	public VirtualServerSocket(VirtualHost host, int port) {
		this.host = host;

		isOpen = true;
		this.localPort = port;
		this.acceptTO = 0;
		
		pendingConnections = new LinkedBlockingQueue<ConnectEvent>();
	}

	@Override
	public SocketInterface accept() throws SocketTimeoutException, IOException {

		if( ! isOpen)
			throw new IOException("VirtualServerSocket is not open.");

		ConnectEvent e = null;
		if(acceptTO == 0) {

			try {
				e = pendingConnections.take();
			} catch (InterruptedException e1) {
				return null;
			}

		} else {

			try {
				e = pendingConnections.poll(acceptTO, TimeUnit.MILLISECONDS);
				if(e == null)
					throw new SocketTimeoutException();
			} catch (InterruptedException e1) {
			}

		}
		
		try {
			VirtualSocket sock = host.newSocket(e.getSourceAddress());
			e.setSocketPort(sock.getLocalPort());
			e.signalSuccess();
			return sock;
		} catch (Exception e1) {
			e.signalError(new NetworkException());
			throw new IOException("Unable to create new socket", e1);
		}
	}

	@Override
	public void close() throws IOException {
		
		if( ! isOpen)
			throw new IOException("Already closed.");

		try {
			host.unregisterServerSocket(localPort);
		} catch (NetworkException e) {
			throw new IOException("Could not close server socket", e);
		}

	}

	@Override
	public int getLocalPort() {
		return localPort;
	}

	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		acceptTO = timeout;
	}

	public void addPendingConnection(ConnectEvent ne) {
		if( ! isOpen) {
			ne.signalError(new NetworkException("Server socket closed"));
			return;
		}
		
		try {
			pendingConnections.put(ne);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getHostName() {
		return host.getHostName();
	}

}
