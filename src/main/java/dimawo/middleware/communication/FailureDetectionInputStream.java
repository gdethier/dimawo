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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import dimawo.middleware.communication.outputStream.OutOfSyncException;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.simulation.socket.SocketInterface;




public class FailureDetectionInputStream implements FailureDetectionCommons {

	private int timeout;
	private SocketInterface sock;
	
	private BufferedInputStream bis;
	private ObjectInputStream in;
	private DataOutputStream ack;

	public FailureDetectionInputStream(SocketInterface sock, int timeout) throws IOException {
		this.sock = sock;
		
		this.timeout = timeout;
		sock.setSoTimeout(timeout);

		bis = new BufferedInputStream(sock.getInputStream());
		ack = new DataOutputStream(sock.getOutputStream());
	}
	
	public Object readObject() throws IOException, ClassNotFoundException {
		in = new ObjectInputStream(bis);
		Object o = in.readObject();
		ack.write(OK_ACK);
		return o;
	}
	
	public Message readMessage(boolean sendAck, DAId localId) throws IOException, ClassNotFoundException, OutOfSyncException {
		in = new ObjectInputStream(bis);
		Message m = (Message) in.readObject();
		return m;
	}
	
	public void close() throws IOException {
		sock.close();
	}

	public void ackOK() throws IOException {
		ack.write(OK_ACK);
	}

	public void ack(DAId msgDest, DAId localId) throws IOException, OutOfSyncException {
		if(! msgDest.equals(localId)) {
			ack.write(OUT_OF_SYNC);
			throw new OutOfSyncException();
		}
		ack.write(OK_ACK);
	}
	
}
