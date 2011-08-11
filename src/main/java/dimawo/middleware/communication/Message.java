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

import java.io.Serializable;
import java.util.concurrent.Semaphore;

import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;



public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	private transient Semaphore sent;
	private transient boolean success;

	private int seqNum; // set by MessageOutputStream
	private DAId from; // set by MessageOutputStream
	private DAId to; // set by MessageOutputStream
	private transient MOSCallBack cb; // Used by MOS to signal error
	private boolean reliabilityFlag = false;

	
	public Message() {
	}
	
	public Message(DAId to) {
		setRecipient(to);
	}
	
	public void enableWaitSent() {
		sent = new Semaphore(0);
	}
	
	public void setMessageSent(boolean success) {

		this.success = success;
		if(sent != null)
			sent.release();
		
		if(cb != null) {
//			System.out.println("DatagramMessage.setMessageSent");
			cb.signalSent(this, success);
		}

	}

	public boolean waitMessageSent() throws InterruptedException {
		if(sent != null) {
			sent.acquire();
			return success;
		}
		return true;
	}

	final public void setSeqNum(int i) {
		seqNum = i;
	}

	final public int getSeqNum() {
		return seqNum;
	}

	public void setSender(DAId from) {
		this.from = from;
	}
	
	public DAId getSender() {
		return from;
	}
	
	public DAId getRecipient() {
		return to;
	}

	public void setRecipient(DAId to) {
		this.to = to;
	}
	
	public void setCallBack(MOSCallBack cb) {
		this.cb = cb;
	}
	
	public MOSCallBack getCallBack() {
		return cb;
	}

	public Object getHandlerId() {
		return null;
	}

	public boolean isReliabilityFlagSet() {
		return reliabilityFlag;
	}
	
	public void setReliabilityFlag(boolean b) {
		this.reliabilityFlag = b;
	}
}
