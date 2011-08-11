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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.events.AccessorClosed;
import dimawo.middleware.communication.events.BrokenDA;
import dimawo.middleware.communication.events.CheckWaitingConnections;
import dimawo.middleware.communication.events.ClosedOutput;
import dimawo.middleware.communication.events.Connect;
import dimawo.middleware.communication.events.ConnectionHandlerClosed;
import dimawo.middleware.communication.events.MulticastMessage;
import dimawo.middleware.communication.events.NewMOS;
import dimawo.middleware.communication.events.NewMOSConnection;
import dimawo.middleware.communication.events.NewRMOSConnection;
import dimawo.middleware.communication.events.PrepareStop;
import dimawo.middleware.communication.events.SendDatagramMessage;
import dimawo.middleware.communication.events.TriggerConnectionGc;
import dimawo.middleware.communication.inputStream.MessageInputStream;
import dimawo.middleware.communication.messages.CommunicatorMessage;
import dimawo.middleware.communication.outputStream.LocalAccessor;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.communication.outputStream.MessageOutputStreamAccessor;
import dimawo.middleware.communication.server.CommunicatorConnectionHandler;
import dimawo.middleware.communication.server.ServerLoop;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgentInterface;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayMessage;
import dimawo.simulation.socket.SocketFactory;


/**
 * The communicator provides several communication means:
 * <ul>
 * <li>a connected mode where a component requests a connection and
 * uses the obtained connection in order to send messages</li>
 * <li>a "datagram" mode where messages are directly submitted to Communicator
 * in order to be sent. In this case, the Communicator establishes the
 * communication to the destination computer if it is has not already been set
 * up. After the message os</li>
 * </ul>
 * 
 * @author Gerard Dethier
 */
public class Communicator
extends LoggingAgent
implements CommunicatorInterface {

	/** Indicates the communicator is going down */
	private int closeLevel;
	private boolean connectionHandlerClosed;


	/** Communicator connections handler */
	private CommunicatorConnectionHandler cH;
	/** Listener for incoming connections */
	private ServerLoop server;

	/** The hosting DA */
	private DistributedAgentInterface da;
	private OverlayAgentInterface overInt;
	private SocketFactory sockFact;

//	/** The address cache */
//	private AddressCache cache;
	private BrokenDaCache brokenCache;

	int nextAccessorUID;
	/** Connected output message streams */
	private TreeMap<DAId, Connection> openConnections;
	/** Components waiting a connection (DA address is not in local cache) */
	private TreeMap<DAId, LinkedList<Connect>> waitingConnections;
	/** Datagram messages waiting a connection to be sent */
	private QueuedDatagramMessages queuedMsg;
	
	/** Maintains data and implements actions for handling output streams */
	private OutputStreamsHandler outHandler;
	
	/** Active MessageReaders */
	private TreeMap<DAId, MessageInputStream> inputStreams;
	
	private MessageHandlersRegistry msgHandReg;


	/////////////////
	// Constructor //
	/////////////////	

	public Communicator(OverlayAgentInterface overInt,
			DistributedAgentInterface da, SocketFactory sockFact) throws IOException {

		super(da, "Communicator");
		
		closeLevel = 0;
		connectionHandlerClosed = false;

		nextAccessorUID = 0;

		this.da = da;
		this.overInt = overInt;
		this.sockFact = sockFact;
		
		setPrintStream(da.getFilePrefix());

		brokenCache = new BrokenDaCache(100);

		openConnections = new TreeMap<DAId, Connection>();
		waitingConnections = new TreeMap<DAId, LinkedList<Connect>>();
		queuedMsg = new QueuedDatagramMessages();
		
		outHandler = new OutputStreamsHandler(this);

		inputStreams = new TreeMap<DAId, MessageInputStream>();
		
		cH = new CommunicatorConnectionHandler(this, overInt, 70, 5000);
		server = new ServerLoop(da.getTcpPort(), this, cH, sockFact);

		msgHandReg = new MessageHandlersRegistry();
	}


	////////////////////
	// Public methods //
	////////////////////
	
	@Override
	public void stop() {
		try {
			submitMessage(new PrepareStop());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void sendDatagramMessage(Message msg) {
		if(msg.getRecipient() == null)
			throw new Error("No recipient specified");
		
		try {
			submitMessage(new SendDatagramMessage(msg));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void multicastMessage(DAId[] ids, Message msg) {
		try {
			submitMessage(new MulticastMessage(ids, msg));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void submitIdentificationMessage(IdentificationMessage o,
			FailureDetectionInputStream chan) throws InterruptedException {

		submitMessage(new NewRMOSConnection(o, chan));

	}
	
	public void submitConnectionHandlerClosed() {
		try {
			submitMessage(new ConnectionHandlerClosed());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void checkWaitingConnections(DAId daId)
	throws InterruptedException {

		this.submitMessage(new CheckWaitingConnections(daId));

	}


	/**
	 * @throws InterruptedException 
	 * 
	 */
	public void submitIncomingMessage(Message msg) {

		if(msg.getSender() == null)
			throw new Error("No sender set");

		if(msgHandReg.submitMessageToHandlers(msg)) {
//			agentPrintMessage("Handler available for id "+msg.getHandlerId());
			return;
		}

		if(msg instanceof CommunicatorMessage) {
			try {
				submitMessage(msg);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if(msg instanceof MulticastInstanceMessage) {
			MulticastInstanceMessage mim = (MulticastInstanceMessage) msg;
			Message m = mim.getMessage();
			
			m.setRecipient(mim.getRecipient());
			m.setSender(mim.getSender());
			
			submitIncomingMessage(m);
		} else if(msg instanceof OverlayMessage) {
			overInt.submitOverlayMessage((OverlayMessage) msg);
		} else {
			try {
				da.submitIncomingMessage(msg);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Gives the ID of the hosting DA.
	 * 
	 * @return A DA ID.
	 */
	public DAId getHostingDaId() {
		
		return da.getDaId();
		
	}
	
	public DistributedAgentInterface getHostingDa() {
		return da;
	}

	/**
	 * Requests the connection to a given DA.
	 * 
	 * @param daId The ID of the DA to connect to.
	 * @param cb A CommunicatorCallBack.
	 * @throws InterruptedException 
	 */
	public void asyncConnect(DAId daId, ConnectionRequestCallBack cb, MOSCallBack errCB, Object attachment) throws InterruptedException {

		submitMessage(new Connect(daId, cb, errCB, attachment));

	}
	
	
	/**
	 * Requests the connection to a given DA.
	 * 
	 * @param daId The ID of the DA to connect to.
	 * @param cb A CommunicatorCallBack.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public MOSAccessorInterface syncConnect(DAId daId, MOSCallBack errCB) throws InterruptedException, IOException {

		Connect c = new Connect(daId, null, errCB, null);
		submitMessage(c);
		c.waitConnect();
		MOSAccessorInterface mai = c.getAccessor();
		if(mai == null)
			throw new IOException("Could not connect to DA "+daId);
		return mai;

	}
	
	
	/**
	 * Signals a broken remote DA. This method is called
	 * by the MessageOutputStream associated to the broken
	 * remote DA.
	 * 
	 * @param remoteDaAddr The address of the remote DA.
	 * @throws InterruptedException 
	 */
	@Override
	public void signalBrokenOutputStream(MessageOutputStream mos) {
		
		try {
			submitMessage(new BrokenDA(mos));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Signals a closed stream to a remote DA. This method is called
	 * by the MessageOutputStream associated to the remote DA.
	 * 
	 * @param remoteDaAddr The address of the remote DA.
	 */
	public void signalClosedOutputStream(MessageOutputStream mos) {
		
		try {
			submitMessage(new ClosedOutput(mos));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Signals the closing of an MessageOutputStreamAccessor.
	 * 
	 * @param remoteDaId The ID of the remote DA associated to the accessor.
	 * 
	 * @throws InterruptedException 
	 */
	public void accessorClosed(DAId remoteDaId, MessageOutputStreamAccessor access) {

		try {
			submitMessage(new AccessorClosed(remoteDaId, access));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}


	/////////////////////////////////////////////////////////
	// TransactionRequestsInterfaceCallBack implementation //
	/////////////////////////////////////////////////////////

//	@Override
//	public synchronized void submitTransactionResult(TransactionResult result) {
//		
//		try {
//			submitMessage(new CommunicatorTransactionResult((GetResult) result));
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//	}


	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void logAgentExit() {

		agentPrintMessage("exit");
		exitCleanUp();

	}


	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		
		cH.start();
		server.start();
	}


	@Override
	protected void handleMessage(Object m) {

		try {
			
			if(closeLevel >= 3) {
				agentPrintMessage("Communicator close level 3, ignoring message "+m.getClass().getName());
				return;
			}

			if(m instanceof ConnectionHandlerClosed) {
				handleConnectionHandlerClosed();
			} else if(m instanceof NewMOSConnection) {
				handleNewMOSConnection((NewMOSConnection) m);
			} else {
				if(closeLevel >= 2) {
					agentPrintMessage("Communicator close level 2, ignoring message "+m.getClass().getName());
					return;
				}

				else if(m instanceof AccessorClosed) {
					handleAccessorClosed((AccessorClosed) m);
				} else if(m instanceof ConnectionHandlerClosed) {
					handleConnectionHandlerClosed();
				} else if(m instanceof NewMOS) {
					handleNewMOS((NewMOS) m);
				} else if(m instanceof ClosedOutput) {
					handleClosedOutput((ClosedOutput) m);
				} else if(m instanceof BrokenDA) {
					handleBrokenDA((BrokenDA) m);
				} else if(m instanceof CheckWaitingConnections) {
					handleCheckWaitingConnections((CheckWaitingConnections) m);
				} else {
					if(closeLevel >= 1) {
						agentPrintMessage("Communicator close level 1, ignoring message "+m.getClass().getName());
						return;
					}

					if(m instanceof MulticastMessage) {
						handleMulticastMessage((MulticastMessage) m);
					} else if(m instanceof Connect) {
						handleConnect((Connect) m);
					} else if(m instanceof SendDatagramMessage) {
						handleSendDatagramMessage((SendDatagramMessage) m);
					} else if(m instanceof PrepareStop) {
						handlePrepareStop();
					} else if(m instanceof TriggerConnectionGc) {
						handleTriggerConnectionGc();
					} else {
						throw new UnknownAgentMessage(m);
					}
				}
			}
		} catch (Exception e) {
			da.signalChildError(e, this.getClass().getName());
		}

	}


	private void handleTriggerConnectionGc() throws Exception {
		
		boolean triggerNewGc = false;
		for(Iterator<Entry<DAId, Connection>> it = openConnections.entrySet().iterator();
		it.hasNext();) {
			Entry<DAId, Connection> e = it.next();
			Connection c = e.getValue();
			if(c.isGcAble()) {
				agentPrintMessage("GC of connection to "+c.getDaId());
				c.close(outHandler);
				it.remove();
			}
			if(c.getConnectedComponentsCount() == 0)
				triggerNewGc = true;
		}
		
		if(triggerNewGc)
			triggerConnectionsGc();
	}


	private void handleNewMOS(NewMOS newmos) throws Exception {

		MessageOutputStream o = newmos.getMOS();
		DAId daId = newmos.getDaId();

		LinkedList<Connect> waitingComponents =
			waitingConnections.remove(daId);
		LinkedList<Message> waitingDatagrams =
			queuedMsg.removeQueue(daId);

		if(closeLevel > 0 && waitingComponents != null) {
			for(Connect c : waitingComponents) {
				finishConnection(c, null);
			}
			waitingComponents = null;
		}

		if(o == null) {

			// MOS could not be created
			if(waitingComponents != null) {
				Iterator<Connect> itCon = waitingComponents.iterator();
				while(itCon.hasNext()) {
					Connect c = itCon.next();
					finishConnection(c, null);
				}
			}

			if(waitingDatagrams != null) {

				Iterator<Message> itMsg = waitingDatagrams.iterator();
				while(itMsg.hasNext()) {
					Message msg = itMsg.next();
					msg.setMessageSent(false);
				}

			}

		} else {
			
			agentPrintMessage("Creating connection to "+daId);
			Connection c = new Connection(daId, o);
			if(openConnections.put(daId, c) != null)
				throw new Error("A connection was already existing");

			if(waitingComponents != null) {
				Iterator<Connect> it = waitingComponents.iterator();
				while(it.hasNext()) {

					Connect wc = it.next();
					MessageOutputStreamAccessor acc =
						new MessageOutputStreamAccessor(nextAccessorUID++, this,
								wc.getErrorCB(), o);
					c.addConnectedComponent(acc);

					finishConnection(wc, acc);
				}
			}

			// If there are waiting datagram messages they are sent
			if(waitingDatagrams != null) {
				agentPrintMessage("-- Sending waiting datagrams");

				// o is not started yet, no exception can occur
				Iterator<Message> it = waitingDatagrams.iterator();
				while(it.hasNext()) {
					Message m = it.next();
					c.writeMessage(m);
				}
			}

			try {
				o.start();
			} catch (AgentException e) {
				e.printStackTrace();
			}

			triggerConnectionsGc();
		}

		if(closeLevel > 0) {
			tryStop();
		}

	}


	private void triggerConnectionsGc() {
		Timer t = new Timer(true);
		t.schedule(
			new TimerTask() {
			public void run() {
				try {
					submitMessage(new TriggerConnectionGc());
				} catch (InterruptedException e) {
				}
			}
		}, 10000);
	}


	private void handlePrepareStop() throws InterruptedException, AgentException {
		
		agentPrintMessage("Received stop request, flushing all waiting messages...");

		// Reject waiting connections
		rejectAllWaitingConnections();
		
		closeLevel = 1;
		tryStop();
	}


	private void rejectAllWaitingConnections() {
		Iterator<Entry<DAId, LinkedList<Connect>>> waitConIt =
			waitingConnections.entrySet().iterator();
		while(waitConIt.hasNext()) {
			Entry<DAId, LinkedList<Connect>> e = waitConIt.next();
			LinkedList<Connect> conList = e.getValue();
			Iterator<Connect> conIt = conList.iterator();
			while(conIt.hasNext()) {
				Connect con = conIt.next();
				finishConnection(con, null);
			}
		}
		waitingConnections.clear();
	}


	private void tryStop() throws InterruptedException, AgentException {
		
		// First stage
		if(closeLevel == 1 && queuedMsg.isEmpty()) {
			agentPrintMessage("Switching to close level 2...");
			closeLevel = 2;
			server.stop();
		} else if(closeLevel == 2 && connectionHandlerClosed) {
			closeLevel = 3;
			super.stop();
		}

	}
	
	
	private void handleConnectionHandlerClosed() throws InterruptedException, AgentException {

		connectionHandlerClosed = true;
		tryStop();

	}

	
	/////////////
	// Helpers //
	/////////////
	
	private void handleMulticastMessage(MulticastMessage m) throws Exception {
		
		DAId[] toIds = m.getToIds();
		for(int i = 0; i < toIds.length; ++i) {
			
			DAId to = toIds[i];
			agentPrintMessage("Broadcasting Datagram to "+to);
			MulticastInstanceMessage mim = m.getMulticastInstanceMessage(to);
			handleSendDatagramMessage(new SendDatagramMessage(mim));
			
		}
		
	}
	
	private void handleSendDatagramMessage(SendDatagramMessage sdm) throws Exception {
		Message m = sdm.getMessage();
		DAId destId = m.getRecipient();
		
		if(brokenCache.isBroken(destId)) {
			m.setMessageSent(false);
			return;
		}
		
		// Loopback
		if(destId.equals(da.getDaId())) {
			agentPrintMessage("Datagram loopback "+m.getClass().getName());
			m.setSender(da.getDaId());
			this.submitIncomingMessage(m);
			return;
		}

		// Send message to another DA
		Connection conn = openConnections.get(destId);
		if(conn != null) {
			try {
				conn.writeMessage(m);
			} catch (IOException e) {
				m.setMessageSent(false);
			}
		} else {
			
			boolean alreadyWaitingMOS = queuedMsg.queueDatagramMessage(m);
			if( ! alreadyWaitingMOS) {
				// Address found in local cache
				agentPrintMessage("Sending datagram to "+destId);

				outHandler.open(destId);
			}
		}
		
	}
	
	
	private void handleCheckWaitingConnections(CheckWaitingConnections m)
	throws Exception {

		DAId daId = m.getDaId();
		establishWaitingConnections(daId);
		
	}


	private void closeRemainingConnections() {

		Iterator<Connection> it2 = openConnections.values().iterator();
		while(it2.hasNext()) {
			
			Connection c = it2.next();

			agentPrintMessage("Closing connection to "+c.getDaId());
			try {

				c.close(outHandler);

			} catch (InterruptedException e) {
			} catch (IOException e) {
			} catch (Exception e) {
			}

		}
		openConnections.clear();
		
		Iterator<MessageInputStream> inIt = inputStreams.values().iterator();
		while(inIt.hasNext()) {
			MessageInputStream in = inIt.next();
			in.close();
		}
		inputStreams.clear();

	}

	/**
	 * The new connection leads to the registration of a new DA, the
	 * creation of a new MessageReader or a failure, which is ignored.
	 * @throws Exception 
	 */
	private void handleNewMOSConnection(NewMOSConnection nc) throws Exception {

		if(closeLevel > 0) {
			agentPrintMessage("Communicator closing, rejecting new MOS connection.");
			nc.finalizeConnection();
			return;
		}

		DAId senderId = nc.getIdMessage().getRemoteDaId();
		try {

			// New MessageInputStream must be created
			MessageInputStream newReader = getMessageInputStream(nc);
			MessageInputStream oldReader = inputStreams.put(senderId, newReader);
			if(oldReader != null) {

				// Old reader is closed if it existed.
				oldReader.close();

			}

			newReader.start();
			
		} catch (IOException e) {
			
			nc.finalizeConnection();

		}

	}
	
	
	/**
	 * Decrements the reference count to a connection
	 * associated to the MessageOutputStream.
	 * 
	 * @param ac Accessor closing parameter.
	 * @throws Exception 
	 */
	private void handleAccessorClosed(AccessorClosed ac)
	throws Exception {

		DAId remoteDaId = ac.getRemoteDaId();
		
		Connection con = openConnections.get(remoteDaId);
		if(con != null) {
		
			con.removeConnectedComponent(ac.getAccessor());
			if(con.getConnectedComponentsCount() == 0) {
				triggerConnectionsGc();
			}
		
		} else {
			
			assert false : "Closed accessor was not associated to an " +
					"existing connection ! ";
			
		}

	}


	/**
	 * Tries to connect to a remote DA. If necessary,
	 * the address of the DA is queried. In this case, the connection
	 * is postponed.
	 * 
	 * @param c Connection parameters.
	 * @throws Exception 
	 */
	private void handleConnect(Connect c) throws Exception {

		DAId daId = c.getDaId();

		// Check if a local accessor is needed.
		if(daId.equals(da.getDaId())) {
			
			agentPrintMessage("Local connection to DA created.");
			finishConnection(c, new LocalAccessor(this));
			return;
			
		}
		
		if(brokenCache.isBroken(daId)) {
			finishConnection(c, null);
			return;
		}

		// Connection to a DA
		Connection conn = openConnections.get(daId);
		if(conn != null) {

			// There is already a connection
			agentPrintMessage("Connection to "+daId+
					" available, returning new accessor.");

			MessageOutputStreamAccessor acc = conn.getAccessor(nextAccessorUID++, this, c.getErrorCB());
			finishConnection(c, acc);

		} else {
		
			// There is no existing connection
			agentPrintMessage("Connection to "+daId+" not available.");

			// Queue connection
			boolean alreadyWaitingMOS;
			LinkedList<Connect> waitingComponents =
				waitingConnections.get(daId);
			if(waitingComponents == null) {

				alreadyWaitingMOS = false;
				waitingComponents = new LinkedList<Connect>();
				waitingConnections.put(daId, waitingComponents);
				
			} else {
				alreadyWaitingMOS = true;
			}
			waitingComponents.add(c);
			
			if( ! alreadyWaitingMOS) {
				agentPrintMessage("Requesting new MOS for DA "+daId);
				outHandler.open(daId);
			} else {
				agentPrintMessage("Queued connection to DA "+daId);
			}
		
		}
		
	}


	private void finishConnection(Connect c, MOSAccessorInterface access) {
		
		if(c.isSyncConnect()) {

			c.signalConnect(access);

		} else {

			if(access != null)
				c.getConnectionCB().connectCallBack(new ConnectCallBack(c.getDaId(), access, c.getAttachment()));
			else
				c.getConnectionCB().connectCallBack(new ConnectCallBack(c.getDaId(), c.getAttachment()));

		}
		
	}


//	private void queryDAAddress(int daId) throws Exception {
//
//		agentPrintMessage("Requesting address for DA "+daId);
////		da.submitUrgentRequest(new GetLocalRequest(
////				DADescriptor.getDaAddressKey(daId)), this);
//		da.getMapAgent().getAsync(DADescriptor.getDaAddressKey(daId), this);
//
//	}


	/**
	 * Signals a connection failure to waiting components.
	 * 
	 * @param waitingComponents The set of waiting components.
	 * @throws InterruptedException 
	 */
	private void signalConnectionFailure(DAId daId,
			LinkedList<Connect> waitingComponents) {
		
		Iterator<Connect> it = waitingComponents.iterator();
		while(it.hasNext()) {

			Connect cb = it.next();
			cb.getConnectionCB().connectCallBack(new ConnectCallBack(daId, cb.getAttachment()));

		}
		
	}
	
	
	private void handleClosedOutput(ClosedOutput m) throws Exception {
		
		DAId id = m.getRemoteDaId();
		agentPrintMessage("Closed output to "+id);
		outHandler.closed(id, m.getMessageOutputStream());
		
	}
	
	
	/**
	 * Handles a broken DA.
	 * 
	 * @param bda
	 * 
	 * @throws InterruptedException 
	 * @throws AgentException 
	 */
	private void handleBrokenDA(BrokenDA bda) throws InterruptedException, AgentException {
		DAId daId = bda.getId();
		if(brokenCache.isBroken(daId))
			return; // already handled

		agentPrintMessage("Broken stream to "+daId);

		// Signal connection failure to waiting components (if any)
		LinkedList<Connect> waitingComponents =
			waitingConnections.remove(daId);
		if(waitingComponents != null) {
			signalConnectionFailure(daId, waitingComponents);
		}

		// Signal connection failure to connected accessors
		Connection con = openConnections.remove(daId);
		if(con != null)
			con.signalBroken();

		// Signal waiting output actions
		outHandler.broken(daId, bda.getMessageOutputStream());

		// If communicator is closing, see if broken stream
		// does not trigger shutdown.
		tryStop();
	}

	/**
	 * @throws InterruptedException
	 * @throws AgentException
	 */
	private void exitCleanUp() {
		closeRemainingConnections();
	}
	
	
	/**
	 * @param addr
	 * @throws Exception 
	 */
	private void establishWaitingConnections(DAId daId)
			throws Exception {

		if(waitingConnections.containsKey(daId) ||
			queuedMsg.hasQueue(daId))
			outHandler.open(daId);

	}
	
	
	protected MessageInputStream getMessageInputStream(NewMOSConnection con)
			throws IOException {

		NewRMOSConnection rCon = (NewRMOSConnection) con;
		return new MessageInputStream(this, con.getSender(),
				rCon.getInputStream());

	}


	protected MessageOutputStream getMessageOutputStream(DAId id)
	throws FileNotFoundException {
		
		assert ! openConnections.containsKey(id);

		return new MessageOutputStream(this, id);

	}


	public SocketFactory getSocketFactory() {
		return sockFact;
	}


	public void submitNewMOS(DAId daId, MessageOutputStream out) {
		try {
			submitMessage(new NewMOS(daId, out));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setOverlayInterface(OverlayAgentInterface overInt) {
		this.overInt = overInt;
	}
	
	@Override
	public synchronized void printMessage(String msg) {
		agentPrintMessage(msg);
	}
	
	@Override
	public synchronized void printMessage(Throwable t) {
		agentPrintMessage(t);
	}


	@Override
	public void registerMessageHandler(Object messageHandlerId,
			MessageHandler mh) {
		msgHandReg.register(messageHandlerId, mh);
	}


	@Override
	public void unregisterMessageHandler(Object messageHandlerId,
			MessageHandler mh) {
		msgHandReg.unregister(messageHandlerId, mh);
	}

}
