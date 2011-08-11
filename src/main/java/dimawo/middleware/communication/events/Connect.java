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
package dimawo.middleware.communication.events;

import java.util.concurrent.Semaphore;

import dimawo.middleware.communication.ConnectionRequestCallBack;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;



public class Connect {
	
	private DAId daId;
	
	private boolean syncConnect;
	private Semaphore sync;
	private MOSAccessorInterface access;
	
	private ConnectionRequestCallBack connCB;
	private MOSCallBack errCB;
	private Object attach;
	
	
	public Connect(DAId daId, ConnectionRequestCallBack ccb,
			MOSCallBack errCB, Object attach) {

		this.daId = daId;

		syncConnect = (ccb == null);
		if(syncConnect)
			sync = new Semaphore(0);
		
		this.connCB = ccb;
		this.errCB = errCB;
		this.attach = attach;
	}

	public boolean isSyncConnect() {
		return syncConnect;
	}
	
	public void waitConnect() throws InterruptedException {
		sync.acquire();
	}
	
	public void signalConnect(MOSAccessorInterface access) {
		this.access = access;
		sync.release();
	}
	
	public DAId getDaId() {
		return daId;
	}	
	
	public ConnectionRequestCallBack getConnectionCB() {
		
		return connCB;
		
	}
	
	public MOSCallBack getErrorCB() {
		
		return errCB;
		
	}
	
	public Object getAttachment() {
		return attach;
	}


	public MOSAccessorInterface getAccessor() {
		return access;
	}

}
