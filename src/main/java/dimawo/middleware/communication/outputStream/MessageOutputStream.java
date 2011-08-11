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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.LinkedList;
import java.util.Timer;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.FailureDetectionOutputStream;
import dimawo.middleware.communication.IdentificationMessage;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.simulation.socket.SocketInterface;




public class MessageOutputStream
extends LoggingAgent {

	/** Number of message send or connection
	 * retries before the channel is reported as broken */
	final static int retries = 3;
	/** Base time in milliseconds between the retries. After a failure
	 * this time is doubled */
	final static int retryTO = 1000;
	/** Time out of connection failure */
	final static int connectionTO = 10000;
	/** Time out of ack read failure */
	final static int ackTO = 20000;

	/** The Communicator */
	protected CommunicatorInterface com;
	/** Address of the remote node */
	protected DAId remoteDaId;
	/** Address of this node */
	protected DAId thisDaId;
	private int seqNum;
	private int lastSent;

	private enum State {open, broken, closed};
	private State state;

	/** Timer used to send the heart beats */
	protected Timer timer;
	
	
	private FailureDetectionOutputStream out;


	public MessageOutputStream(CommunicatorInterface com, DAId remoteDaId)
	throws FileNotFoundException {

		super(com, "MessageOutputStream_to_"+remoteDaId);

		thisDaId = com.getHostingDaId();
		this.com = com;
		this.remoteDaId = remoteDaId;
		seqNum = 0;
		lastSent = -1;
		state = null;
		
		agentPrintMessage("MessageOutputStream constructed.");

	}
	

	////////////////////
	// Public methods //
	////////////////////
	
	public DAId getRemoteDaId() {

		return remoteDaId;

	}

	/**
	 * Requests a given message to be sent.
	 * 
	 * @param m A message
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void writeMessage(Message m) throws IOException {
		
		synchronized(this) {
			if(state != null && ! isOpen())
				throw new IOException("Stream is not open");
		}
		
		m.setSender(thisDaId);
		m.setRecipient(remoteDaId);
		try {
			submitMessage(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Signals an AccessorsHandler waits a close confirmation.
	 * @param mos
	 * @throws IOException 
	 */
	public void writeClose(MessageOutputStreamAccessor mos) throws IOException {
		synchronized(this) {
			if(state != null && ! isOpen())
				throw new IOException("Stream is not open");
		}
		
		try {
			submitMessage(new CloseConfirmationRequest(mos));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private boolean isBroken() {

		return State.broken.equals(state);

	}
	
	
	private boolean isClosed() {

		return State.closed.equals(state);

	}
	
	
	private boolean isOpen() {

		return State.open.equals(state);

	}


	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void init() throws Exception {

		agentPrintMessage("Connecting to "+remoteDaId+"...");
		
		try {

			connectToRemoteDA();
			synchronized (this) {
				state = State.open;
			}
//			startHeartBeat();

		} catch(IOException e) {
			
			signalBrokenStream(e);
			
		} catch(OutOfSyncException e) {
			
			signalBrokenStream(e);
			
		}
		
		agentPrintMessage("Connected to "+remoteDaId);

	}
	
	@Override
	protected void logAgentExit() {
		LinkedList<Object> l = this.flushPendingMessages();
		rejectPendingMessages(l);

		try {
			if(out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(state.equals(State.broken))
			com.signalBrokenOutputStream(this);
		else {
			synchronized(this) {
				state = State.closed;
			}
			com.signalClosedOutputStream(this);
		}
		
		agentPrintMessage("exit");
	}


	private void rejectPendingMessages(LinkedList<Object> l) {
		for(Object o : l) {
			if(o instanceof Message) {
				Message m = (Message) o;
				m.setMessageSent(false);
			} else if(o instanceof CloseConfirmationRequest) {
				CloseConfirmationRequest req = (CloseConfirmationRequest) o;
				req.getAccessor().confirmClose(false);
			}
		}
	}


	@Override
	protected void handleMessage(Object o) throws Exception {
		try {
//			stopHeartBeat();
			if( ! isOpen()) {
				
				if(o instanceof Message) {
					Message m = (Message) o;
					m.setMessageSent(false);
				} else if(o instanceof CloseConfirmationRequest) {
					CloseConfirmationRequest req = (CloseConfirmationRequest) o;
					req.getAccessor().confirmClose(false);
				}
				return;

			}
			
			if(o instanceof CloseConfirmationRequest) {

				CloseConfirmationRequest req = (CloseConfirmationRequest) o;
				req.getAccessor().confirmClose(true);

			} else if(o instanceof Message) {

				Message m = (Message) o;
				m.setSender(thisDaId);
				m.setRecipient(remoteDaId);

				assert seqNum-1 == lastSent;
				lastSent = seqNum;
				m.setSeqNum(seqNum++);
				
				
				agentPrintMessage("Sending message n°"+m.getSeqNum()+
						" ("+o.getClass().getSimpleName()+")");

				try {

					sendMessage(m);
					m.setMessageSent(true);
//					startHeartBeat();

				} catch (NotSerializableException e) {

					throw e;

				} catch (IOException e) {

					signalBrokenStream(e);
					m.setMessageSent(false);

				} catch (OutOfSyncException e) {

					signalBrokenStream(e);
					m.setMessageSent(false);

				}

			} else {

				throw new UnknownAgentMessage(o);

			}
		} catch (Exception e) {
			synchronized (this) {
				state = State.closed;
			}
			
			throw e;
		}
		
	}


	/////////////////////
	// Private methods //
	/////////////////////

	private void signalBrokenStream(Exception e) throws InterruptedException {

		if( ! isBroken() && ! isClosed()) {

			synchronized (this) {
				state = State.broken;
			}
			agentPrintMessage("Destination DA "+remoteDaId+
					" seems to be broken : ");
			agentPrintMessage(e);

			try {

				stop(); // Must stop

			} catch (AgentException e1) {
				agentPrintMessage(e1);
			}

		}

	}
	
	
	/**
	 * Stops the heart beat timer.
	 */
//	private void stopHeartBeat() {
//
//		if(timer != null) {
//
//			timer.cancel();
//			timer = null;
//
//		}
//
//	}

	/**
	 * Starts the heart beat timer
	 */
//	private void startHeartBeat() {
//
//		timer = new Timer("Heart beat timer", true);
//		timer.schedule(new TimerTask() {
//
//			public void run() {
//				
//				try {
//
//					submitMessage(new HeartBeat());
//
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				
//			}
//
//		}, heartBeatTO);
//
//	}
	

	/**
	 * Creates the low-level connection to the remote DA.
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	protected void connectToRemoteDA() throws IOException, OutOfSyncException {

		int currentRetryTO = retryTO;

		// Connect socket
		for(int i = 0; i < retries; ++i) {

			SocketInterface sock = com.getSocketFactory().newSocket();

			try {

				// Streams creation
				sock.connect(
						sock.getSocketAddress(remoteDaId.getHostName(), remoteDaId.getPort()),
						connectionTO);
				out = new FailureDetectionOutputStream(sock, ackTO);
				
				// Send identification message
				out.writeObject(new IdentificationMessage(thisDaId));

				agentPrintMessage("Stream connected to "+
						sock.getRemoteSocketAddress());
				
				// Properly identified
				return;

			} catch(IOException e) {

				agentPrintMessage("Ignored exception #"+(i+1)+
						" on connection to DA "+remoteDaId + 
						":");
				agentPrintMessage(e);

				// Wait some time before re-connection
				try {
					Thread.sleep(retryTO);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
				currentRetryTO *= 2;
				
				try {
					sock.close();
				} catch(Throwable e1) {}

			}

		}
		
		throw new IOException("Could not connect to remote DA "+remoteDaId);
	}

	/**
	 * Sends a message to the remote DA.
	 * 
	 * @param m The message to send.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws AgentException
	 */
	protected void sendMessage(Message m) throws IOException, OutOfSyncException {
		if(m.isReliabilityFlagSet()) {
			out.writeMessage(m, true);
		} else {
			out.writeMessage(m, false);
		}
	}

//	/**
//	 * Resets the connection to the remote DA. This method is called if a message
//	 * could not be sent. This ensures that the next retry will be done on a
//	 * "good basis".
//	 * 
//	 * @throws IOException
//	 * @throws OutOfSyncException 
//	 * @throws InterruptedException 
//	 * @throws InterruptedException
//	 * @throws AgentException
//	 */
//	private void resetConnection() throws IOException, OutOfSyncException {
//
//		try {
//
//			if(out != null)
//				out.close();
//
//		} catch(IOException e) {}
//
//		out = null;
//		connectToRemoteDA();
//
//	}
	
	@Override
	public void agentPrintMessage(String msg) {
		com.printMessage("[MOS to "+remoteDaId+"] "+msg);
	}
	
	@Override
	public void agentPrintMessage(Throwable t) {
		com.printMessage("[MOS to "+remoteDaId+"] Exception");
		com.printMessage(t);
	}
}
