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
package dimawo;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UncaughtThrowable;
import dimawo.master.events.MessageSent;
import dimawo.master.messages.DaError;
import dimawo.master.messages.MasterMessage;
import dimawo.master.messages.TopologyUpdateMessage;
import dimawo.master.messages.WorkerExitMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.OverlayException;


/**
 * This class represents the Master Agent (MA). It implements the master
 * process of a master/worker application. The Master Agent is instantiated
 * by one, and only one, Distributed Agent (DA) part of the group of DAs
 * implementing the same DiMaWo application (note that each DA is generally
 * executed in a different process, each process being executed by a different
 * computer). This potentially implies the solving of leader election problem
 * in order to select the DA that will instantiate the MA.
 * <p>
 * The base MA provides common functionalities but requires some key elements
 * of its behavior to be implemented. In general, MA's behavior is more or
 * less as follows:
 * <ol>
 * <li>the MA temporizes in order to wait for all workers to be instantiated
 * or waits for a given number of workers to be up. The instantiation of new
 * workers is signaled through topology changes ("joins", see
 * {@link #onTopologyChange(DAId, ChangeType)}).</li>
 * <li>Initial data are provided to each worker through datagram messages
 * or a broadcast.</li>
 * <li>The MA waits for all workers to finish their execution. It may
 * have to control workers execution or centralize informations (beware of
 * a bottleneck!).</li>
 * <li>After all workers have finished their execution, the MA ends the
 * application by calling {@link #shutdown()}.</li>
 * </ol> 
 * <p>
 * The common functionalities provided by the base implementation are:
 * <ul>
 * <li>broadcasting of messages to all workers (see {@link #broadcastWorkerMessage(WorkerMessage, WorkerMessage)})</li>
 * <li>a shortcut to send datagram messages to workers (see {@link #sendMessage(WorkerMessage)})</li>
 * <li>the ability to shutdown the whole application i.e. terminate all
 * processes running workers and the master itself (see {@link #shutdown()}).
 * The shutdown generally occurs after the detection of a fatal error or
 * when the application successfully reached the end of its execution.</li>
 * </ul>
 * <p>
 * Following elements of the behavior of the MA must be implemented by the user:
 * <ul>
 * <li>handling of user defined events and messages (see
 * {@link #handleMasterEvent(Object)} and {@link #handleUserDefinedAgentMessage(MasterMessage)}).</li>
 * <li>The behavior in case a worker ends its execution
 * (see {@link #handleWorkerExit(WorkerExitMessage)}).</li>
 * <li>handling of topology changes (see {@link #onTopologyChange(DAId, ChangeType)}).</li>
 * <li>initialization of master's state and final operations executed before
 * master ends its execution (see {@link #onStartup()} and {@link
 * #onExit()} respectively).</li>
 * </ul>
 * <p>
 * The master implements following agent's behavior:
 * <ul>
 * <li>In case an uncaught exception is submitted by a child agent, the whole
 * application is interrupted.</li>
 * <li>On agent's initialization, a message is logged and user's code is called.</li>
 * <li>On agent's exit, user's code is called and a message is logged.</li>
 * </ul>
 * 
 * @author Gerard Dethier
 * 
 * @see dimawo.middleware.distributedAgent.DistributedAgent
 * @see be.ulg.montefiore.dimawo.middleware.WorkerAgent
 */
public abstract class MasterAgent
extends LoggingAgent
implements MOSCallBack {
	/**
	 * This enumeration provides the types of topology changes.
	 * 
	 * @author Gerard Dethier
	 */
	protected enum ChangeType {
		/**
		 * Represents the fact that a worker left the workers group.
		 * This generally happens when the process executing the worker
		 * is interrupted, for example when the computer running it crashes.
		 */
		leave,
		/**
		 * Represents the fact that a new worker joined the workers group.
		 */
		join};

	/**
	 * The DA hosting the MA.
	 */
	private DistributedAgent hostingDa;
	/**
	 * A flag indicating if the application is already terminating. If
	 * <code>isGoingDown</code> is true, the application is terminating.
	 */
	private boolean isGoingDown;
	/**
	 * A reference to the communicator associated to the DA hosting the master.
	 * 
	 * @see dimawo.middleware.communication.Communicator
	 */
	private CommunicatorInterface com;


	/**
	 * Instantiates the Master Agent (MA) giving it a name and associating it to a
	 * DA. 
	 * 
	 * @param hostingDa The DA that instantiates the MA.
	 * @param name The name of the MA.
	 */
	public MasterAgent(DistributedAgent hostingDa, String name) {
		super(hostingDa, name);

		this.hostingDa = hostingDa;
		isGoingDown = false;
		com = hostingDa.getCommunicator();

		setPrintStream(hostingDa.getFilePrefix());
	}

	
	/**
	 * Returns the DA that instantiated the MA.
	 * 
	 * @return The DA that instantiated the MA.
	 */
	public DistributedAgent getHostingDA() {
		return hostingDa;
	}
	
	
	/**
	 * Inserts a <code>MasterMessage</code> message in MA's message queue.
	 * 
	 * @throws InterruptedException Thrown if executing thread is interrupted
	 * during a blocking insertion (happens when message queue is full). 
	 */
	public void submitMasterMessage(MasterMessage m)
	throws InterruptedException {
		submitMessage(m);
	}


	/**
	 * Returns the working directory of hosting DA.
	 * 
	 * @return The working directory of hosting DA.
	 */
	public String getWorkingDir() {
		return hostingDa.getFilePrefix();
	}


	@Override
	protected void logAgentExit() {
		onExit();
		agentPrintMessage("exit");
	}


	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		onStartup();
	}


	@Override
	protected void handleMessage(Object o) {
		try {
			if(o instanceof MessageSent) {
				handleMessageSent((MessageSent) o);
			} else {
				if(isGoingDown) {
					agentPrintMessage("Ignored "+o.getClass().getSimpleName()+" while closing.");
					return;
				}

				if(o instanceof MasterMessage) {
					handleMasterMessage((MasterMessage) o);
				} else if(o instanceof MessageSent) {
					handleMessageSent((MessageSent) o);
				} else {
					handleMasterEvent(o);
				}
			}
		} catch (Throwable t) {
			agentPrintMessage("Error during message handling:");
			agentPrintMessage(t);

			shutdown();
		}
	}


	/**
	 * Causes the whole application to terminate its execution, potentially
	 * before completion.
	 */
	@Override
	protected void handleChildException(UncaughtThrowable o)
	throws AgentException {
		agentPrintMessage("Received following message from a child:");
		agentPrintMessage(o);
		
		shutdown();
	}


	/**
	 * Handles a "message sent" event. This kind of event is submitted to
	 * MA if a subclass of <code>MasterAgent</code> sends a message with
	 * the callback set (see
	 * {@link dimawo.middleware.communication.Message Message} class).
	 * 
	 * @param o The "message sent" event.
	 * 
	 * @throws Exception If an error occurred during event's handling.
	 */
	private void handleMessageSent(MessageSent o) throws Exception {
		handleUserDefinedMessageSent(o.getMessage());
	}


	/**
	 * Handles a received <code>MasterMessage</code>.
	 * 
	 * @param msg A MasterMessage sent by a DA.
	 * 
	 * @throws Exception If an error occurred during message's handling. 
	 * 
	 */
	private void handleMasterMessage(MasterMessage msg)
	throws Exception {
		if(msg instanceof DaError) {
			handleDaError((DaError) msg);
		} else if(msg instanceof WorkerExitMessage) {
			handleWorkerExit((WorkerExitMessage) msg);
		} else if(msg instanceof TopologyUpdateMessage) {
			handleTopologyUpdateMessage((TopologyUpdateMessage) msg);
		} else {
			handleUserDefinedAgentMessage(msg);
		}
	}


	/**
	 * Handles a received topology update message. This type of message is sent
	 * by a worker in order to signal a local topology change.
	 * 
	 * @param msg A topology update message.
	 * 
	 * @throws Exception If an error occured during topology change handling.
	 */
	private void handleTopologyUpdateMessage(TopologyUpdateMessage msg) throws Exception {
		agentPrintMessage("Topology change detected.");
		
		if(msg.isJoin()) {
			onTopologyChange(msg.getDaId(), ChangeType.join);
		} else if(msg.isLeave()) {
			onTopologyChange(msg.getDaId(), ChangeType.leave);
		} else {
			throw new Error("Unhandled topology change type");
		}
	}


	/**
	 * Terminates the execution of the application. All processes executing a
	 * worker will eventually terminate their execution.
	 */
	protected void shutdown() {
		if( ! isGoingDown) {

			isGoingDown = true;
			
			agentPrintMessage("Master shutdown.");
			try {
				hostingDa.getOverlayInterface().shutdownOverlay();
			} catch (OverlayException e1) {
				e1.printStackTrace();
			}

			try {
				stop();
			} catch (InterruptedException e) {
			} catch (AgentException e) {
			}
		}
	}

	
	/**
	 * Sends a message to a worker agent.
	 * 
	 * @param msg The message to send to the worker agent.
	 */
	protected void sendMessage(WorkerMessage msg) {
		com.sendDatagramMessage(msg);
	}
	
	
	@Override
	public void signalSent(Message m, boolean success) {
		try {
			submitMessage(new MessageSent(m, success));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void signalBroken(BrokenDA bda) {
		// Ignore, topology change will be detected and signaled by a worker
	}
	
	/**
	 * Broadcasts a message to all workers. Two copies of the message to send
	 * must be provided: one for local worker (i.e. the worker instantiated by
	 * the same DA as master) and one for remote workers. This is required
	 * because the message for local worker is transmitted through DA and
	 * not through the Communicator like remote workers' message.
	 * 
	 * @param toLocal The message copy to transmit to local worker.
	 * @param toRemote The message copy to transmit to remote workers.
	 */
	protected void broadcastWorkerMessage(WorkerMessage toLocal, WorkerMessage toRemote) {
		BroadcastingAgentInterface broad = hostingDa.getOverlayInterface().getBroadcastInterface();
		broad.broadcastMessage(toRemote);
		toLocal.setSender(hostingDa.getDaId());
		try {
			hostingDa.submitIncomingMessage(toLocal);
		} catch(InterruptedException e) {
			throw new Error("unexpected exception");
		}
	}
	
	
	/**
	 * Handles an error message sent by a DA. This kind of message is sent by
	 * a DA when it is about to terminate its execution because of a fatal
	 * error. The reception of this message causes the master to terminate
	 * the execution of the whole application.
	 * 
	 * @param msg The error message.
	 */
	protected void handleDaError(DaError msg) {
		agentPrintMessage("Got following error from DA "+msg.getSourceDaId());
		agentPrintMessage(msg.getError());
		shutdown();
	}
	
	/**
	 * Handles a "message sent" event for a user defined sent message.
	 * 
	 * @param o The "message sent" event.
	 * 
	 * @throws Exception If an error occurred during event's handling.
	 * 
	 * @see #handleMessageSent(MessageSent)
	 */
	protected void handleUserDefinedMessageSent(Message msg) throws Exception {}


	/**
	 * Handles a received message that is not handled by base implementation.
	 * A message that is not handled by base implementation is either a user
	 * defined message or an unexpected message (in this case, an exception
	 * is thrown).
	 * 
	 * @param msg The message to handle.
	 * 
	 * @throws Exception If an error occurred during message handling.
	 */
	protected abstract void
	handleUserDefinedAgentMessage(MasterMessage msg)
	throws Exception;
	
	
	/**
	 * Handles a message indicating the end of the execution of a worker.
	 * This kind of message is sent by a
	 * DA to signal the worker it instantiated has terminated its execution.
	 * The message contains the result the worker may have produced.
	 * The DA continues its execution until the whole application is
	 * terminated.
	 * 
	 * @param msg The message indicating the end of the execution of a worker.
	 * 
	 * @throws Exception If an error occurred during the handling of the message.
	 * 
	 * @see WorkerExitMessage#getResult()
	 */
	protected abstract void handleWorkerExit(WorkerExitMessage msg) throws Exception;
	
	
	/**
	 * Called when Master Agent terminates its execution. This method should
	 * essentially contain "clean-up" operations.
	 */
	protected abstract void onExit();
	
	
	/**
	 * Called when Master Agent (MA) is initialized i.e. directly after the
	 * agent is started and before any message is handled. 
	 * 
	 * @throws Throwable If an error occurs during MA's initialization.
	 */
	protected abstract void onStartup() throws Throwable;
	
	
	/**
	 * Called when the Master Agent (MA) handles a message signaling a local
	 * change in DA's topology (i.e. a new DA joined or a DA left the overlay).
	 * 
	 * @param subject The DA that joined or left the overlay.
	 * @param type The type of topology change (join, leave).
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	protected abstract void onTopologyChange(DAId subject, ChangeType type) throws Exception;
	
	
	/**
	 * Handles a user defined or unknown master event.
	 * 
	 * @param o The event.
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	protected abstract void handleMasterEvent(Object o) throws Exception;
}
