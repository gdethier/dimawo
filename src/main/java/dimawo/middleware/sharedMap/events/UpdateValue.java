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
package dimawo.middleware.sharedMap.events;

import java.io.Serializable;

import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.Update;




public class UpdateValue extends AsynchronousCall implements Serializable {

	private static final long serialVersionUID = 1L;

	private String key;
	private Update update;
	private SharedMapEntry newEntry;

	private transient SharedMapCallBackInterface cb;

	public UpdateValue(String key, Update update, SharedMapEntry newEntry,
			SharedMapCallBackInterface cb) {
		this.key = key;
		this.update = update;
		this.newEntry = newEntry;
		this.cb = cb;
	}

	public String getKey() {
		return key;
	}
	
	public Update getUpdateData() {
		return update;
	}

	public SharedMapEntry getEntry() {
		return newEntry;
	}

	public SharedMapCallBackInterface getCallBack() {
		return cb;
	}
	
}
