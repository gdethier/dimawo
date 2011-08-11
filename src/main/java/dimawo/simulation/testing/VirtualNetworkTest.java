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

import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.net.VirtualNetwork;

public class VirtualNetworkTest {

	/**
	 * @param args
	 * @throws NetworkException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws NetworkException, InterruptedException {
		
		if(args.length != 1) {
			System.out.println("Usage: <nHosts>");
			System.exit(-1);
		}
		
		int nHosts = Integer.parseInt(args[0]);
		
		// Creating network, hosts and tasks
		VirtualNetwork net = new VirtualNetwork();
		TestUnit[] units = new TestUnit[nHosts];
		for(int i = 0; i < nHosts; ++i) {
			VirtualHost host = new VirtualHost("host"+i, net);
			VNetTestReceiver recv = new VNetTestReceiver(host, i, nHosts);
			VNetTestSender send = new VNetTestSender(host, i, nHosts);
			TestUnit unit = new TestUnit(i, host, recv, send);
			
			units[i] = unit;
		}
		
		// Starting receivers
		for(int i = 0; i < nHosts; ++i) {
			units[i].startReceiver();
		}
		
		// Starting senders
		for(int i = 0; i < nHosts; ++i) {
			units[i].startSender();
		}

		// Join tasks
		for(int i = 0; i < nHosts; ++i) {
			units[i].join();
		}
	}

}
