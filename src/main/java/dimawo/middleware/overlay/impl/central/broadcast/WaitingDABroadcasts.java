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

import java.io.IOException;
import java.util.HashMap;

import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;



public class WaitingDABroadcasts implements MOSCallBack {
	
	private CommunicatorInterface com;
	
	private class WaitingDA {
		public DAId daId;
		public int broadNum;
		
		public WaitingDA(DAId daId, int broadNum) {
			this.daId = daId;
			this.broadNum = broadNum;
		}
	}
	
	private HashMap<Message, WaitingDA> das;
	
	
	public WaitingDABroadcasts(CommunicatorInterface com) {
		this.com = com;
		
		das = new HashMap<Message, WaitingDA>();
	}

	public void add(DAId sourceDa, int daBroadNum, Message msg) {
		das.put(msg, new WaitingDA(sourceDa, daBroadNum));
	}

	@Override
	public void signalBroken(BrokenDA bda) {
		// SKIP
	}

	@Override
	public void signalSent(Message m, boolean success) {
		WaitingDA dat = das.remove(m);
		com.sendDatagramMessage(new BroadcastRequestAckMessage(dat.daId, dat.broadNum));
	}

}
