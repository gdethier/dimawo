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

import java.io.Serializable;

import dimawo.agents.LoggingAgent;
import dimawo.master.messages.TopologyUpdateMessage;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.LeaderElectionInterface;
import dimawo.middleware.overlay.mntree.MnPeerState;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdateCallBackInterface;



/**
 * This class represents the Worker Agent (WA). It implements the worker process
 * of a master/worker application. One WA is instantiated by each DA part of the
 * group of DAs executing a DiMaWo application (note that each DA is generally
 * executed in a different process, each process being executed by a different
 * computer).
 * <p>
 * In principle, one WA is instantiated per computer. Therefore, if a computer
 * running a WA has several cores or processors, the
 * implementation of the WA should be multi-threaded, the number of
 * instantiated threads being dependent of the number of cores/processors the
 * computer has. Another possibility is to instantiate as much WAs as
 * processors/cores. However, DiMaWo was not designed this way.
 * <p>
 * The base implementation of the WA provides a default agent behavior but
 * requires some additional implementation.
 * <p>
 * The base WA automatically signals the end of its execution to the DA that
 * instantiated it (see {@link #logAgentExit()}). It also signals a local
 * change of topology to the master (see {@link #handleMnTreeLocalUpdate(MnTreeLocalUpdate)}).
 * However, additional behavior must be defined:
 * <ul>
 * <li>for handling of user-defined events (see {@link #handleWorkerEvent(Object)})
 * and messages (see {@link #handleWorkerMessage(WorkerMessage)})</li>
 * <li>to handle local topology changes (other than signal change to master,
 * see {@link #onLocalTopologyChange(MnPeerState)})</li>
 * <li>clean-up or result gathering operations at the end of the execution
 * of the worker (see {@link #preWorkerExit()}).</li>
 * </ul>
 * 
 * @author Gerard Dethier
 * 
 * @see dimawo.middleware.distributedAgent.DistributedAgent
 * @see be.ulg.montefiore.dimawo.middleware.MasterAgent
 */
public abstract class WorkerAgent extends LoggingAgent implements MnTreeLocalUpdateCallBackInterface {

	/**
	 * The Distributed Agent that instantiated the Worker Agent.
	 */
	private DistributedAgent da;

	/**
	 * Instantiates a Worker Agent (WA).
	 * 
	 * @param da The Distributed Agent that instantiates this WA.
	 * @param name The name of this WA. The name of the WA is used for logging.
	 * 
	 * @see LoggingAgent
	 */
	public WorkerAgent(DistributedAgent da, String name) {
		super(da, name);

		this.da = da;
		setPrintStream(da.getFilePrefix());
		da.getOverlayInterface().registerForMnTreeUpdates(this);
	}
	
	
	/**
	 * @return The DA hosting this task.
	 */
	public DistributedAgent getDistributedAgent() {
		return da;
	}
	
	
	/**
	 * Inserts a <code>WorkerMessage</code> into Worker Agent's message queue.
	 * 
	 * @param m A WorkerMessage.
	 */
	public void submitWorkerMessage(WorkerMessage m) {
		try {
			submitMessage(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	final protected void logAgentExit() {
		Serializable s = preWorkerExit();
		try {
			da.signalWorkerExit(null, s);
		} catch (InterruptedException e) {
			assert false;
		}
	}

	@Override
	protected void handleMessage(Object o)  throws Throwable {
		if(o instanceof WorkerMessage) {
			handleWorkerMessage((WorkerMessage) o);
		} else if(o instanceof MnTreeLocalUpdate) {
			handleMnTreeLocalUpdate((MnTreeLocalUpdate) o);
		} else if(o instanceof DiMaWoException) {
			throw (DiMaWoException) o;
		} else {
			handleWorkerEvent(o);
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

	/**
	 * Handles a local topology change event taken from message queue. If this
	 * Worker Agent is "responsible" of the local area, it forwards the signal
	 * to the Master Agent.
	 * 
	 * @param event The local topology change event.
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	private void handleMnTreeLocalUpdate(MnTreeLocalUpdate event) throws Exception {
		agentPrintMessage("Toplogy update detected.");

		MnPeerState state = event.getNewState();
		onLocalTopologyChange(state); // specific handling of local change
		
		/* Check if the local update should be forwarded to master by this
		 * worker */
		MnTreeLocalUpdate.Cause cause = event.getCause();
		MnTreeLocalUpdate.Target targ = event.getTarget();
		if(state.isMainPeer(da.getDaId()) &&
			(cause.equals(MnTreeLocalUpdate.Cause.init) ||
			((cause.equals(MnTreeLocalUpdate.Cause.join) ||
					cause.equals(MnTreeLocalUpdate.Cause.leave)) &&
			targ.equals(MnTreeLocalUpdate.Target.thisMn)))) {
			agentPrintMessage("-- sending notification to leader.");
			LeaderElectionInterface lead = da.getOverlayInterface().getLeaderElectionInterface();
			lead.sendMessageToLeader(new TopologyUpdateMessage(da.getDaId(),
				event.getSubject(), cause.equals(MnTreeLocalUpdate.Cause.leave)));
		}
	}
	
	/**
	 * Called by local topology change handling method. The user must implement
	 * this method and possibly implement a specific handling code. The specific
	 * code may simply do nothing (empty method).
	 * 
	 * @param newState The new local topology.
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	protected abstract void onLocalTopologyChange(MnPeerState newState) throws Exception;


	/**
	 * Handles an event taken from messages queue.
	 * 
	 * @param o The event.
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	protected abstract void handleWorkerEvent(Object o) throws Exception;


	/**
	 * Handles a <code>WorkerMessage</code> taken from messages queue.
	 * 
	 * @param o The <code>WorkerMessage</code>.
	 * 
	 * @throws Exception If an error occurred during event handling.
	 */
	protected abstract void handleWorkerMessage(WorkerMessage o) throws Exception;
	
	
	/**
	 * Executes user code when the Worker Agent (WA) terminates its execution
	 * and provides the result produced by this WA. The returned result is
	 * forwarded to MA.
	 * 
	 * @see dimawo.MasterAgent#handleWorkerExit(dimawo.master.messages.WorkerExitMessage)
	 */
	protected abstract Serializable preWorkerExit();
}
