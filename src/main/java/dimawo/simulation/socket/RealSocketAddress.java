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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RealSocketAddress implements SocketAddressInterface {
	private InetSocketAddress addr;
	
	public RealSocketAddress() {
	}

	public RealSocketAddress(InetSocketAddress addr) {
		this.addr = addr;
	}

	public RealSocketAddress(String hostName, int port) {
		this.addr = new InetSocketAddress(hostName, port);
	}

	public SocketAddress getSocketAddress() {
		return addr;
	}

	@Override
	public String getHostName() {
		return addr.getHostName();
	}

	@Override
	public int getPort() {
		return addr.getPort();
	}
}
