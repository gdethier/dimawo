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
package dimawo.middleware.communication;

import java.io.IOException;
import java.util.TreeSet;

import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.distributedAgent.DAId;



public class OutputStreamsHandler {
	
	private Communicator com;
	
	private TreeSet<DAId> openOutputStreams;
	private TreeSet<DAId> closingOutputStreams;
	private TreeSet<DAId> awaitedOutputs;
	
	
	public OutputStreamsHandler(Communicator com) {
		this.com = com;
		
		openOutputStreams = new TreeSet<DAId>();
		closingOutputStreams = new TreeSet<DAId>();
		awaitedOutputs = new TreeSet<DAId>();
	}


	public void open(DAId daId) throws IOException {
		
		if(openOutputStreams.contains(daId))
			return; // MOS already opened

		openOutputStreams.add(daId);
		
		if(closingOutputStreams.contains(daId)) {
			// Wait MOS closed
			awaitedOutputs.add(daId);
		} else {
			// Create new MOS
			com.submitNewMOS(daId, com.getMessageOutputStream(daId));
		}
		
	}

	public void closing(DAId id) throws Exception {
		if( ! openOutputStreams.remove(id))
			throw new Exception("MOS is not open");
		if( ! closingOutputStreams.add(id))
			throw new Exception("MOS is already closing");
	}

	public void closed(DAId id, MessageOutputStream mos) throws Exception {

		if( ! closingOutputStreams.remove(id))
			throw new Exception("MOS not marked as closing");

		mos.join();
		
		if(awaitedOutputs.remove(id))
			com.submitNewMOS(id, com.getMessageOutputStream(id));

	}

	public void broken(DAId daId, MessageOutputStream mos) {
		try {
			mos.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		openOutputStreams.remove(daId);
		closingOutputStreams.remove(daId);
		if(awaitedOutputs.remove(daId))
			com.submitNewMOS(daId, null);
	}


}
