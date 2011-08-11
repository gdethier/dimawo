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
package dimawo.middleware.communication.outputStream;

import java.io.IOException;

import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;



public class LocalAccessor implements MOSAccessorInterface {

	private Communicator com;
	private DAId daId;


	public LocalAccessor(Communicator com) {

		this.com = com;
		daId = com.getHostingDaId();

	}
	

	@Override
	public void writeNonBlockingMessage(Message m) {
		writeMessage(m);
	}


	@Override
	public void close() throws IOException, InterruptedException {
		
		// SKIP
		
	}


	@Override
	public void writeBlockingMessage(Message m) throws IOException,
			InterruptedException {
		writeMessage(m);
	}

	private void writeMessage(Message m) {
		m.setSender(daId);
		m.setRecipient(daId);
		com.submitIncomingMessage(m);
	}


	@Override
	public DAId getDestinationDAId() {
		return daId;
	}
}
