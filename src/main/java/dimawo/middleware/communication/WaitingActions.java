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

import java.util.LinkedList;

public class WaitingActions {

	private int referenceCount;
	private int closes;
	private LinkedList<Message> queuedMessages;


	public WaitingActions() {

		queuedMessages = new LinkedList<Message>();

	}

	public int getQueuedMessagesCount() {

		return queuedMessages.size();

	}

	public LinkedList<Message> getQueuedMessages() {

		return queuedMessages;

	}

	public int getReferenceCount() {
		
		return referenceCount;
		
	}
	
	public void removeReference() {

		--referenceCount;

	}

	public void addReference() {

		++referenceCount;

	}

	public void queueMessage(Message m) {

		queuedMessages.addLast(m);

	}

	public void addClose() {

		++closes;
		
	}

	public int getClosesCount() {

		return closes;

	}

}
