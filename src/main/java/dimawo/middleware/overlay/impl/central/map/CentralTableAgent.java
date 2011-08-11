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
package dimawo.middleware.overlay.impl.central.map;

import java.util.LinkedList;

import dimawo.agents.UncaughtThrowable;
import dimawo.agents.UnknownAgentMessage;
import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.SharedMapGetResult;
import dimawo.middleware.overlay.SharedMapPutResult;
import dimawo.middleware.overlay.SharedMapRemoveResult;
import dimawo.middleware.overlay.SharedMapUpdateResult;
import dimawo.middleware.overlay.impl.central.CentralOverlay;
import dimawo.middleware.sharedMap.AbstractSharedMapAgent;
import dimawo.middleware.sharedMap.events.GetValue;
import dimawo.middleware.sharedMap.events.PutValue;
import dimawo.middleware.sharedMap.events.RemoveValue;
import dimawo.middleware.sharedMap.events.UpdateValue;



public class CentralTableAgent extends AbstractSharedMapAgent {
	
	private DistributedAgent da;
	
	private LinkedList<AsynchronousCall> pendingCalls;
	private AsynchronousCall currentCall;


	public CentralTableAgent(CentralOverlay over, DistributedAgent da) {
		super(over, "CentralTableAgent");
		this.da = da;

		pendingCalls = new LinkedList<AsynchronousCall>();
	}
	
	private void handleCall(AsynchronousCall c) throws InterruptedException {
		if(currentCall != null) {
			agentPrintMessage("Queuing call");
			pendingCalls.addLast(c);
		} else {
			agentPrintMessage("Sending call to central controller");
			currentCall = c;
			da.getOverlayInterface().getLeaderElectionInterface().sendMessageToLeader(new CentralTableCallMessage(da.getDaId(), c));
		}
	}

	@Override
	protected void handleGetValue(GetValue o) throws Exception {
		handleCall(o);
	}

	@Override
	protected void handlePutValue(PutValue o) throws Exception {
		handleCall(o);
	}

	@Override
	protected void handleRemoveValue(RemoveValue o) throws Exception {
		handleCall(o);
	}

	@Override
	protected void handleUpdateValue(UpdateValue o) throws Exception {
		handleCall(o);
	}
	
	private void handleCentralTableCallBackMessage(CentralTableCallBackMessage o) throws InterruptedException {
		agentPrintMessage("Received call-back from central controller");

		handleCallBack(o.getCall());

		if(pendingCalls.size() > 0) {
			currentCall = pendingCalls.removeFirst();
			da.getOverlayInterface().getLeaderElectionInterface().sendMessageToLeader(new CentralTableCallMessage(da.getDaId(), currentCall));
		} else {
			currentCall = null;
		}
	}

	private void handleCallBack(AsynchronousCall o) {
		if(o instanceof PutValue) {
			handlePutResult((PutValue) o);
		} else if(o instanceof GetValue) {
			handleGetResult((GetValue) o);
		} else if(o instanceof UpdateValue) {
			handleUpdateResult((UpdateValue) o);
		} else if(o instanceof RemoveValue) {
			handleRemoveResult((RemoveValue) o);
		} else {
			o.setError(new UnknownAgentMessage(o));
		}
	}

	private void handleRemoveResult(RemoveValue o) {
		RemoveValue rv = (RemoveValue) currentCall;
		
		currentCall.signalSuccess();
		SharedMapCallBackInterface cb = rv.getCallBack();
		if(cb != null)
			cb.sharedMapRemoveCallBack(new SharedMapRemoveResult(rv.getKey()));
	}

	private void handleUpdateResult(UpdateValue o) {
		UpdateValue rv = (UpdateValue) currentCall;
		
		currentCall.signalSuccess();
		SharedMapCallBackInterface cb = rv.getCallBack();
		if(cb != null)
			cb.sharedMapUpdateCallBack(new SharedMapUpdateResult(rv.getKey(), rv.getUpdateData(), rv.getEntry()));
	}

	private void handleGetResult(GetValue o) {
		GetValue rv = (GetValue) currentCall;

		SharedMapEntry res = o.getResult();
		rv.setResult(res);
		rv.signalSuccess();
		SharedMapCallBackInterface cb = rv.getCallBack();
		if(cb != null)
			cb.sharedMapGetCallBack(new SharedMapGetResult(rv.getKey(), res));
	}

	private void handlePutResult(PutValue o) {
		PutValue rv = (PutValue) currentCall;
		
		currentCall.signalSuccess();
		SharedMapCallBackInterface cb = rv.getCallBack();
		if(cb != null)
			cb.sharedMapPutCallBack(new SharedMapPutResult(rv.getEntry()));
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void init() throws Throwable {
		setPrintStream(da.getFilePrefix());
		agentPrintMessage("init");
	}

	@Override
	protected void handleSpecificMessage(Object o) throws Exception {
		if(o instanceof CentralTableCallBackMessage) {
			handleCentralTableCallBackMessage((CentralTableCallBackMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

}
