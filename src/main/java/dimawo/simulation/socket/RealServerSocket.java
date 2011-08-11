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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class RealServerSocket implements ServerSocketInterface {
	
	private ServerSocket sock;


	public RealServerSocket(int port) throws IOException {

		sock = new ServerSocket(port);

	}


	@Override
	public SocketInterface accept() throws SocketTimeoutException, IOException {

		Socket conSock = sock.accept();

		return new RealSocket(conSock);

	}


	@Override
	public void close() throws IOException {
		sock.close();
	}


	@Override
	public int getLocalPort() {
		return sock.getLocalPort();
	}


	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		sock.setSoTimeout(timeout);
	}


	@Override
	public String getHostName() {
		return sock.getInetAddress().getHostName();
	}

}
