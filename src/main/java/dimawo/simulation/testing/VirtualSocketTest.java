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
package dimawo.simulation.testing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.net.VirtualNetwork;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;
import dimawo.simulation.socket.VirtualSocketAddress;



public class VirtualSocketTest {
	
	static final int TIME_OUT = 5000;
	static final int MSG_TO_SEND = 1000;
	static final int CLIENT_SERVER_MULT = 4;
	static final int packetSize = 100;
	static final boolean virtual = true;
	
	static final VirtualNetwork net = new VirtualNetwork();
	static VirtualHost host;
	
	private static SocketFactory buildSocketFactory() throws NetworkException {
		
		if(virtual) {

			return new SocketFactory(host.getAccess());
		
		} else {
			return new SocketFactory();
		}
		
	}
	
	private static void printMessage(String msg) {
		synchronized(net) {
			System.out.println(msg);
		}
	}
	
	private static void printMessage(Throwable t) {
		synchronized(net) {
			t.printStackTrace(System.out);
		}
	}

	public static void main(String[] args) throws Exception {

		final VirtualSocketAddress serverAddr;
		final VirtualSocketAddress clientAddr;

		serverAddr = new VirtualSocketAddress("localhost", 50200);
		clientAddr = new VirtualSocketAddress("localhost", 50201);
		
		if(virtual) {
			host = new VirtualHost("localhost", net);
		}


		Thread serverThread = new Thread() {

			public void run() {
				
				SocketFactory servFact;
				try {
					servFact = buildSocketFactory();
				} catch (NetworkException e1) {
					e1.printStackTrace();
					return;
				}
				try {

					ServerSocketInterface serv =
						servFact.newServerSocket(serverAddr.getPort());
					printMessage("ServerSocket bound.");

					SocketInterface sock = serv.accept();
					serv.close();
					printMessage("Remote host: "+sock.getRemoteSocketAddress());
					
					sock.setSoTimeout(TIME_OUT);
					
					// Data test
					InputStream dis = new BufferedInputStream(sock.getInputStream());
					OutputStream ackStream = sock.getOutputStream();
					byte[] buf = new byte[packetSize * CLIENT_SERVER_MULT];
					for(int i = 0; i < MSG_TO_SEND; ++i) {

						printMessage("Receiving message n°"+i+" from client.");
						for(int j = 0; j < CLIENT_SERVER_MULT; ++j)
							dis.read(buf, j * packetSize, packetSize);
						printMessage("Received message n°"+i+" from client");
						ackStream.write(0);
						ackStream.flush();

					}
					
					
					// Object test
					ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
					String str = (String) ois.readObject();
					printMessage("Received String "+str);

					sock.close();

				} catch (Exception e) {
					printMessage(e);
				}

			}
			
			private void printMessage(String str) {
				VirtualSocketTest.printMessage("[Server] "+str);
			}
			
			private void printMessage(Throwable t) {
				VirtualSocketTest.printMessage("[Server] Exception:");
				VirtualSocketTest.printMessage(t);
			}
			
		};

		Thread clientThread = new Thread() {
			
			public void run() {
				
				SocketFactory clientFact;
				try {
					clientFact = buildSocketFactory();
				} catch (NetworkException e1) {
					e1.printStackTrace();
					return;
				}

				try {

					SocketInterface sock = clientFact.newSocket();
					sock.connect(serverAddr, TIME_OUT);
					
					printMessage("Remote host: "+sock.getRemoteSocketAddress());
					
					sock.setSoTimeout(TIME_OUT);
					
					// Data test
					OutputStream dos = sock.getOutputStream();
					InputStream ackStream = new BufferedInputStream(sock.getInputStream());
					byte[] msg = new byte[packetSize];
					Random r = new Random(System.currentTimeMillis());
					for(int i = 0; i < MSG_TO_SEND; ++i) {

						for(int j = 0; j < CLIENT_SERVER_MULT; ++j) {
							r.nextBytes(msg);
							dos.write(msg);
						}
						printMessage("Sent message n°"+i+" to server.");
						printMessage("Waiting ack from server.");
						ackStream.read();

					}
					
					
					// Object test
					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(sock.getOutputStream()));
					oos.writeObject("test");
					oos.close();

					sock.close();

				} catch (Exception e) {
					printMessage(e);
				}

			}
			
			private void printMessage(String str) {
				VirtualSocketTest.printMessage("[client] "+str);
			}
			
			private void printMessage(Throwable t) {
				VirtualSocketTest.printMessage("[client] Exception:");
				VirtualSocketTest.printMessage(t);
			}
			
		};
		

		serverThread.start();
		Thread.sleep(1000);
		clientThread.start();
		
		serverThread.join();
		clientThread.join();

	}

}
