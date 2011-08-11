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

import dimawo.middleware.distributedAgent.DAId;

public class ComputerTreePosition {
	private DAId thisId;
	private DAId parentId;
	private DAId[] childIds;
	private boolean hasNoChild;
	private int numOfChildren;
	
	public ComputerTreePosition(DAId thisId, DAId parentId, DAId[] childIds) {
		this.thisId = thisId;
		this.parentId = parentId;
		this.childIds = childIds;
		
		hasNoChild = true;
		numOfChildren = 0;
		for(int i = 0; i < childIds.length; ++i) {
			if(childIds[i] != null) {
				hasNoChild = false;
				++numOfChildren;
			}
		}
	}
	
	public DAId getThisId() {
		return thisId;
	}
	
	public DAId getParentId() {
		return parentId;
	}
	
	public DAId getChildId(int childIndex) {
		return childIds[childIndex];
	}

	public int getMaxNumOfChildren() {
		return childIds.length;
	}
	
	public int getChildIndex(DAId childId) {
		for(int i = 0; i < childIds.length; ++i) {
			if(childIds[i].equals(childId))
				return i;
		}
		
		return -1;
	}

	public boolean isParent(DAId daId) {
		return daId.equals(parentId);
	}

	public boolean hasNoChild() {
		return hasNoChild;
	}

	public boolean hasParent() {
		return parentId != null;
	}

	public boolean hasChild(int i) {
		return childIds[i] != null;
	}

	public int getNumOfChildren() {
		return numOfChildren;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[parent=").append(parentId).append(';');
		for(int i = 0; i < numOfChildren-1; ++i) {
			DAId childId = childIds[i];
			sb.append("child").append(i).append('=').append(childId).append(';');
		}
		if(numOfChildren > 0)
			sb.append("child").append(numOfChildren-1).append(childIds[numOfChildren-1]);
		sb.append(']');
		return sb.toString();
	}
}
