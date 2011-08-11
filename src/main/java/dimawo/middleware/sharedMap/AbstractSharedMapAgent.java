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
package dimawo.middleware.sharedMap;

import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.middleware.overlay.SharedMapAgentInterface;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.Update;
import dimawo.middleware.sharedMap.events.GetValue;
import dimawo.middleware.sharedMap.events.PutValue;
import dimawo.middleware.sharedMap.events.RemoveValue;
import dimawo.middleware.sharedMap.events.UpdateValue;
import dimawo.middleware.sharedMap.messages.SharedMapAgentMessage;

public abstract class AbstractSharedMapAgent extends LoggingAgent implements
		SharedMapAgentInterface {
	
	public AbstractSharedMapAgent(ErrorHandler eh, String name) {
		super(eh, name);
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof PutValue) {
			handlePutValue((PutValue) o);
		} else if(o instanceof GetValue) {
			handleGetValue((GetValue) o);
		} else if(o instanceof UpdateValue) {
			handleUpdateValue((UpdateValue) o);
		} else if(o instanceof RemoveValue) {
			handleRemoveValue((RemoveValue) o);
		} else {
			handleSpecificMessage(o);
		}
	}
	
	public void putAsync(SharedMapEntry entry, SharedMapCallBackInterface cb) {
		try {
			submitMessage(new PutValue(entry, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void getAsync(String key, SharedMapCallBackInterface cb) {
		try {
			submitMessage(new GetValue(key, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void put(SharedMapEntry dhtEntry) throws Exception {
		PutValue pv = new PutValue(dhtEntry, null);
		try {
			submitMessage(pv);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		pv.waitOn();
		if( ! pv.isSuccessful()) {
			throw pv.getError();
		}
		
	}
	
	public void update(String key, Update update, SharedMapEntry newEntry) throws Exception {
		if(newEntry != null && ! key.equals(newEntry.getKey()))
			throw new Error("Entry key is different from call key");
		UpdateValue uv = new UpdateValue(key, update, newEntry, null);
		try {
			submitMessage(uv);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		uv.waitOn();
		if( ! uv.isSuccessful()) {
			uv.throwError();
		}
	}
	
	@Override
	public void updateAsync(String key, Update update, SharedMapEntry newEntry,
			SharedMapCallBackInterface cb) {
		if(newEntry != null && ! key.equals(newEntry.getKey()))
			throw new Error("Entry key is different from call key");
		UpdateValue uv = new UpdateValue(key, update, newEntry, cb);
		try {
			submitMessage(uv);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void remove(String key) throws Exception {
		RemoveValue uv = new RemoveValue(key, null);
		try {
			submitMessage(uv);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		uv.waitOn();
		if( ! uv.isSuccessful()) {
			uv.throwError();
		}
	}


	public void removeAsync(String key, SharedMapCallBackInterface cb) {
		try {
			submitMessage(new RemoveValue(key, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void submitDMapAgentMessage(SharedMapAgentMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected abstract void handlePutValue(PutValue o) throws Exception;
	protected abstract void handleUpdateValue(UpdateValue o) throws Exception;
	protected abstract void handleRemoveValue(RemoveValue o) throws Exception;
	protected abstract void handleGetValue(GetValue o) throws Exception;
	protected abstract void handleSpecificMessage(Object o) throws Exception;

}
