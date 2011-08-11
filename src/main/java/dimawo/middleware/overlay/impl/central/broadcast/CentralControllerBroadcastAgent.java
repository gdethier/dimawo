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
package dimawo.middleware.overlay.impl.central.broadcast;

import java.util.TreeSet;

import dimawo.WorkerMessage;
import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.messages.BroadcastRequestMessage;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.BroadcastingAgentMessage;
import dimawo.middleware.overlay.impl.central.CentralOverlay;




public class CentralControllerBroadcastAgent
extends LoggingAgent
implements BroadcastingAgentInterface {
	
	private DistributedAgent ctrl;
	private TreeSet<DAId> das;
	
	private WaitingDABroadcasts waitingDABroadcasts;
	
	private CommunicatorInterface com;

	public CentralControllerBroadcastAgent(ErrorHandler over,
			DistributedAgent ctrl, Communicator com) {
		super(over, "CentralControllerBroadcastAgent");
		this.ctrl = ctrl;
		this.com = com;
		
		setPrintStream(ctrl.getFilePrefix());
		
		das = new TreeSet<DAId>();
		
		waitingDABroadcasts = new WaitingDABroadcasts(com);
	}

	@Override
	public void broadcastMessage(Message msg) {
		try {
			submitMessage(new LocalBroadcastRequest(msg));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		
		com = ctrl.getCommunicator();
		waitingDABroadcasts = new WaitingDABroadcasts(com);
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof LocalBroadcastRequest) {
			handleLocalBroadcastRequest((LocalBroadcastRequest) o);
		} else if(o instanceof BroadcastRequestMessage) {
			handleBroadcastRequestMessage((BroadcastRequestMessage) o);
		} else if(o instanceof DAId) {
			handleDAId((DAId) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleBroadcastRequestMessage(BroadcastRequestMessage m) {
		agentPrintMessage("Received broadcast request from "+m.getSender());
		
		Message msg = m.getMessage();
		int daBroadNum = m.getDABroadcastNumber();
		DAId sourceDa = m.getSender();
		if(daBroadNum != -1) {
			waitingDABroadcasts.add(sourceDa, daBroadNum, msg);
			msg.setCallBack(waitingDABroadcasts);
		}
		
		// Submit to hosting DA
		try {
			if(! (msg instanceof WorkerMessage))
				ctrl.submitIncomingMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Broadcast to all DAs
		handleLocalBroadcastRequest(new LocalBroadcastRequest(msg));
	}

	private void handleLocalBroadcastRequest(LocalBroadcastRequest o) {
		DAId[] daIds = new DAId[das.size()];
		das.toArray(daIds);

		Message msg = o.getMessage();
		if(daIds.length > 0) {
			agentPrintMessage("Broadcasting "+msg.getClass().getName()+" to "+daIds.length+" DAs.");
			com.multicastMessage(daIds, msg);
		} else {
			agentPrintMessage("No DA to broadcast message "+msg.getClass().getName()+" to.");
			msg.setMessageSent(true);
		}
	}

	@Override
	public void submitBroadcastingAgentMessage(BroadcastingAgentMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void submitNewDA(DAId id) {
		try {
			submitMessage(id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handleDAId(DAId o) {
		agentPrintMessage("New DA registered: "+o);
		if(! das.add(o)) {
			throw new Error("DA already registered: "+o);
		}
	}

}
