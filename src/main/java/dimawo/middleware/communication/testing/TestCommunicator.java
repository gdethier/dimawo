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
package dimawo.middleware.communication.testing;

import java.io.IOException;

import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.ConnectionRequestCallBack;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.faultdetection.FaultDetectionAgent;
import dimawo.simulation.socket.SocketFactory;



public class TestCommunicator implements CommunicatorInterface {
	
	private DAId localAddr;
	private SocketFactory sockFact;


	public TestCommunicator(DAId localAddr, SocketFactory sockFact) {
		
		this.localAddr = localAddr;
		this.sockFact = sockFact;

	}
	

	@Override
	public DAId getHostingDaId() {
		return localAddr;
	}

	@Override
	public SocketFactory getSocketFactory() {
		return sockFact;
	}

	@Override
	public void signalBrokenOutputStream(MessageOutputStream mos) {
		printMessage("Output stream to "+mos.getRemoteDaId()+" broken.");
	}

	@Override
	public void submitIncomingMessage(Message tm) {
		printMessage("Received "+tm.getClass().getSimpleName()+" from "+tm.getSender());
	}

	@Override
	public void signalChildError(Throwable t, String errorSourceId) {
		printMessage("Child threw "+t.toString());
		t.printStackTrace();
	}

	public void printMessage(String msg) {
		
		System.out.println("[TestCommunicator on "+localAddr+"] "+msg);
		
	}


	@Override
	public void signalClosedOutputStream(MessageOutputStream mos) {
		printMessage("Signal closed: "+mos.getRemoteDaId());
	}


	@Override
	public void asyncConnect(DAId daId, ConnectionRequestCallBack cb,
			MOSCallBack errCB, Object attachment) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sendDatagramMessage(Message m) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public MOSAccessorInterface syncConnect(DAId id, MOSCallBack errCB)
			throws InterruptedException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void multicastMessage(DAId[] ids, Message msg) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public synchronized void printMessage(Throwable t) {
	}


	@Override
	public void registerMessageHandler(Object messageHandlerId,
			MessageHandler mh) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void unregisterMessageHandler(Object messageHandlerId,
			MessageHandler mh) {
		// TODO Auto-generated method stub
		
	}
}
