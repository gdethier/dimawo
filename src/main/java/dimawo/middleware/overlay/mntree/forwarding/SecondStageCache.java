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
package dimawo.middleware.overlay.mntree.forwarding;

import java.util.TreeMap;

public class SecondStageCache {
	
	private TreeMap<MessageId, SecondStageMessageInfo> cache;
	
	
	public SecondStageCache() {
		cache = new TreeMap<MessageId, SecondStageMessageInfo>();
	}
	
	public void cache(SecondStageMessageInfo info) {
		MessageId ssmId = info.getMessageId();
		cache.put(ssmId, info);
	}

	public SecondStageMessageInfo remove(MessageId id) {
		return cache.remove(id);
	}
	
	public int size() {
		return cache.size();
	}

	public SecondStageMessageInfo get(MessageId msgId) {
		return cache.get(msgId);
	}

	public boolean contains(MessageId messageId) {
		return cache.containsKey(messageId);
	}
}
