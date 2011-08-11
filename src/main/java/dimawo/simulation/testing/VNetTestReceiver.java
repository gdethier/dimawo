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

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;




public class VNetTestReceiver implements Runnable, VNetTestConstants {
	
	private VirtualHost host;
	
	private int taskId;
	private int nHosts;
	
	private SocketFactory sockFact;


	public VNetTestReceiver(VirtualHost host, int taskId, int nHosts) throws NetworkException {
		this.host = host;
		
		this.taskId = taskId;
		this.nHosts = nHosts;
		
		sockFact = new SocketFactory(host.getAccess());
	}

	@Override
	public void run() {
		
		SocketInterface sock = null;
		try {
			ServerSocketInterface serv = sockFact.newServerSocket(PORT);
			sock = serv.accept();
			serv.close();
			
			printMessage("local address: "+sock.getLocalSocketAddress());
			printMessage("remote address: "+sock.getRemoteSocketAddress());
			
			ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
			for(int i = 0; i < MSGS; ++i) {
				printMessage("Receiving message "+i);
				int msg = (Integer) ois.readObject();
				oos.writeObject(new Integer(msg));
			}
			
			sock.close();
		} catch (Exception e) {
			if(sock != null)
				try {
					sock.close();
				} catch (IOException e1) {
				}
			
			printMessage(e.toString());
			e.printStackTrace(System.out);
		}
		
	}

	private void printMessage(String msg) {
		System.out.println("[receiver"+taskId+"] "+msg);
	}
}
