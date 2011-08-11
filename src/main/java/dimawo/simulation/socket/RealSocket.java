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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class RealSocket implements SocketInterface {
	
	private Socket sock;
	private RealSocketAddress localAddr, remoteAddr;
	
	public RealSocket() {
		sock = new Socket();
		localAddr = new RealSocketAddress((InetSocketAddress) sock.getLocalSocketAddress());
	}

	public RealSocket(Socket conSock) {
		this.sock = conSock;
		localAddr = new RealSocketAddress((InetSocketAddress) sock.getLocalSocketAddress());
		remoteAddr = new RealSocketAddress((InetSocketAddress) sock.getRemoteSocketAddress());
	}

	@Override
	public void close() throws IOException {
		sock.close();
	}

	@Override
	public void connect(SocketAddressInterface endpoint, int timeout) throws IOException {
		RealSocketAddress realAddr = (RealSocketAddress) endpoint;
		sock.connect(realAddr.getSocketAddress(), timeout);
		remoteAddr = new RealSocketAddress((InetSocketAddress) sock.getRemoteSocketAddress());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return sock.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return sock.getOutputStream();
	}

	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		sock.setSoTimeout(timeout);
	}

	@Override
	public boolean isConnected() {
		return sock.isConnected();
	}

	@Override
	public SocketAddressInterface getRemoteSocketAddress() {
		return localAddr;
	}

	@Override
	public SocketAddressInterface getLocalSocketAddress() {
		return remoteAddr;
	}

	@Override
	public SocketAddressInterface getSocketAddress(String hostName, int port) {
		return new RealSocketAddress(hostName, port);
	}

}
