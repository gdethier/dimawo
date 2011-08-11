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

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import dimawo.agents.AgentException;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.communication.outputStream.MessageOutputStreamAccessor;
import dimawo.middleware.distributedAgent.DAId;





/**
 * Contains all information linked to a connection to a remote DA:
 * A reference to the MessageOutputStream and the set of components
 * that requested a connection to this remote DA.
 * 
 * @author Gérard Dethier
 *
 */
public class Connection {

	private DAId remoteDaId;
	private MessageOutputStream out;
	private TreeSet<MessageOutputStreamAccessor> connectedComponents;
	
	private long lastActivity;


	public Connection(DAId remoteDaId, MessageOutputStream out) {
		
		this.remoteDaId = remoteDaId;
		this.out = out;

		connectedComponents = new TreeSet<MessageOutputStreamAccessor>();

	}

	/**
	 * Adds a component to the set connected components.
	 * 
	 * @param cb The component.
	 * 
	 * @return True if the component was added to the set, false if it
	 * was already in it.
	 * @throws Exception 
	 */
	public void addConnectedComponent(MessageOutputStreamAccessor access) throws Exception {

		if( ! connectedComponents.add(access))
			throw new Exception("Accessor already associated to MOS");

	}


	/**
	 * Returns the MessageOutputStream associated to this connection.
	 * 
	 * @return A MessageOutputStream or null if the connection was closed.
	 */
//	public MessageOutputStream getMessageOutputStream() {
//
//		return out;
//
//	}

	/**
	 * Removes a component from the set of connected components.
	 * 
	 * @param cb The component.
	 * @throws InterruptedException 
	 */
	public void removeConnectedComponent(MessageOutputStreamAccessor access)
	throws InterruptedException {

		connectedComponents.remove(access);
		if(connectedComponents.size() == 0)
			lastActivity = System.currentTimeMillis();

	}

	/**
	 * @return The number of components using this connection.
	 */
	public int getConnectedComponentsCount() {

		return connectedComponents.size();

	}

	/**
	 * Closes the underlying MessageOutputStream.
	 * @param outHandler 
	 * @throws Exception 
	 * 
	 */
	public void close(OutputStreamsHandler outHandler) throws Exception {
		
		if(out == null) {
			
			throw new IOException("Stream already closed");
			
		}

		outHandler.closing(remoteDaId);
		try {
			out.stop();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		out = null;

	}

	public DAId getDaId() {
		
		return remoteDaId;
		
	}

	public void signalBroken() {
		
		Iterator<MessageOutputStreamAccessor> it = connectedComponents.iterator();
		while(it.hasNext()) {
			MessageOutputStreamAccessor access = it.next();
			access.signalBroken();
		}
		connectedComponents.clear();
		
	}

	public void writeMessage(Message m) throws IOException {
		if(connectedComponents.size() == 0)
			lastActivity = System.currentTimeMillis();
		out.writeMessage(m);
	}
	
	public MessageOutputStreamAccessor getAccessor(int uid, Communicator com, MOSCallBack cb) {
		MessageOutputStreamAccessor acc = new MessageOutputStreamAccessor(uid, com, cb, out);
		connectedComponents.add(acc);
		return acc;
	}

	public boolean isGcAble() {
		if(connectedComponents.size() > 0)
			return false;
		
		long inactivityTime = System.currentTimeMillis() - lastActivity;
		return inactivityTime > 5000; // inactivity of 5s -> GC
	}

}
