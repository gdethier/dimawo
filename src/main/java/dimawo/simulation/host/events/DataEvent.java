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

public class DataEvent extends NetworkEvent {
	
	private int remotePort;
	private int byteVal;
	private byte[] data;

	public DataEvent(VirtualSocketAddress remoteAddr, int val) {
		super(remoteAddr.getHostName());
		this.remotePort = remoteAddr.getPort();

		byteVal = val & 0xFF;
	}
	
	public DataEvent(VirtualSocketAddress remoteAddr, byte[] data) {
		super(remoteAddr.getHostName());
		this.remotePort = remoteAddr.getPort();

		this.data = data;
	}

	public int getPort() {
		return remotePort;
	}
	
	public boolean isByte() {
		return data == null;
	}

	public byte[] getData() {
		return data;
	}
	
	public int getByte() {
		return byteVal;
	}

	public int getNumberOfBytes() {
		if(data != null)
			return data.length;
		else
			return 1;
	}

}
