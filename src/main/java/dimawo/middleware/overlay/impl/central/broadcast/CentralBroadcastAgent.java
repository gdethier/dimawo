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

import java.util.TreeMap;

import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.distributedAgent.messages.BroadcastRequestMessage;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.BroadcastingAgentMessage;
import dimawo.middleware.overlay.impl.central.CentralOverlay;




public class CentralBroadcastAgent extends LoggingAgent implements BroadcastingAgentInterface {
	
	private DistributedAgent da;
	private DAId ctrlId;
	private CommunicatorInterface com;
	
	private int nextBroadNum;
	
	private class WaitingBroadcast {
		public Message msg;
		public MOSCallBack cb;
		
		public WaitingBroadcast(Message msg, MOSCallBack cb) {
			this.msg = msg;
			this.cb = cb;
		}
	}
	private TreeMap<Integer, WaitingBroadcast> waitingBroadcast;
	
	public CentralBroadcastAgent(ErrorHandler over, DistributedAgent da) {
		super(over, "CentralBroadcastAgent");
		this.da = da;
		
		setPrintStream(da.getFilePrefix());
		
		waitingBroadcast = new TreeMap<Integer, WaitingBroadcast>();
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
		
		com = da.getCommunicator();
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof LocalBroadcastRequest) {
			handleLocalBroadcastRequest((LocalBroadcastRequest) o);
		} else if(o instanceof BroadcastRequestAckMessage) {
			handleBroadcastRequestAckMessage((BroadcastRequestAckMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleLocalBroadcastRequest(LocalBroadcastRequest req) {
		agentPrintMessage("Sending broadcast request to controller.");
		
		Message msg = req.getMessage();
		MOSCallBack cb = msg.getCallBack();
		int broadNum = -1;
		if(cb != null) {
			broadNum = nextBroadNum;
			waitingBroadcast.put(nextBroadNum, new WaitingBroadcast(msg, cb));
			++nextBroadNum;
		}
		
		BroadcastRequestMessage brm = new BroadcastRequestMessage(broadNum,
				msg);
		brm.setRecipient(ctrlId);
		com.sendDatagramMessage(brm);
	}
	
	private void handleBroadcastRequestAckMessage(BroadcastRequestAckMessage m) throws Exception {
		int broadNum = m.getBroadcastNumber();
		WaitingBroadcast wb = waitingBroadcast.remove(broadNum);
		if(wb == null)
			throw new Exception("No waiting broadcast "+broadNum);
		agentPrintMessage("Signaling broadcast "+broadNum);
		wb.cb.signalSent(wb.msg, true);
	}

	@Override
	public void submitBroadcastingAgentMessage(BroadcastingAgentMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}

	public void setLeaderId(DAId ctrlId) {
		this.ctrlId = ctrlId;
	}

}
