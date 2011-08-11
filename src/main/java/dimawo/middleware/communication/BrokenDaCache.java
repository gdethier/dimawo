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
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class BrokenDaCache {
	private int capacity;
	private TreeSet<DAId> brokenDAs;
	private LinkedList<DAId> chronology;

	public BrokenDaCache(int capacity) {
		this.capacity = capacity;
		
		brokenDAs = new TreeSet<DAId>();
		chronology = new LinkedList<DAId>();
	}

	public void addBrokenDa(DAId daId) {
		boolean added = brokenDAs.add(daId);
		if(added) {
			chronology.addLast(daId);

			if(chronology.size() > capacity) {
				DAId removed = chronology.removeFirst();
				brokenDAs.remove(removed);
			}
		}
	}

	public boolean isBroken(DAId daId) {
		return brokenDAs.contains(daId);
	}
}
