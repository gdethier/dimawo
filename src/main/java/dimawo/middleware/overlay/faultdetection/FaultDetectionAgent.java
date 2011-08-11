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
package dimawo.middleware.overlay.faultdetection;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.faultdetection.events.RegisterCB;
import dimawo.middleware.overlay.faultdetection.events.UnregisterCB;
import dimawo.middleware.overlay.faultdetection.messages.PingMessage;



public class FaultDetectionAgent extends LoggingAgent implements MOSCallBack, MessageHandler {
	private static final String FAULT_DETECTION_HANDLER_ID = "FaultDetectionServiceMsg";
	private CommunicatorInterface com;
	
	private long pingPeriod;
	private long timeout;

	private TreeMap<DAId, TargetInfo> toPing;
	private Timer pingTimer;

	public FaultDetectionAgent(ErrorHandler parent, CommunicatorInterface com,
			long pingPeriod, long timeout) {
		super(com, "PingServiceAgent");
		this.com = com;
		com.registerMessageHandler(FAULT_DETECTION_HANDLER_ID, this);
		
		this.pingPeriod = pingPeriod;
		this.timeout = timeout;
		
		toPing = new TreeMap<DAId, TargetInfo>();
	}
	
	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		pingTimer = new Timer(true);
		pingTimer.schedule(new TimerTask() {
			public void run() {
				try {
					submitMessage(new TriggerPing());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, pingPeriod, pingPeriod);
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof TriggerPing) {
			handleTriggerPing();
		} else if(o instanceof BrokenDA) {
			handleBrokenDA((BrokenDA) o);
		} else if(o instanceof RegisterCB) {
			handleRegisterCB((RegisterCB) o);
		} else if(o instanceof UnregisterCB) {
			handleUnregisterCB((UnregisterCB) o);
		} else if(o instanceof PingMessage) {
			handlePingMessage((PingMessage) o);
		} else if(o instanceof FaultDetectionAckMessage) {
			handlePingAckMessage((FaultDetectionAckMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handlePingAckMessage(FaultDetectionAckMessage o) {
		DAId target = o.getSender();
		TargetInfo inf = toPing.get(target);
		if(inf != null) {
			inf.ackLastPing();
		}
	}

	private void handlePingMessage(PingMessage o) {
		com.sendDatagramMessage(new FaultDetectionAckMessage(o.getSender()));
	}

	private void handleUnregisterCB(UnregisterCB o) {
		DAId target = o.getTarget();
		FaultDetectionServiceCallBackInterface cb = o.getCallBack();
		
		agentPrintMessage("Unregistered for "+target+": "+cb.getClass().getName());
		
		TargetInfo inf = toPing.get(target);
		if(inf != null) {
			inf.unregister(cb);
			if(inf.noCbRegistered())
				toPing.remove(target);
		}
	}

	private void handleRegisterCB(RegisterCB o) {
		DAId target = o.getTarget();
		FaultDetectionServiceCallBackInterface cb = o.getCallBack();
		
		agentPrintMessage("Registered for "+target+": "+cb.getClass().getName());
		
		TargetInfo inf = toPing.get(target);
		if(inf == null) {
			inf = new TargetInfo(target);
			toPing.put(target, inf);
		}
		
		inf.addCB(cb);
	}

	private void handleBrokenDA(BrokenDA o) {
		agentPrintMessage("Failure detected for "+o.getDAId());
		TargetInfo inf = toPing.remove(o.getDAId());
		inf.signalFailure();
	}

	private void handleTriggerPing() {
		for(Iterator<Entry<DAId, TargetInfo>> it = toPing.entrySet().iterator();
		it.hasNext();) {
			Entry<DAId, TargetInfo> e = it.next();
			DAId dest = e.getKey();
			TargetInfo inf = e.getValue();
			
			if(inf.isLastPingAcked()) {
				PingMessage pm = new PingMessage(dest);
				pm.setCallBack(this);
				com.sendDatagramMessage(pm);
				inf.ping();
			} else if(inf.hasTimedOut(timeout)) {
				agentPrintMessage(dest+" has timed out");
				inf.signalTimeout();
				it.remove();
			}
		}
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
		pingTimer.cancel();
	}

	@Override
	public void signalBroken(BrokenDA bda) {
		try {
			submitMessage(bda);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success)
			signalBroken(new BrokenDA(m.getRecipient()));
	}

	public void register(DAId target, FaultDetectionServiceCallBackInterface cb) {
		try {
			submitMessage(new RegisterCB(target, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void unregister(DAId target, FaultDetectionServiceCallBackInterface cb) {
		try {
			submitMessage(new UnregisterCB(target, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void submitIncomingMessage(Message msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
