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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;
import dimawo.simulation.socket.VirtualSocketAddress;



public class VNetTestSender implements Runnable, VNetTestConstants {
	
	private VirtualHost host;
	
	private int taskId;
	private int nHosts;
	
	private SocketFactory sockFact;


	public VNetTestSender(VirtualHost host, int taskId, int nHosts) throws NetworkException {
		this.host = host;
		
		this.taskId = taskId;
		this.nHosts = nHosts;
		
		sockFact = new SocketFactory(host.getAccess());
	}

	@Override
	public void run() {
		
		try {
			SocketInterface sock = sockFact.newSocket();
			String hostName = "host"+(taskId + 1)%nHosts;
			sock.connect(new VirtualSocketAddress(hostName, PORT), CONNECT_TO);
			
			printMessage("local address: "+sock.getLocalSocketAddress());
			printMessage("remote address: "+sock.getRemoteSocketAddress());
			
			ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
			Random r = new Random(System.currentTimeMillis());
			for(int i = 0; i < MSGS; ++i) {
				int msg = r.nextInt();
				printMessage("Sending message "+i+": "+msg);

				oos.writeObject(new Integer(msg));
				int ack = (Integer) ois.readObject();
				
				if(msg != ack) {
					throw new IOException(msg+" /= "+ack);
				}
				
				if(taskId == 0 && i == 2) {
					printMessage("\"Crashing\"");
					sock.close();
					printMessage("Socket closed");
					return;
				}
			}
			
			sock.close();
		} catch (Exception e) {
			printMessage(e.toString());
			e.printStackTrace(System.out);
		}

	}

	private void printMessage(String msg) {
		System.out.println("[sender"+taskId+"] "+msg);
	}
}
