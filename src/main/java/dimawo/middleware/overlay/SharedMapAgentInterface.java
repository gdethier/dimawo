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
package dimawo.middleware.overlay;

import dimawo.middleware.sharedMap.messages.SharedMapAgentMessage;

public interface SharedMapAgentInterface {

	public void put(SharedMapEntry dhtEntry) throws Exception;
	public void putAsync(SharedMapEntry entry, SharedMapCallBackInterface cb);

	public void getAsync(String key, SharedMapCallBackInterface cb);
	
	public void update(String key, Update update, SharedMapEntry newEntry) throws Exception;
	public void updateAsync(String key, Update update, SharedMapEntry newEntry, SharedMapCallBackInterface cb);

	public void remove(String key) throws Exception;
	public void removeAsync(String key, SharedMapCallBackInterface cb);
	
	public void submitDMapAgentMessage(SharedMapAgentMessage m);

	public void start() throws Exception;
	public void stop() throws Exception;
	public void join() throws InterruptedException;
}
