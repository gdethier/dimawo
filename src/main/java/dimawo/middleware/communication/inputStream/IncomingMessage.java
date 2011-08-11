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
package dimawo.middleware.communication.inputStream;

import dimawo.middleware.communication.Message;

/**
 * IncomingMessages are used by input stream handlers (@see InputStreamHandler)
 * to wrap messages before they are passed to a consumer agent.
 * This way, the consumer agent can make the difference between messages coming from remote DAs
 * and messages coming from local agents.
 * <p>
 * This is particularly interesting when some
 * message types can be generated both by remote and local agents.
 * 
 * @author Gérard Dethier
 *
 */
public class IncomingMessage {

	/** The wrapped message */
	protected Message msg;
	
	/**
	 * The constructor. The given Message is wrapped by this object.
	 * 
	 * @param m The message to wrap.
	 */
	public IncomingMessage(Message m) {
		msg = m;
	}
	
	/**
	 * Gets the wrapped message.
	 * 
	 * @return A reference to the wrapped message.
	 * 
	 * @see comLayer.messages.Message
	 */
	public Message getMessage() {
		return msg;
	}

}
