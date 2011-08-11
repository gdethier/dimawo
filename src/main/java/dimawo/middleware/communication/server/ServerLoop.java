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

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import dimawo.agents.ErrorHandler;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;





public class ServerLoop implements Runnable {

	/** Associated Communicator */
	private ErrorHandler errorHandler;

	/** Server thread */
	private Thread serverThread;
	private ServerSocketInterface serv;
	/** Indicates if the Server was stopped. */
	private boolean stopping;
	
	private ConnectionHandler connHandler;
	
	public ServerLoop(int port, ErrorHandler com, ConnectionHandler cH,
			SocketFactory sockFact) throws IOException {

		this.errorHandler = com;

		serv = sockFact.newServerSocket(port);
		stopping = false;

		connHandler = cH;

	}

	
	////////////////////
	// Public methods //
	////////////////////
	
	public int getPort() {

		return serv.getLocalPort();

	}
	
	
	/**
	 * Starts the server.
	 */
	public void start() {
		
		if(serverThread == null) {

			serverThread = new Thread(this, "ServerLoop");
			serverThread.start();

		}
		
	}

	/**
	 * Requests the server to stop.
	 * @throws InterruptedException 
	 */
	public void stop() {

		stopping = true;

	}
	
	public void join() throws InterruptedException {

		serverThread.join();

	}

	
	/////////////////////////////
	// Runnable implementation //
	/////////////////////////////
	
	public void run() {
		
		printMessage("Launching server loop.");

		try {

			serv.setSoTimeout(1000); /* To check if the server was stopped
			while accepting connections. */
			

		} catch (SocketException e1) {

			e1.printStackTrace();
			errorHandler.signalChildError(e1, "ServerLoop");

			exitActions();
			return;

		}

		// Accept new incoming connections
		while(true) {
			
			// Check if the server must stop.
			if(stopping) {

				exitActions();
				return;

			}
			
			SocketInterface inConnection = null;
			try {

				inConnection = serv.accept();

			} catch(SocketTimeoutException e) {

				// A Time-out occured.
				if(stopping) {

					exitActions();
					return;

				} else {
					
					continue;
					
				}

			} catch (IOException e) {

				errorHandler.signalChildError(e, "ServerLoop");
				exitActions();
				return;

			}

			try {

				connHandler.newConnection(inConnection);

			} catch (InterruptedException e) {

				exitActions();
				return;

			}

		}

	}


	/**
	 * Does the final clean-up before the server stops.
	 */
	private void exitActions() {

		// Signal connection handler server loop is closed.
		connHandler.serverLoopClosed();

		printMessage("Server loop closing.");
		try {
			serv.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	private void printMessage(String txt) {
		
		connHandler.printMessage("[ServerLoop] "+txt);
		
	}


	public boolean isStopped() {
		return stopping;
	}

}
