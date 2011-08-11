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
package dimawo.middleware.distributedAgent;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

import dimawo.DiMaWoException;
import dimawo.MasterAgent;
import dimawo.MasterWorkerFactory;
import dimawo.WorkerAgent;
import dimawo.WorkerMessage;
import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UncaughtThrowable;
import dimawo.agents.UnknownAgentMessage;
import dimawo.master.messages.DaError;
import dimawo.master.messages.MasterMessage;
import dimawo.master.messages.WorkerExitMessage;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.inputStream.IncomingMessage;
import dimawo.middleware.distributedAgent.events.OverlayDisconnected;
import dimawo.middleware.distributedAgent.events.ShutdownDA;
import dimawo.middleware.distributedAgent.events.WorkerExit;
import dimawo.middleware.distributedAgent.logging.LoggerInterface;
import dimawo.middleware.fileSystem.FileSystemAgent;
import dimawo.middleware.fileSystem.FileSystemAgentParameters;
import dimawo.middleware.fileSystem.messages.FileSystemMessage;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayException;
import dimawo.middleware.overlay.mntree.MnPeerState;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdateCallBackInterface;




public class DistributedAgent
extends LoggingAgent implements DistributedAgentInterface, MnTreeLocalUpdateCallBackInterface {

	private DAId daId;

	/** Indicates if the DA has received a shutdown request.
	 *  If it is the case, all task related messages are ignored. */
	protected boolean isGoingDown;
	
	protected MasterWorkerFactory tFact;
	private boolean taskWaitStart;

	protected CommunicatorInterface com;
	private OverlayAgentInterface overlay;
	private DiscoveryServiceInterface discServ;

	/** Prefix used to generate unique file names */
	protected String fileNamePref;
	
	protected FileSystemAgent fsPeer;
	
	protected WorkerAgent worker;
	private boolean isLeaderDa;
	private MasterAgent master;
	private LinkedList<MasterMessage> ctrlQueue;
	
	private LoggerInterface logger;


	private static String getFilePrefix(String baseDir, DAId daId) {

		return baseDir + "/" + daId + "_";

	}


	public DistributedAgent(DAId daId,
			String baseDir,
			MasterWorkerFactory tFact,
			DiscoveryServiceInterface discServ,
			LoggerInterface logger,
			FileSystemAgentParameters fsParams)
	throws IOException, DiMaWoException {

		super(null, "DA");
		
		this.daId = daId;

		isGoingDown = false;
		
		this.discServ = discServ;
		this.logger = logger;
		
//		this.sockFact = sockFact;
		fileNamePref = getFilePrefix(baseDir, daId);
		setPrintStream(fileNamePref);

		fsPeer = new FileSystemAgent(this, fsParams);
		this.tFact = tFact;

		ctrlQueue = new LinkedList<MasterMessage>();
	}



	////////////////////
	// Public methods //
	////////////////////
	
	public void setOverlayInterface(OverlayAgentInterface oi) {
		this.overlay = oi;
		com = oi.getCommunicator();
	}
	
	public DAId getDaId() {
		return daId;
	}


	/**
	 * Called by the controller agent hosted by this DA to request the
	 * shutdown of the P2PCL layer.
	 */
	public void signalShutdown() {

		try {
			submitMessage(new ShutdownDA());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	
//	public void submitRequest(LocalTransactionRequest req,
//			RequestInterfaceCallBack sub) throws InterruptedException {
//
//		submitMessage(new SubmitLocalTransactionRequest(false, req, sub));
//
//	}
//	
//	private void handleSubmitLocalTransactionRequest(SubmitLocalTransactionRequest sub) throws Exception {
//		if(sub.isUrgent())
//			reqInt.submitUrgentRequest(sub.getRequest(), sub.getCallBack());
//		else
//			reqInt.submitRequest(sub.getRequest(), sub.getCallBack());
//	}


//	private void handleDAInsertion() {
//		
//		try {
//
//			agentPrintMessage("DA inserted, starting task...");
//
//			task.start();
//
//		} catch (AgentException e) {
//
//			e.printStackTrace();
//
//		}
//		
//	}

//	/**
//	 * Signals to this DA it has been successfully added to the DA tree.
//	 * The Task can be started.
//	 */
//	public void signalDAInsertion() {
//		
//		try {
//			submitMessage(new DAInsertion());
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//	}
	
	
	/**
	 * Signals the end of the task.
	 * 
	 * @throws InterruptedException 
	 * 
	 */
	public void signalWorkerExit(Throwable error, Serializable result)
	throws InterruptedException {

		submitMessage(new WorkerExit(error, result));

	}

	/**
	 * Gives a reference to the Communicator of this DA.
	 * 
	 * @return A reference to the Communicator.
	 */
	public CommunicatorInterface getCommunicator() {
		if(com == null)
			throw new Error("Communicator not set yet");
		return com;
	}
	
	
	/**
	 * Gives a reference to the FileSystemPeer of this DA.
	 * 
	 * @return A reference to the FileSystemPeer.
	 */
	public FileSystemAgent getFileSystemPeer() {

		return fsPeer;

	}
	
	/**
	 * Gives the prefix of the files to be created using this 
	 * 
	 * @return A directory name.
	 */
	public String getFilePrefix() {

		return fileNamePref;

	}
//
//	/**
//	 * Gives the identity data of this DA.
//	 */
//	public DADescriptor getIdData() {
//
//		return idData;
//
//	}


	/**
	 * Gives the host name of the machine running the computing peer.
	 */
	public String getHostName() {
		return daId.getHostName();
	}

	/**
	 * Gives the port used by the computing peer.
	 */
	public int getTcpPort() {
		return daId.getPort();
	}
	

	/**
	 * Submits a message coming from an MessageInputStream to DA.
	 * 
	 * @param msg The received message.
	 * 
	 * @throws InterruptedException
	 */
	public void submitIncomingMessage(Message msg) throws InterruptedException {
		submitMessage(new IncomingMessage(msg));
	}


	/////////////////////////////////////////////
	// AccessorsHandlerCallBack implementation //
	/////////////////////////////////////////////

//	@Override
//	public void connectionError(int daId) {
//
//		assert false : "DA never creates a permanent connection.";
//
//	}
//	
//	@Override
//	public void sendMessageError(int daId, LinkedList<Message> msgs) {
//		
//		agentPrintMessage("Could not send some messages:");
//		Iterator<Message> it = msgs.iterator();
//		while(it.hasNext()) {
//			
//			Message m = it.next();
//			agentPrintMessage(m.toString());
//
//		}
//		
//	}


	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void handleMessage(Object o) {
		
		try {
			if(o instanceof IncomingMessage) { // Message from Communicator
				
				IncomingMessage im = (IncomingMessage) o;

				handleExternalDaMessage(im.getMessage());
				
			} else {

				handleInternalDaMessage(o);

			}
		} catch (Exception e) {
			agentPrintMessage("Error during DA message handling:");
			agentPrintMessage(e);

			if( ! isGoingDown) {

				// Send UncaughtThrowable to Master
				overlay.getLeaderElectionInterface().sendMessageToLeader(new DaError(daId, e));
			}
		}
		
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init.");
		
		if(com == null)
			throw new Error("Communicator is not set");
		
		if(overlay == null)
			throw new Error("Overlay is not set");
		
		logger.setCommunicator(com);
		
		agentPrintMessage("Creating Worker...");
		worker = tFact.getWorkerAgent(this);
		taskWaitStart = true;

		try {
			fsPeer.start();
//			mapAgent.start();
		} catch (Throwable e) {
			agentPrintMessage("Got an uncaught Throwable during init:");
			agentPrintMessage(e);

			if( ! isGoingDown) {
				// Send UncaughtThrowable to Master
				overlay.getLeaderElectionInterface().sendMessageToLeader(new DaError(daId, e));
			}
			
			throw e;
		}

		overlay.registerForMnTreeUpdates(this);
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("Da Closing...");
		closeDa();
		agentPrintMessage("Da Closed.");
	}
	
	@Override
	protected void handleChildException(UncaughtThrowable e) {
		agentPrintMessage("Got an uncaught Throwable from a child:");
		agentPrintMessage(e);

		// Send UncaughtThrowable to Master (to signal error)
		DaError error = new DaError(daId, e);
		overlay.getLeaderElectionInterface().sendMessageToLeader(error);

		// Stop this DA.
		triggerClosing();
	}


	/////////////////////
	// Private methods //
	/////////////////////
	
	protected void triggerClosing() {
		try {
			submitMessage(new ShutdownDA());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	protected void handleSpecializedExternalDaMessage(Message m) throws Exception {
		throw new UnknownAgentMessage(m);
	}
	
	/**
	 * Handles a message coming from Communicator.
	 */
	private final void handleExternalDaMessage(Message m) throws Exception {
		
		if(m instanceof WorkerMessage) {

			handleWorkerMessage((WorkerMessage) m);

		} else if(m instanceof MasterMessage) {

			handleMasterMessage((MasterMessage) m);

		} else if(m instanceof FileSystemMessage) {

			fsPeer.submitFileSystemMessage((FileSystemMessage) m);

//		} else if(m instanceof ShutdownDA) {
//
//			handleShutdownDA((ShutdownDA) m);

//		} else if(m instanceof TransactionResult) {
//
//			reqInt.handleTransactionResult((TransactionResult) m);

		} else {
			
			handleSpecializedExternalDaMessage(m);
			
		}

	}
	

	/**
	 * Handles a shutdown request for this DA.
	 * 
	 * @param m The shutdown message.
	 */
	protected void handleShutdownDA(ShutdownDA m) {

		if(isGoingDown)
			return;
		
		isGoingDown = true;

		agentPrintMessage("Received a Shutdown request.");

		shutdown();
	}


	private void shutdown() {
		stopAgents();
		
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}


	/**
	 * 
	 */
	protected void stopAgents() {

		// Stop task.
		if(worker != null) {

			try {
				agentPrintMessage("Stopping task...");
				worker.stop();
			} catch (InterruptedException e) {
			} catch (AgentException e) {
			}

		}
		
		if(master != null) {
			try {
				agentPrintMessage("Stopping CA...");
				master.stop();
			} catch (InterruptedException e) {
			} catch (AgentException e) {
			}
		}

		// Stop file system peer.
		try {

			agentPrintMessage("Stopping DFS...");
			fsPeer.stop();

		} catch (AgentException e) {
		} catch (InterruptedException e) {
		}

	}
	
	protected void joinAgents() {

		try {
			if(worker != null) {
				worker.join();
				agentPrintMessage("Task stopped.");
			}
			fsPeer.join();
			agentPrintMessage("DFS stopped.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Forwards a message to the task associated to this DA.
	 * 
	 * @param m
	 * @throws DAException
	 * @throws InterruptedException 
	 */
	private void handleWorkerMessage(WorkerMessage m)
	throws DAException, InterruptedException {
		
		// If DA goes down, task is stopping.
		if(isGoingDown)
			return;

		if(worker != null) {

			worker.submitWorkerMessage(m);

		} else {

			agentPrintMessage("No task available for message "+m.getClass());

		}

	}
	
	private void handleMasterMessage(MasterMessage m) {
		// If DA goes down, task is stopping.
		if(isGoingDown)
			return;

		if(master != null) {

			try {
				master.submitMasterMessage(m);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {

			agentPrintMessage("Queuing message for CA ("+m.getClass().getName()+")");
			ctrlQueue.addLast(m);
		}
	}
	
	/**
	 * Handles an internal message (coming from another local component).
	 * 
	 * @param o The message.
	 * 
	 * @throws UnknownAgentMessage
	 */
	private final void handleInternalDaMessage(Object o) throws Exception {

		if(o instanceof ShutdownDA) {

			handleShutdownDA((ShutdownDA) o);

		} else if(o instanceof OverlayDisconnected) {

			handleOverlayDisconnected((OverlayDisconnected) o);

		} else if(o instanceof WorkerExit) {

			handleWorkerExit((WorkerExit) o);

		} else if(o instanceof MasterMessage) {

			handleMasterMessage((MasterMessage) o);

		} else if(o instanceof MnTreeLocalUpdate) {

			handleMnTreeLocalUpdate((MnTreeLocalUpdate) o);

		} else {

			handleSpecializedInternalMessage(o);

		}

	}

	private void handleMnTreeLocalUpdate(MnTreeLocalUpdate o) throws Exception {
//		if(o.getCause().equals(MnTreeLocalUpdate.Cause.join)) {
//			this.log("topology", "join");
//		}
//		
//		if(o.getCause().equals(MnTreeLocalUpdate.Cause.leave)) {
//			this.log("topology", "leave");
//		}
		
		MnPeerState newState = o.getNewState();
		if(newState.isMainPeer(daId) && newState.getParentMnId() == null &&
				! isLeaderDa) {
			isLeaderDa = true;
			
			agentPrintMessage("Starting CA...");
			try {
				master = tFact.getMasterAgent(this);
			} catch (Exception e) {
				agentPrintMessage("Could not instantiate CA.");
				agentPrintMessage(e);
				overlay.shutdownOverlay();
				return;
			}
			if(master == null)
				throw new DiMaWoException("No master instantiated");
			
			for(MasterMessage m : ctrlQueue) {
				master.submitMasterMessage(m);
			}
			ctrlQueue.clear();
			
			master.start();
		}
		
		if(taskWaitStart) {
			worker.start();
			taskWaitStart = false;
		}
	}


	private void handleOverlayDisconnected(OverlayDisconnected o) {
		agentPrintMessage("Overlay disconnected, DA shutdown triggered.");
		shutdown();
	}


	protected void handleSpecializedInternalMessage(Object o) throws Exception {
		throw new UnknownAgentMessage(o);
	}

	protected void onKillDA() {}


	/**
	 * 
	 * @param o
	 * @throws InterruptedException 
	 */
	private void handleWorkerExit(WorkerExit te) throws InterruptedException {

		agentPrintMessage("Worker exited.");
		worker = null;

		if( ! isGoingDown) {

			WorkerExitMessage exit = new WorkerExitMessage(daId,
					te.getError(), te.getResult());
			overlay.getLeaderElectionInterface().sendMessageToLeader(exit);
		
		}

	}

	/**
	 * Executes the clean-up operations before exit.
	 * 
	 */
	protected void closeDa() {
		stopAgents();

		// Join child agents
		if(worker != null) {

			agentPrintMessage("Waiting end of task...");
			try {
				worker.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		
		if(master != null) {

			agentPrintMessage("Waiting end of CA...");
			try {
				master.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		try {
			
			agentPrintMessage("Waiting end of FS Peer...");
			fsPeer.join();

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		agentPrintMessage("Leaving overlay...");
		try {
			overlay.leaveOverlay();
		} catch (OverlayException e) {
			e.printStackTrace();
		}
		
		agentPrintMessage("DA "+daId+" closed");
	}

//	public SocketFactory getSocketFactory() {
//		
//		return sockFact;
//		
//	}
	
//	public void broadcastMessage(Message msg) {
//		try {
//			submitMessage(new BroadcastRequest(msg));
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
	
	public OverlayAgentInterface getOverlayInterface() {
		return overlay;
	}

	/**
	 * Recipient field of datagram is ignored.
	 * @param msg
	 */
//	protected abstract void handleBroadcastRequest(BroadcastRequest o) throws Exception;
//	public abstract void newDa(NewDA newDA) throws InterruptedException;
//	public abstract void submitTransactionRequest(TransactionRequest tr);
//	protected abstract void shutdownComLayer();

	public synchronized void printMessage(String string) {
		agentPrintMessage(string);
	}


	public void killDA() {
		try {
			stop();
		} catch (InterruptedException e1) {
		} catch (AgentException e1) {
		}

		try {
			fsPeer.stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
		if(worker != null)
			try {
				worker.stop();
			} catch (InterruptedException e) {
			} catch (AgentException e) {
			}
		
		onKillDA();
	}

	public void submitMasterMessage(MasterMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public DiscoveryServiceInterface getDiscoveryService() {
		return discServ;
	}

	public void signalOverlayDisconnected() {
		try {
			submitMessage(new OverlayDisconnected());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void signalMnTreeLocalUpdate(MnTreeLocalUpdate update) {
		try {
			submitMessage(update);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Override
	public synchronized void log(String id, String msg) {
//		logger.log(id, msg);
	}
}
