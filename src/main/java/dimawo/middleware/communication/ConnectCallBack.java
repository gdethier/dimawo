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
package dimawo.middleware.communication;

import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.distributedAgent.DAId;

public class ConnectCallBack {
	private DAId daId;
	private boolean success;
	private MOSAccessorInterface access;
	private Object attachment;
	
	public ConnectCallBack(DAId daId, MOSAccessorInterface access, Object attach) {
		this.daId = daId;
		this.success = true;
		this.access = access;
		this.attachment = attach;
	}
	
	public ConnectCallBack(DAId daId, Object attach) {
		this.daId = daId;
		this.success = false;
		this.access = null;
		this.attachment = attach;
	}
	
	public DAId getDaId() {
		return daId;
	}
	
	public boolean isSuccessful() {
		return success;
	}
	
	public MOSAccessorInterface getAccess() {
		return access;
	}
	
	public Object getAttachment() {
		return attachment;
	}
}
