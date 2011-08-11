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
package dimawo.middleware.communication;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import dimawo.middleware.communication.outputStream.OutOfSyncException;
import dimawo.simulation.socket.SocketInterface;




public class FailureDetectionOutputStream implements FailureDetectionCommons {
	
	private SocketInterface sock;
	
	private BufferedOutputStream bos;
	private ObjectOutputStream out;
	private DataInputStream ack;
	
	public FailureDetectionOutputStream(SocketInterface sock, int timeout) throws IOException {
		this.sock = sock;
		
		sock.setSoTimeout(timeout);
		
		bos = new BufferedOutputStream(sock.getOutputStream());
		ack = new DataInputStream(sock.getInputStream());
	}
	
	public void writeObject(Object o) throws IOException {
		out = new ObjectOutputStream(bos);
		out.writeObject(o);
		out.flush();
		if(ack.read() != OK_ACK)
			throw new IOException("Could not write object");
	}
	
	public void writeMessage(Message m, boolean waitAck) throws IOException, OutOfSyncException {
		out = new ObjectOutputStream(bos);
		out.writeObject(m);
		out.flush();
		
		if(waitAck) {
			int ackVal = ack.read();
			if(ackVal == OUT_OF_SYNC)
				throw new OutOfSyncException("Could not write object");
			if(ackVal != OK_ACK)
				throw new IOException("Could not write object");
		}
	}
	
	public void close() throws IOException {
		sock.close();
	}

}
