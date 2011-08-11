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
package dimawo.simulation.host;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.simulation.socket.VirtualSocket;



public class ConnectionEvent {
	
	private VirtualSocket conSock;
	private boolean isAccepted;
	private Semaphore sem;


	public ConnectionEvent(VirtualSocket sock) {
		conSock = sock;
		isAccepted = false;
		sem = new Semaphore(0);
	}

	public VirtualSocket getConnecting() {
		return conSock;
	}
	
	public void accept() {
		isAccepted = true;
	}

	public boolean isAccepted() {
		return isAccepted;
	}

	public void acquire(long timeout) throws InterruptedException {
		sem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
	}

	public void release() {
		sem.release();
	}

}
