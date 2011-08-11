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
package dimawo.middleware.sharedMap.dht.chord;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.Update;



public class ChordDataTable {
	private int numEntries;
	private HashMap<ChordId, LinkedList<SharedMapEntry>> table;
	private HashMap<ChordId, LinkedList<SharedMapEntry>> replicas;
	
	public ChordDataTable() {
		numEntries = 0;
		table = new HashMap<ChordId, LinkedList<SharedMapEntry>>();
		replicas = new HashMap<ChordId, LinkedList<SharedMapEntry>>();
	}

	public SharedMapEntry getEntry(String key) {
		ChordId id = new ChordId(key);
		return getEntry(id, key);
	}

	public SharedMapEntry getEntry(ChordId id, String key) {
		LinkedList<SharedMapEntry> list = table.get(id);
		if(list == null) {
			list = replicas.get(id);
			if(list == null)
				return null;
		}
		
		for(SharedMapEntry en : list) {
			if(en.getKey().equals(key))
				return en;
		}
		
		return null;
	}

	public void putEntry(ChordId keyChordId, SharedMapEntry entry) {
		put(table, keyChordId, entry, true);
	}
	
	private void put(HashMap<ChordId, LinkedList<SharedMapEntry>> tab, ChordId keyChordId, SharedMapEntry entry, boolean modifyNumEntries) {
		LinkedList<SharedMapEntry> list = tab.get(keyChordId);
		if(list == null) {
			list = new LinkedList<SharedMapEntry>();
			table.put(keyChordId, list);
		} else {
			if(list.remove(entry) && modifyNumEntries)
				--numEntries;
		}
		
		if(modifyNumEntries)
			++numEntries;
		list.add(entry);
	}
	
	public LinkedList<SharedMapEntry> getContent() {
		LinkedList<SharedMapEntry> content = new LinkedList<SharedMapEntry>();
		for(LinkedList<SharedMapEntry> l : table.values())
			for(SharedMapEntry e : l)
				content.addLast(e.clone());
		return content;
	}
	
	public LinkedList<SharedMapEntry> getContent(ChordId low, ChordId up) {
		LinkedList<SharedMapEntry> ll = new LinkedList<SharedMapEntry>();
		for(Iterator<Entry<ChordId, LinkedList<SharedMapEntry>>> it =
			table.entrySet().iterator(); it.hasNext();) {
			Entry<ChordId, LinkedList<SharedMapEntry>> e = it.next();
			ChordId eId = e.getKey();
			if(eId.isInInterval(low, up)) {
				for(SharedMapEntry entry : e.getValue()) {
					ll.add(entry);
				}
			}
		}
		return ll;
	}

	public void putEntries(Iterable<SharedMapEntry> initData) {
		for(SharedMapEntry e : initData) {
			String key = e.getKey();
			ChordId id = new ChordId(key);
			
			if(! contains(id, e))
				putEntry(id, e);
		}
	}

	private boolean contains(ChordId id, SharedMapEntry e) {
		LinkedList<SharedMapEntry> l = table.get(id);
		return l != null && l.contains(e);
	}

	public SharedMapEntry updateEntry(ChordId keyChordId, String key, Update updateData,
			SharedMapEntry entry) {
		LinkedList<SharedMapEntry> list = table.get(keyChordId);
		if(list == null && entry == null) {
			return null; // Nothing to update and no new entry to add.
		}

		SharedMapEntry updated = null;
		if(list == null) {
			list = new LinkedList<SharedMapEntry>();
			table.put(keyChordId, list);
			list.add(entry);
			updated = entry;
			++numEntries;
		} else {
			for(SharedMapEntry e : list) {
				if(e.getKey().equals(key)) {
					updated = e;
					break;
				}
			}
			
			if(updated != null) {
				updated.update(updateData);
			} else if(entry != null) {
				list.add(entry);
				updated = entry;
				++numEntries;
			}
		}
		
		return updated;
	}

	public void removeEntry(ChordId id, String key) {
		LinkedList<SharedMapEntry> list = table.get(id);
		if(list == null) {
			return;
		}
		
		for(Iterator<SharedMapEntry> it = list.iterator(); it.hasNext();) {
			SharedMapEntry en = it.next();
			if(en.getKey().equals(key)) {
				it.remove();
				--numEntries;
				if(list.isEmpty()) {
					table.remove(id);
				}
				return;
			}
		}
	}

	public void printKeys(ChordAgent chordAgent) {
		for(LinkedList<SharedMapEntry> ll : table.values()) {
			for(SharedMapEntry e : ll) {
				chordAgent.agentPrintMessage(e.getKey());
			}
		}
	}

	public int size() {
		return numEntries;
	}

	public void putReplica(ChordId chordId, SharedMapEntry e) {
		put(replicas, chordId, e, false);
	}

	public void migrateReplicas(ChordId precId, ChordId thisId) {
		for(Iterator<Entry<ChordId, LinkedList<SharedMapEntry>>> it =
			replicas.entrySet().iterator(); it.hasNext();) {
			Entry<ChordId, LinkedList<SharedMapEntry>> e = it.next();
			ChordId eId = e.getKey();
			if(eId.isInInterval(precId, thisId)) {
				for(SharedMapEntry entry : e.getValue()) {
					LinkedList<SharedMapEntry> l = table.get(eId);
					if(l == null || ! l.contains(entry)) {
						putEntry(eId, entry);
					}
				}
				it.remove();
			}
		}
	}

	public void cleanTable(ChordId low, ChordId up) {
		for(Iterator<Entry<ChordId, LinkedList<SharedMapEntry>>> it =
			table.entrySet().iterator(); it.hasNext();) {
			Entry<ChordId, LinkedList<SharedMapEntry>> e = it.next();
			ChordId eId = e.getKey();
			LinkedList<SharedMapEntry> eLl = e.getValue();
			if(! eId.isInInterval(low, up)) {
				replicas.put(eId, eLl);
				numEntries -= eLl.size();
				it.remove();
			}
		}
	}
}
