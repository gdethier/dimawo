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
package dimawo;

import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.distributedAgent.DAId;

/**
 * 
 * @author Gerard Dethier
 * 
 * Represents a message sent to a Worker Agent (WA). All messages sent to a
 * remote WA must inherit from this class. Indeed, the Distributed Agent
 * routes all subclasses of <code>WorkerMessage</code> to the WA it
 * instantiated.
 */
public class WorkerMessage extends Message {
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a <code>WorkerMessage</code>.
	 * This constructor is generally used if the message will be sent using
	 * connected mode (i.e. destination is imposed by the connection).
	 * 
	 * @see Communicator#asyncConnect(DAId, dimawo.middleware.communication.ConnectionRequestCallBack, dimawo.middleware.communication.outputStream.MOSCallBack, Object)
	 * @see Communicator#syncConnect(DAId, dimawo.middleware.communication.outputStream.MOSCallBack)
	 * @see MOSAccessorInterface
	 */
	public WorkerMessage() {
		super();
	}

	/**
	 * Instantiates a <code>WorkerMessage</code> and sets its destination.
	 * This constructor is generally used when sending a datagram message.
	 * 
	 * @param t The destination of this message.
	 * 
	 * @see Communicator#sendDatagramMessage(Message)
	 */
	public WorkerMessage(DAId t) {
		super(t);
	}
}
