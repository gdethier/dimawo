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
package dimawo.fileTransfer.test;

import java.io.IOException;

import dimawo.fileTransfer.client.FileTransferClientAgent;
import dimawo.fileTransfer.server.FileTransferServerAgent;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.faultdetection.FaultDetectionAgent;




public class ServerCommunicator extends TestCommunicator {

	public ServerCommunicator(FileTransferServerAgent server,
			FileTransferClientAgent client) {
		super(server, client);
	}

	@Override
	public void sendDatagramMessage(Message m) {
		m.setSender(SERVERDAID);
		try {
			routeMessage(m);
		} catch (IOException e) {
			m.setMessageSent(false);
		}
	}

	@Override
	public DAId getHostingDaId() {
		return SERVERDAID;
	}

	@Override
	public void multicastMessage(DAId[] ids, Message msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerMessageHandler(Object messageHandlerId,
			MessageHandler lbSimThread) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterMessageHandler(Object messageHandlerId,
			MessageHandler lbSimThread) {
		// TODO Auto-generated method stub
		
	}
}
