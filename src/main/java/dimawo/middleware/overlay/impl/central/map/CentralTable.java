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

import java.util.TreeMap;

import dimawo.agents.UnknownAgentMessage;
import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.sharedMap.events.GetValue;
import dimawo.middleware.sharedMap.events.PutValue;
import dimawo.middleware.sharedMap.events.RemoveValue;
import dimawo.middleware.sharedMap.events.UpdateValue;




public class CentralTable {

	private TreeMap<String, SharedMapEntry> table;
	
	
	public CentralTable() {
		table = new TreeMap<String, SharedMapEntry>();
	}

//	public DHTEntry put(String key, DHTEntry val) {
//		return table.put(key, val);
//	}

	public void handleCall(AsynchronousCall o) {
		if(o instanceof PutValue) {
			handlePutValue((PutValue) o);
		} else if(o instanceof GetValue) {
			handleGetValue((GetValue) o);
		} else if(o instanceof UpdateValue) {
			handleUpdateValue((UpdateValue) o);
		} else if(o instanceof RemoveValue) {
			handleRemoveValue((RemoveValue) o);
		} else {
			o.setError(new UnknownAgentMessage(o));
		}
	}

	private void handleRemoveValue(RemoveValue o) {
		String key = o.getKey();
		table.remove(key);
	}

	private void handleUpdateValue(UpdateValue o) {
		String key = o.getKey();
		SharedMapEntry e = table.get(key);
		if(e == null) {
			table.put(key, o.getEntry());
		} else {
			e.update(o.getUpdateData());
		}
	}

	private void handleGetValue(GetValue o) {
		String key = o.getKey();
		SharedMapEntry e = table.get(key);
		if(e == null) {
			o.setError(new Exception("No entry for key "+key));
		} else {
			o.setResult(e);
		}
	}

	private void handlePutValue(PutValue o) {
		SharedMapEntry e = o.getEntry();
		table.put(e.getKey(), e);
	}

}
