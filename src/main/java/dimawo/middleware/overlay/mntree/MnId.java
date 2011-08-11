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
package dimawo.middleware.overlay.mntree;

import java.io.Serializable;
import java.util.Arrays;

public class MnId implements Serializable, Comparable<MnId> {
	private static final long serialVersionUID = 1L;

	/** This array describes the path from root to this
	 * MN. A down-edge is an edge going from a parent to its child.
	 * edgeIds[i] gives the id of the down-edge followed at level i
	 * starting from reached node at this level.
	 * The root is at level 0, its children at level 1, etc.
	 * edgeIds.length of this array gives the level in the MN-tree of this MN.*/
	private int[] edgeIds;
	
	private MnId(int[] edgeIds) {
		this.edgeIds = edgeIds;
	}
	
	public MnId getChildId(int childId) {
		int[] edgeIds = new int[this.edgeIds.length + 1];
		System.arraycopy(this.edgeIds, 0, edgeIds, 0, this.edgeIds.length);
		edgeIds[edgeIds.length - 1] = childId;
		return new MnId(edgeIds);
	}
	
	public MnId getParentId() {
		if(edgeIds.length == 0)
			throw new Error("This MN has no parent");

		int[] edgeIds = new int[this.edgeIds.length - 1];
		System.arraycopy(this.edgeIds, 0, edgeIds, 0, edgeIds.length);
		return new MnId(edgeIds);
	}
	
	public boolean equals(Object o) {
		if(! (o instanceof MnId))
			return false;
		
		MnId otherId = (MnId) o;
		return Arrays.equals(edgeIds, otherId.edgeIds);
	}

	public static MnId getRootId() {
		return new MnId(new int[0]);
	}
	
	public String toString() {
		if(edgeIds.length == 0)
			return "[]";
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(int i = 0; i < edgeIds.length - 1; ++i)
			sb.append(edgeIds[i]).append(',');
		sb.append(edgeIds[edgeIds.length-1]).append(']');
		return sb.toString();
	}

	@Override
	public int compareTo(MnId sourceId) {
		if(edgeIds.length != sourceId.edgeIds.length) {
			return edgeIds.length - sourceId.edgeIds.length;
		} else {
			int numLower = 0, numGreater = 0;
			int x, y;
			for(int i = 0; i < edgeIds.length; ++i) {
				x = edgeIds[i];
				y = sourceId.edgeIds[i];
				if(x < y) {
					++numLower;
				} else if(x > y) {
					++numGreater;
				}
			}
			
			if(numLower > numGreater) {
				return -1;
			} else if(numLower < numGreater) {
				return 1;
			} else if(numLower == numGreater && numLower != 0) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public int getLevel() {
		return edgeIds.length;
	}

	public int getLastLevelChildIndex() {
		return edgeIds[edgeIds.length - 1];
	}
}
