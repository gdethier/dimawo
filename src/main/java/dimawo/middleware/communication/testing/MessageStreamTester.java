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
import java.net.SocketTimeoutException;

import dimawo.agents.AgentException;
import dimawo.middleware.communication.FailureDetectionInputStream;
import dimawo.middleware.communication.IdentificationMessage;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.inputStream.MessageInputStream;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.net.VirtualNetwork;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;





public class MessageStreamTester {
	
	private static boolean virt = true;
	private static Message[] sendMsgs;

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		
		final DAId serverAddr;
		final DAId clientAddr;
		final VirtualNetwork net;
		
		if(virt) {
		
			// Init virtual network
			net = new VirtualNetwork();
			
			serverAddr = new DAId("serverHost", 0, 0);
			clientAddr = new DAId("clientHost", 0, 0);
		
		} else {
			
			net = null;

			serverAddr = new DAId("localhost", 50201, 0);
			clientAddr = new DAId("localhost", 50202, 0);

		}


		Thread serverThread = new Thread() {

			public void run() {
				
				SocketFactory servFact;
				if(virt) {
				
					VirtualHost server;
					try {
						server = new VirtualHost(serverAddr.getHostName(), net);
						server.start();
					} catch (NetworkException e1) {
						e1.printStackTrace();
						return;
					} catch (AgentException e) {
						e.printStackTrace();
						return;
					}
					
					try {
						servFact = new SocketFactory(server.getAccess());
					} catch (NetworkException e1) {
						e1.printStackTrace();
						return;
					}
				
				} else {
					servFact = new SocketFactory();
				}
				
				try {

					ServerSocketInterface serv =
						servFact.newServerSocket(serverAddr.getPort());
					printMessage("ServerSocket bound.");

					SocketInterface sock = serv.accept();
					serv.close();
					printMessage("Remote host: "+sock.getRemoteSocketAddress());
					
					TestCommunicator servCom =
						new TestCommunicator(serverAddr, servFact);
					
					// Negotiation...
					printMessage("Creating initial stream...");
					FailureDetectionInputStream in = new FailureDetectionInputStream(sock, 10000);
					
					try {

						printMessage("Receiving Identification message.");
						IdentificationMessage idm = (IdentificationMessage) in.readObject();
						printMessage("Identification message received.");

						// Instantiate MIS
						MessageInputStream mis =
							new MessageInputStream(servCom, idm.getRemoteDaId(), in);
						mis.run();

					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

				} catch (SocketTimeoutException e) {
					printMessage(e.toString());
				} catch (IOException e) {
					printMessage(e.toString());
				}

			}
			
			private void printMessage(String str) {
				System.out.println("[Server] "+str);
			}
			
		};

		Thread clientThread = new Thread() {
			
			public void run() {
				
				SocketFactory clientFact;
				if(virt) {
				
					VirtualHost client;
					try {
						client = new VirtualHost(clientAddr.getHostName(), net);
						client.start();
					} catch (NetworkException e1) {
						e1.printStackTrace();
						return;
					} catch (AgentException e) {
						e.printStackTrace();
						return;
					}
					
					try {
						clientFact = new SocketFactory(client.getAccess());
					} catch (NetworkException e1) {
						e1.printStackTrace();
						return;
					}
				
				} else {
					clientFact = new SocketFactory();
				}

				try {

					TestCommunicator clientCom =
						new TestCommunicator(clientAddr, clientFact);
					
					MessageOutputStream mos = new MessageOutputStream(clientCom, serverAddr);
					try {
						mos.start();
					} catch (AgentException e) {
						e.printStackTrace();
						return;
					}
					
					int maxNumOfMsgs = 100;
					sendMsgs = new Message[maxNumOfMsgs];
					for(int i = 0; i < maxNumOfMsgs; ++i) {
						sendMsgs[i] = new Message();
						mos.writeMessage(sendMsgs[i]);
//						try {
//							Thread.sleep(10000);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							return;
//						}
					}
					
					try {
						mos.stop();
						mos.join();
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					
					

				} catch (IOException e) {
					printMessage(e.toString());
				}

			}
			
			private void printMessage(String str) {
				System.out.println("[Client] "+str);
			}
			
		};
		

		serverThread.start();
		clientThread.start();
		
		serverThread.join();
		clientThread.join();
		
		System.out.println("Checking if all messages were sent.");
		for(Message m : sendMsgs) {
			m.waitMessageSent();
		}
	}
	
}

