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

import dimawo.agents.UnknownAgentMessage;
import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.SharedMapAgentInterface;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
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

public class CentralControllerMapAgent extends AbstractSharedMapAgent implements
		SharedMapAgentInterface {
	
	private DistributedAgent ctrl;
	private CentralTable table;

	public CentralControllerMapAgent(CentralOverlay over, DistributedAgent ctrl) {
		super(over, "CentralControllerMapAgent");
		this.ctrl = ctrl;
		table = new CentralTable();
	}
	
	@Override
	protected void handleGetValue(GetValue o) throws Exception {
		table.handleCall(o);
		
		agentPrintMessage("Getting key "+o.getKey());
		
		o.signalSuccess();
		SharedMapCallBackInterface cb = o.getCallBack();
		if(cb != null)
			cb.sharedMapGetCallBack(new SharedMapGetResult(o.getKey(), o.getResult()));
	}

	@Override
	protected void handlePutValue(PutValue o) throws Exception {
		table.handleCall(o);
		
		agentPrintMessage("Putting key "+o.getEntry().getKey());

		o.signalSuccess();
		SharedMapCallBackInterface cb = o.getCallBack();
		if(cb != null)
			cb.sharedMapPutCallBack(new SharedMapPutResult(o.getEntry()));
	}

	@Override
	protected void handleRemoveValue(RemoveValue o) throws Exception {
		table.handleCall(o);
		
		agentPrintMessage("Removing key "+o.getKey());

		o.signalSuccess();
		SharedMapCallBackInterface cb = o.getCallBack();
		if(cb != null)
			cb.sharedMapRemoveCallBack(new SharedMapRemoveResult(o.getKey()));
	}

	@Override
	protected void handleUpdateValue(UpdateValue o) throws Exception {
		table.handleCall(o);
		
		agentPrintMessage("Updating key "+o.getKey());

		o.signalSuccess();
		SharedMapCallBackInterface cb = o.getCallBack();
		if(cb != null)
			cb.sharedMapUpdateCallBack(new SharedMapUpdateResult(o.getKey(), o.getUpdateData(), o.getEntry()));
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void init() throws Throwable {
		setPrintStream(ctrl.getFilePrefix());
		agentPrintMessage("init");
	}
	
	@Override
	protected void handleSpecificMessage(Object o) throws Exception {
		if(o instanceof CentralTableCallMessage) {
			handleCentralTableCallMessage((CentralTableCallMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleCentralTableCallMessage(CentralTableCallMessage m) {
		AsynchronousCall call = m.getCall();
		
		table.handleCall(call);
		
		DAId daId = m.getSourceDaId();
		ctrl.getCommunicator().sendDatagramMessage(
				new CentralTableCallBackMessage(daId, call));
	}

}
