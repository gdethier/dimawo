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
package dimawo.middleware.communication.outputStream;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;




public class MessageOutputStreamAccessor
implements MOSAccessorInterface, Comparable<MessageOutputStreamAccessor> {
	
	private int uid;

	private boolean accessorBroken;
	private boolean accessorClosed, closeConfirmed;

	private Communicator com;
	private MessageOutputStream mos;
	private MOSCallBack cb;
	
	private Semaphore waitClose;
	private boolean closeSuccess;
	

	public MessageOutputStreamAccessor(int uid, Communicator com,
			MOSCallBack cb,
			MessageOutputStream mos) {
		
		this.uid = uid;

		accessorBroken = false;
		accessorClosed = false;
		closeConfirmed = false;
		this.com = com;
		this.mos = mos;
		this.cb = cb;

	}

	
	@Override
	public void writeBlockingMessage(Message m) throws IOException, InterruptedException {
		
		synchronized(this) {
		
			if(accessorClosed) {
				
				throw new IOException("Accessor closed.");
				
			}
			
			if(accessorBroken) {
				
				throw new IOException("Accessor broken.");
				
			}
		
		}

		m.enableWaitSent();
		mos.writeMessage(m);
		if( ! m.waitMessageSent()) {
			
			synchronized(this) {
				
				accessorBroken = true;
				throw new IOException("Accessor broken.");
				
			}
			
		}

	}
	
	
	@Override
	public void writeNonBlockingMessage(Message m) {
		synchronized(this) {
			if(accessorClosed || accessorBroken) {
				m.setSender(this.com.getHostingDaId());
				m.setRecipient(mos.getRemoteDaId());
				m.setMessageSent(false);
			}
		}

		try {
			mos.writeMessage(m);
		} catch (IOException e) {
			m.setMessageSent(false);
		}

	}

	@Override
	public void close() throws IOException, InterruptedException {
		
		synchronized(this) {
		
			if(accessorClosed) {
				
				throw new IOException("Accessor already closed.");
				
			}
			accessorClosed = true;
			
			if(accessorBroken) {
				
				throw new IOException("Accessor broken.");
				
			}

		}

		// Signal accessor closed
		waitClose = new Semaphore(0);
		mos.writeClose(this);
		waitClose.acquire();
		
		if( ! closeSuccess)
			throw new IOException("Unsuccessful close");

	}


	public synchronized void confirmClose(boolean success) {
		if(accessorClosed) {
			closeConfirmed = true;
			closeSuccess = success;
			com.accessorClosed(mos.getRemoteDaId(), this);
			waitClose.release();
		}
	}


	@Override
	public int compareTo(MessageOutputStreamAccessor other) {
		return uid - other.uid;
	}


	public synchronized void signalBroken() {

		if(closeConfirmed)
			return;

		if( ! accessorBroken) {
			accessorBroken = true;
			if(cb != null)
				cb.signalBroken(new BrokenDA(mos.getRemoteDaId()));
		}
	}


	@Override
	public DAId getDestinationDAId() {
		return mos.getRemoteDaId();
	}

}
