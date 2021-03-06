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
package dimawo.middleware.sharedMap.dht.chord.messages;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.SharedMapValue;
import dimawo.middleware.sharedMap.dht.chord.ChordId;


public class GetDataEntryResultMessage extends ChordAgentMessage
implements CallStateMessage {
	private static final long serialVersionUID = 1L;
	
	private String key;
	private SharedMapValue val;
	private boolean routingError;

	public GetDataEntryResultMessage(DAId to, ChordId chordFrom,
			ChordId chordTo, String key, SharedMapValue val, boolean routingError) {
		super(to, chordFrom, chordTo);
		
		this.key = key;
		this.val = val;
		this.routingError = routingError;
	}
	
	public String getKey() {
		return key;
	}

	public SharedMapValue getValue() {
		return val;
	}
	
	public boolean isRoutingError() {
		return routingError;
	}
}
