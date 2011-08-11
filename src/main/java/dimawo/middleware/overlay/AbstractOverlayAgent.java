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
package dimawo.middleware.overlay;

import java.io.IOException;

import dimawo.agents.LoggingAgent;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.impl.central.events.ShutdownOverlay;
import dimawo.simulation.socket.SocketFactory;



public abstract class AbstractOverlayAgent extends LoggingAgent implements
		OverlayAgentInterface {
	private DistributedAgent da;
	private Communicator com;
	
	public AbstractOverlayAgent(DistributedAgent da, String name,
			SocketFactory sockFact) throws IOException {
		super(null, name);
		
		this.da = da;
		this.setPrintStream(da.getFilePrefix());

		com = new Communicator(this, da, sockFact);
		com.setOverlayInterface(this);
		
		da.setOverlayInterface(this);
	}

	@Override
	public void shutdownOverlay() throws OverlayException {
		try {
			submitMessage(new ShutdownOverlay());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public DistributedAgent getDA() {
		return da;
	}

	@Override
	public CommunicatorInterface getCommunicator() {
		return com;
	}

	@Override
	public void submitOverlayMessage(OverlayMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void logAgentExit() {
		onClose();
		
		com.stop();
		try {
			com.join();
		} catch (InterruptedException e) {
		}
		
		da.signalOverlayDisconnected();
		agentPrintMessage("Overlay closed.");
	}

	protected abstract void onClose();

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof OverlayMessage) {
			handleOverlayMessage(o);
		} else {
			handleEvent(o);
		}
	}

	private void handleEvent(Object o) {
		if(o instanceof ShutdownOverlay) {
			handleShutdownOverlay((ShutdownOverlay) o);
		}
	}
	
	protected abstract void handleShutdownOverlay(ShutdownOverlay o);
	protected abstract void handleOverlayMessage(Object msg);

}
