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
package dimawo.fileTransfer.test;

import java.io.IOException;

import dimawo.fileTransfer.client.FileTransferClientAgent;
import dimawo.fileTransfer.client.messages.FileTransferClientMessage;
import dimawo.fileTransfer.server.FileTransferServerAgent;
import dimawo.fileTransfer.server.messages.SimpleFTPServerMessage;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.ConnectCallBack;
import dimawo.middleware.communication.ConnectionRequestCallBack;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.simulation.socket.SocketFactory;




public abstract class TestCommunicator implements CommunicatorInterface {

	private FileTransferServerAgent server;
	private FileTransferClientAgent client;
	
	public static final DAId CLIENTDAID = new DAId("client", 42, 0), SERVERDAID = new DAId("server", 666, 0);
	
	private class ClientAccessor implements MOSAccessorInterface {

		@Override
		public void close() throws IOException, InterruptedException {
		}

		@Override
		public DAId getDestinationDAId() {
			return CLIENTDAID;
		}

		@Override
		public void writeBlockingMessage(Message m) throws IOException,
				InterruptedException {
			m.setSender(SERVERDAID);
			client.submitClientMessage((FileTransferClientMessage) m);
		}

		@Override
		public void writeNonBlockingMessage(Message m) {
			m.setSender(SERVERDAID);
			client.submitClientMessage((FileTransferClientMessage) m);
		}
		
	}
	
	private class ServerAccessor implements MOSAccessorInterface {

		@Override
		public void close() throws IOException, InterruptedException {
		}

		@Override
		public DAId getDestinationDAId() {
			return SERVERDAID;
		}

		@Override
		public void writeBlockingMessage(Message m) throws IOException,
				InterruptedException {
			m.setSender(CLIENTDAID);
			server.submitServerMessage((SimpleFTPServerMessage) m);
		}

		@Override
		public void writeNonBlockingMessage(Message m) {
			m.setSender(CLIENTDAID);
			server.submitServerMessage((SimpleFTPServerMessage) m);
		}
		
	}
	
	
	public TestCommunicator(FileTransferServerAgent server, FileTransferClientAgent client) {
		this.server = server;
		this.client = client;
	}

	@Override
	public void asyncConnect(DAId daId, ConnectionRequestCallBack cb,
			MOSCallBack errCB, Object attachment) throws InterruptedException {
		if(daId.equals(CLIENTDAID)) {
			cb.connectCallBack(new ConnectCallBack(daId, new ClientAccessor(), attachment));
		} else if(daId.equals(SERVERDAID)) {
			cb.connectCallBack(new ConnectCallBack(daId, new ServerAccessor(), attachment));
		} else {
			cb.connectCallBack(new ConnectCallBack(daId, attachment));
		}
	}

	@Override
	public SocketFactory getSocketFactory() {
		throw new Error("Unimplemented");
	}
	
	@Override
	public void submitIncomingMessage(Message tm) {
		throw new Error("Unimplemented");
	}

	@Override
	public MOSAccessorInterface syncConnect(DAId daId, MOSCallBack errCB)
			throws InterruptedException, IOException {
		if(daId.equals(CLIENTDAID)) {
			return new ClientAccessor();
		} else if(daId.equals(SERVERDAID)) {
			return new ServerAccessor();
		} else {
			throw new IOException("Unknown destination: "+daId);
		}
	}
	
	@Override
	public void signalBrokenOutputStream(MessageOutputStream mos) {
		throw new Error("Unimplemented");
	}

	@Override
	public void signalClosedOutputStream(MessageOutputStream mos) {
		throw new Error("Unimplemented");
	}
	
	@Override
	public void signalChildError(Throwable t, String errorSourceId) {
		throw new Error("unimplemented");
	}
	
	protected void routeMessage(Message m) throws IOException {
		DAId daId = m.getRecipient();
		if(daId.equals(CLIENTDAID)) {
			client.submitClientMessage((FileTransferClientMessage) m);
		} else if(daId.equals(SERVERDAID)) {
			server.submitServerMessage((SimpleFTPServerMessage) m);
		} else {
			throw new IOException("Unknown destination: "+daId);
		}
	}
	
	@Override
	public synchronized void printMessage(String msg) {
		System.out.println(msg);
	}
	
	@Override
	public synchronized void printMessage(Throwable t) {
		t.printStackTrace(System.out);
	}

}
