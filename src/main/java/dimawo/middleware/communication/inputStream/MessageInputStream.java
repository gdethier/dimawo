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
package dimawo.middleware.communication.inputStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;

import dimawo.agents.AbstractAgent;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.FailureDetectionInputStream;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.HeartBeat;
import dimawo.middleware.communication.outputStream.OutOfSyncException;
import dimawo.middleware.distributedAgent.DAId;




public class MessageInputStream implements Runnable {
	
	/////////////
	// Members //
	/////////////
	
	/** The Channel Handler that instantiated this Input Stream Handler. */
	protected CommunicatorInterface com;
	
	/** The ID of the hosting DA */
	private DAId hostingDaId;
	private DAId remoteDaId;
	private int lastSeqNum;

	/** Indicates if the MessageInputStream has been stopped or not. */
	private boolean stopped;
	
	protected Thread readerThread;
	
	private FailureDetectionInputStream in;
	
	
	/**
	 * Constructor.
	 * 
	 * @param receiver The ID of the local DA.
	 * @param parent The ChannelHandler that calls this constructor.
	 * @param sender The ID of the remote DA.
	 * @param s The socket to use to receive data.
	 * @param consumer The agent that consumes the messages read by this Input Stream Handler.
	 * @throws IOException If the streams could not be constructed on the given socket.
	 */
	public MessageInputStream(CommunicatorInterface com,
			DAId remoteDaId,
			FailureDetectionInputStream in)
	throws IOException {

		hostingDaId = com.getHostingDaId();

		this.com = com;
		this.remoteDaId = remoteDaId;
		lastSeqNum = -1;

		stopped = false;
		
		this.in = in;
	}
	
	
	////////////////////
	// Public methods //
	////////////////////

	public DAId getRemoteDaId() {

		return remoteDaId;

	}
	
	public void start() {

		readerThread = new Thread(this, "MIS Thread for remote DA "+remoteDaId);
		readerThread.start();

	}
	
	public synchronized void close() {
		
		if( ! stopped) {
			printMessage("Closing...");
			stopped = true;
		}

	}
	

	public synchronized boolean isStopped() {

		return stopped;

	}


	/////////////////////////////
	// Runnable implementation //
	/////////////////////////////


	/**
	 * 
	 * The message reading loop. The Input Stream Handler tries to read messages
	 * from the socket until it is interrupted or an IO error occurs.
	 * 
	 */
	public void run() {
		
		printMessage("Started.");
		while(true) {
			// Low-level stream set, reception of the message.
			try {

				Message m = in.readMessage(false, hostingDaId);
				if(m.isReliabilityFlagSet()) {
					in.ack(m.getRecipient(), hostingDaId);
				}
				
				handleMessage(m);
				
				if(isStopped()) {
					printMessage("Stream closed.");
					break;
				}

			} catch (SocketTimeoutException e) {

				if(isStopped()) {
					printMessage("Stream closed.");
					break;
				} else {
					printMessage("TO but stream not closed");
					continue;
				}

			} catch (EOFException e) {

				printMessage("Stream remotely closed.");
				break;

			} catch (IOException e) {

				printMessage("Error while reading next message: ");
				printMessage(e);
				break;

			} catch (OutOfSyncException e) {

				printMessage("Stream to "+remoteDaId+" out of sync.");
				printMessage(e);
				break;

			} catch (Throwable t) {

				printMessage("Signaling error " + t);
				com.signalChildError(t, "MessageInputStream for remote DA "+
						remoteDaId);
				break;

			}

		} /* while */

		exit();

	}
	
	
	/////////////////////
	// Private methods //
	/////////////////////

	/**
	 * 
	 * This method handles a newly read message. Regarding its class,
	 * different code is executed.
	 * 
	 * @param o The message read from the socket.
	 * @throws Throwable If an error occured.
	 */
	private void handleMessage(Message o) throws Throwable {
		
		int recvSeqNum = o.getSeqNum();
		printMessage("Received message "+o.getClass().getSimpleName()+" with seqNum="+recvSeqNum);
		
		if(lastSeqNum != -1) {

			if(recvSeqNum > lastSeqNum + 1)
				throw new Exception("Lost messages ("+recvSeqNum+":"+lastSeqNum+")");
			else if(recvSeqNum <= lastSeqNum) {
				printMessage("Ignored seqNum="+recvSeqNum);
				return; // Ignore replay
			}

		}
		lastSeqNum = recvSeqNum;
		
		if(o instanceof HeartBeat) {
			return;
		}
		
		Message m = (Message) o;

		m.setSender(remoteDaId);
		com.submitIncomingMessage(m);

	}

	
	/**
	 * Prints a well-formatted logging message on standard output.
	 * 
	 * @param s The message to print.
	 * 
	 */
	protected void printMessage(String s) {
		com.printMessage("[MIS from "+remoteDaId+"] "+s);
	}
	
	protected void printMessage(Throwable t) {
		com.printMessage("[MIS from "+remoteDaId+"] Exception");
		com.printMessage(t);
	}
	
	
	protected void exit() {
		
		printMessage("Exit.");
		
		try {
			in.close();
		} catch(Exception e) {
			printMessage(e);
		}
		
	}

}
