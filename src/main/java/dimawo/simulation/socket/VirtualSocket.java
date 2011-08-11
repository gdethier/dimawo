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
import java.net.SocketException;
import java.net.SocketTimeoutException;

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.host.events.DataEvent;
import dimawo.simulation.net.NetworkException;




public class VirtualSocket implements SocketInterface {
	
	private VirtualHost host;

	private int localPort;
	private VirtualSocketAddress remoteAddr;

	private boolean isConnected;
	private VirtualSocketInputStream in;
	private VirtualSocketOutputStream out;
	private BlockingByteBuffer bbb;


	public VirtualSocket(VirtualHost host, int port,
			VirtualSocketAddress remoteAddr) {
		this.host = host;
		this.localPort = port;
		this.remoteAddr = remoteAddr;
		
		in = new VirtualSocketInputStream(this);
		out = new VirtualSocketOutputStream(this);
		bbb = new BlockingByteBuffer(this);
		
		isConnected = true;
	}

	public VirtualSocket(VirtualHost host, int port) {
		this.host = host;
		this.localPort = port;
		
		in = new VirtualSocketInputStream(this);
		out = new VirtualSocketOutputStream(this);
		bbb = new BlockingByteBuffer(this);
		
		isConnected = false;
	}

	@Override
	public void close() throws IOException {
		
		synchronized(this) {
		
			if( ! isConnected)
				throw new IOException("Virtual socket already closed");
			printMessage("Closing.");
			isConnected = false;
		
		}

		in.close();
		out.close();

		try {
			host.signalSocketClose(remoteAddr);
		} catch (NetworkException e) {
			printMessage("Closed.");
			throw new IOException(e);
		}
		try {
			host.unregisterSocket(localPort);
		} catch (NetworkException e) {
			printMessage("Closed.");
			throw new IOException("Could not close socket", e);
		}
		
		printMessage("Closed.");
	}

	@Override
	public void connect(SocketAddressInterface endpoint, int timeout) throws IOException {
		VirtualSocketAddress remote = (VirtualSocketAddress) endpoint;
		try {
			printMessage("Connecting to socket "+remoteAddr+"...");
			int sockPort =
				host.connectTo(remote, timeout, localPort);
			
			remoteAddr = new VirtualSocketAddress(remote.getHostName(), sockPort);
			isConnected = true;
			
			printMessage("Connected to socket "+remoteAddr);
		} catch (NetworkException e) {
			throw new IOException("Could not connect", e);
		} catch (InterruptedException e) {
			throw new IOException("Could not connect", e);
		}
		
	}

	@Override
	public InputStream getInputStream() throws IOException {
		
		synchronized(this) {
			
			if( ! isConnected)
				throw new IOException("Virtual socket is closed");
		
		}
		
		return in;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		
		synchronized(this) {
			
			if( ! isConnected)
				throw new IOException("Virtual socket is closed");
		
		}
		
		return out;
	}

	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		in.setTimeout(timeout);
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public VirtualSocketAddress getRemoteSocketAddress() {
		return remoteAddr;
	}

	protected void sendData(int val) throws IOException {
		if( ! isConnected) {
			throw new IOException("Socket is closed");
		}
		
		try {
			host.sendData(remoteAddr, val);
		} catch (NetworkException e) {
			throw new IOException("Could not send data", e);
		}
	}
	
	protected void sendData(byte[] data) throws IOException {
		if( ! isConnected) {
			throw new IOException("Socket is closed");
		}
		
		try {
			host.sendData(remoteAddr, data);
		} catch (NetworkException e) {
			throw new IOException("Could not send data", e);
		}
	}

	public void putData(DataEvent ne) {
		if( ! isConnected) {
			ne.signalError(new NetworkException("Broken pipe"));
			return;
		}
		
		bbb.putBytes(ne); // Event in order to implement a form of flow control
		ne.signalSuccess();
	}

	public int getLocalPort() {
		return localPort;
	}

	public void putRemoteClose() {
		bbb.putClose();
	}

	@Override
	public SocketAddressInterface getLocalSocketAddress() {
		return new VirtualSocketAddress(host.getHostName(), localPort);
	}
	
	public void printMessage(String msg) {
		host.printMessage("[VirtualSocket "+host.getHostName()+":"+localPort+"] "+ msg);
	}

	public void printMessage(Throwable t) {
		host.printMessage(t);
	}

	public int readByteFromBuffer(int timeout) throws SocketTimeoutException, IOException {
		return bbb.getByte(timeout);
	}

	public int availableBytesInBuffer() {
		return bbb.availableBytes();
	}

	public int fillWithBytesFromBuf(byte[] b, int off, int len, int timeout) throws IOException {
		return bbb.fill(b, off, len, timeout);
	}

	@Override
	public SocketAddressInterface getSocketAddress(String hostName, int port) {
		return new VirtualSocketAddress(hostName, port);
	}

}
