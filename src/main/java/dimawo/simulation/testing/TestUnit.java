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

public class TestUnit {

	private int unitId;
	private VirtualHost host;
	private VNetTestReceiver recv;
	private VNetTestSender send;

	private Thread recvThread;
	private Thread sendThread;


	public TestUnit(int unitId, VirtualHost host, VNetTestReceiver recv,
			VNetTestSender send) {
		this.unitId = unitId;
		this.host = host;
		this.recv = recv;
		this.send = send;
	}

	public void startReceiver() {
		recvThread = new Thread(recv, "receiver"+unitId);
		recvThread.start();
	}

	public void startSender() {
		sendThread = new Thread(send, "sender"+unitId);
		sendThread.start();
	}

	public void join() throws InterruptedException {
		recvThread.join();
		sendThread.join();
	}
	
}
