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
package dimawo.middleware.communication.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.FailureDetectionInputStream;
import dimawo.middleware.communication.IdentificationMessage;
import dimawo.middleware.communication.inputStream.MessageInputStream;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayJoin;
import dimawo.middleware.overlay.OverlayMessage;
import dimawo.simulation.socket.SocketInterface;



/**
 * This agent checks if incoming connections are "safe". It also ensures
 * that the Communicator will not be locked by a remote hanging system
 * when waiting an ack of reading an object. Finally, it limits the amount of
 * open and waiting connections.
 * 
 * @author Gérard Dethier
 *
 */
public class CommunicatorConnectionHandler
extends LoggingAgent
implements ConnectionHandler {

	private int inactivityTO;
	
	private Communicator com;
	private OverlayAgentInterface overInt;
	
	public CommunicatorConnectionHandler(Communicator com,
			OverlayAgentInterface overInt,
			int maxConnections, int inactivityTO) {

		super(com, "CommunicatorConnectionHandler", maxConnections);
		
		this.inactivityTO = inactivityTO;
		
		this.com = com;
		this.overInt = overInt;
		
		setPrintStream(com.getHostingDa().getFilePrefix());

	}
	
	
	////////////////////
	// Public methods //
	////////////////////
	
	public void newConnection(SocketInterface sock) throws InterruptedException {

		submitMessage(sock);

	}

	
	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
		com.submitConnectionHandlerClosed();
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {

		if(o instanceof SocketInterface) {
			
			handleNewConnection((SocketInterface) o);
			
		} else {

			throw new UnknownAgentMessage(o);

		}

	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
	}
	
	
	/////////////////////
	// Private methods //
	/////////////////////
	
	private void handleNewConnection(final SocketInterface sock) throws InterruptedException {

		// ServerLoop submits null when closed.
		if(sock == null)
			try {
				stop();
			} catch (AgentException e2) {
				e2.printStackTrace();
			}

			Thread t = new Thread() {

				public void run() {

					try {

						FailureDetectionInputStream chan = new FailureDetectionInputStream(sock, inactivityTO);
						Object o = chan.readObject();

						if(o instanceof OverlayMessage) {

							overInt.submitOverlayMessage((OverlayMessage) o);
							chan.close();

						} else if(o instanceof IdentificationMessage) {

							printMessage("MOS connecting...");
							com.submitIdentificationMessage((IdentificationMessage) o, chan);

						} else {

							printMessage("Protocol incoherence, closing connection.");
							sock.close();

						}

					} catch (Exception e) {

						printMessage("Connection from "+
								sock.getRemoteSocketAddress()+" broken.");
						agentPrintMessage(e);
						try {

							sock.close();
						} catch (IOException e1) {
						}
					}
				}
			};
			t.setDaemon(true);
			t.start();
		
	}


	@Override
	public synchronized void printMessage(String msg) {
		agentPrintMessage(msg);
	}


	@Override
	public void serverLoopClosed() {
		try {
			stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}

}
