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
package dimawo.simulation.host.events;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.simulation.host.TimeoutException;
import dimawo.simulation.net.NetworkException;



public class NetworkEvent {
	
	private String destHostname;
	private NetworkException error;
	protected Semaphore waitSem;


	public NetworkEvent(String destHost) {
		this.destHostname = destHost;
		waitSem = new Semaphore(0);
	}
	
	public void waitOn() throws InterruptedException {
		waitSem.acquire();
		waitSem.release(); // If several threads are waiting.
	}
	
	public void waitOn(long timeout) throws TimeoutException, InterruptedException {
		if( ! waitSem.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException();
		}
	}
	
	public NetworkException getException() {
		return error;
	}
	
	public boolean isSuccessful() {
		return error == null;
	}

	public void signalError(NetworkException error) {
		this.error = error;
		waitSem.release();
	}

	public String getHostname() {
		return destHostname;
	}
	
	public void signalSuccess() {
		waitSem.release();
	}

}
