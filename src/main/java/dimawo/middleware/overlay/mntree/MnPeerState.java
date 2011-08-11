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
import java.util.Set;
import java.util.TreeSet;

import dimawo.middleware.distributedAgent.DAId;



public class MnPeerState implements Serializable {
	private static final long serialVersionUID = 1L;
	private int reliabilityThreshold;
	
	private boolean isDisconnected;

	private int childIndex; // this MN is the childIndex th child of its parent MN
	
	private MnId thisMnId;
	private MnId parentId;
	private MnId[] childrenIds;
	
	private MnRoutingTable thisTable;
	private MnRoutingTable parentTable;
	private int numOfChildren;
	private MnRoutingTable[] childrenTable;
	
	private int nextInsertChildIndex;


	public static MnPeerState createRootState(DAId id, int maxNumOfChildren,
			int reliabilityThreshold) {
		MnPeerState state = new MnPeerState(maxNumOfChildren, reliabilityThreshold);
		
		state.thisMnId = MnId.getRootId();
		state.thisTable.addMainPeer(id);
		state.isDisconnected = false;

		return state;
	}
	
	public MnPeerState(int maxNumOfChildren, int reliabilityThreshold) {
		childIndex = -1;
		
		this.reliabilityThreshold = reliabilityThreshold;

		childrenIds = new MnId[maxNumOfChildren];
		thisTable = new MnRoutingTable();
		childrenTable = new MnRoutingTable[maxNumOfChildren];
		isDisconnected = false;
		
		nextInsertChildIndex = 0;
	}
	
	public MnPeerState clone() {
		MnPeerState cloned = new MnPeerState(childrenIds.length, this.reliabilityThreshold);
		cloned.isDisconnected = isDisconnected;
		cloned.childIndex = childIndex;

		cloned.thisMnId = thisMnId;
		cloned.parentId = parentId;
		for(int i = 0; i < childrenIds.length; ++i)
			cloned.childrenIds[i] = childrenIds[i];
		
		cloned.thisTable.fillWith(thisTable);
		if(parentTable != null) {
			cloned.parentTable = new MnRoutingTable();
			cloned.parentTable.fillWith(parentTable);
		}
		cloned.numOfChildren = numOfChildren;
		for(int i = 0; i < childrenTable.length; ++i)
			if(childrenTable[i] != null) {
				cloned.childrenTable[i] = new MnRoutingTable();
				cloned.childrenTable[i].fillWith(childrenTable[i]);
			} else
				cloned.childrenTable[i] = null;

		return cloned;
	}
	

	public int getThisMnSize() {
		return thisTable.size();
	}

	public void addPeerToThisMn(DAId da) {
		thisTable.addPeer(da);
	}
	
	public boolean isMainPeer(DAId id) {
		return id.equals(thisTable.getMainPeer());
	}

	public DAId getThisMnMainPeer() {
		return thisTable.getMainPeer();
	}

	public MnId getThisMnId() {
		return thisMnId;
	}

	public int getNumOfChildren() {
		return numOfChildren;
	}

	public int getMaxNumOfChildren() {
		return childrenIds.length;
	}

	public DAId getChildMnMainPeer(int childId) {
		if(childrenTable[childId] == null)
			return null;
		return childrenTable[childId].getMainPeer();
	}

	public MnId getChildMnId(int childId) {
		return childrenIds[childId];
	}

	public boolean thisMnContains(DAId daId) {
		return thisTable.containsPeer(daId);
	}

	public void getThisMnPeersIds(Set<DAId> peers) {
		thisTable.getPeers(peers);
	}
	
	public void getThisMnPeersIds(DAId[] peers) {
		thisTable.getPeers(peers);
	}

	public void getParentMnPeersIds(Set<DAId> addr) {
		if(parentTable != null)
			parentTable.getPeers(addr);
	}
	
	public void getChildMnPeersIds(int childId, Set<DAId> addr) {
		childrenTable[childId].getPeers(addr);
	}

	public MnId getParentMnId() {
		return parentId;
	}

	public int getThisChildIndex() {
		return childIndex;
	}

	public boolean hasChild(int childId) {
		return childrenIds[childId] != null;
	}

	public void extractYoungestPeersFromThisMn(DAId[] newMnPeers, int from, int length) {
		thisTable.extractYoungestPeers(newMnPeers, from, length);
	}

	public int getNextChildId() {
		for(int i = 0; i < childrenIds.length; ++i)
			if(childrenIds[i] == null)
				return i;

		throw new Error("No more child can be added");
	}

	public MnPeerState getNewChildState(int childIndex, DAId mainPeer, DAId[] peers) {
		MnPeerState newState = new MnPeerState(childrenIds.length, this.reliabilityThreshold);
		newState.childIndex = childIndex;
		newState.thisTable.addMainPeer(mainPeer);
		newState.thisTable.addPeers(peers);
		newState.parentId = thisMnId;
		newState.parentTable = new MnRoutingTable();
		newState.parentTable.fillWith(thisTable);
		newState.thisMnId = thisMnId.getChildId(childIndex);
		return newState;
	}

//	public void setChildMnMainPeer(int childId, DAId daId) {
//		childrenTable[childId].addMainPeer(daId);
//	}

//	public void addPeersToChildMn(int newChildId, DAId[] newMnPeers) {
//		childrenTable[childId].addPeers(newMnPeers);
//	}
	
	public void setChildMn(int childId, DAId main, DAId[] peers) {
		if(childrenTable[childId] != null)
			throw new Error("Child for index "+childId+" is already set");
		
		++numOfChildren;

		MnRoutingTable table = new MnRoutingTable();
		table.addMainPeer(main);
		table.addPeers(peers);
		childrenTable[childId] = table;
		childrenIds[childId] = thisMnId.getChildId(childId);
	}

	public boolean thisMnIsReliable() {
		return thisTable.size() >= reliabilityThreshold;
	}

	public boolean hasMaxNumOfChildren() {
		return numOfChildren == childrenIds.length;
	}

	public int getReliabilityThreshold() {
		return reliabilityThreshold;
	}

	public int getFirstValidChildId(int childId) {
		if(numOfChildren == 0)
			return -1;
		while(childrenTable[childId] == null) {
			childId = (childId + 1) % childrenTable.length;
		}
		return childId;
	}

	public boolean[] removePeersFromParentMn(DAId[] removedPeers) {
		if(parentTable != null)
			return parentTable.removePeers(removedPeers);
		else
			return new boolean[removedPeers.length];
	}

	public boolean[] removePeersFromChildMn(int childId, DAId[] removedPeers) {
		if(childrenTable[childId] != null)
			return childrenTable[childId].removePeers(removedPeers);
		else
			return new boolean[removedPeers.length];
	}

	public boolean[] removePeersFromThisMn(DAId[] removedPeers) {
		return thisTable.removePeers(removedPeers);
	}
	
	public boolean removePeersFromThisMn(DAId daId) {
		return thisTable.removePeer(daId);
	}

	public void addPeerToParentMn(DAId daId) {
		parentTable.addPeer(daId);
	}

	public void addPeerToChildMn(int childId, DAId daId) {
		childrenTable[childId].addPeer(daId);
	}

	public int getParentMnSize() {
		return parentTable.size();
	}

	public int getChildMnSize(int childId) {
		return childrenTable[childId].size();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
//		sb.append("reliabilityThreshold=").append(reliabilityThreshold).append('\n');
		sb.append("thisMnId=").append(thisMnId.toString()).append('\n');
		sb.append("thisRoutingTable:").append('\n');
		sb.append(thisTable.toString()).append('\n');
		if(parentTable != null)
			sb.append("parentSize=").append(parentTable.size()).append('\n');
		sb.append("numOfChildren=").append(numOfChildren).append('\n');
		for(int i = 0; i < childrenTable.length; ++i) {
			sb.append("childrenMnSize[").append(i).append("]=");
			if(childrenTable[i] != null) {
				sb.append(childrenTable[i].size());
				sb.append(" (main=").append(childrenTable[i].getMainPeer()).append(")");
			} else
				sb.append('0');
			sb.append('\n');
		}
		return sb.toString();
	}

	public DAId getParentMnMainPeer() {
		if(parentTable != null)
			return parentTable.getMainPeer();
		return null;
	}

	public DAId getMainId(MnId mnId) {
		if(mnId.equals(thisMnId)) {
			return thisTable.getMainPeer();
		}

		int thisLevel = thisMnId.getLevel();
		int otherLevel = mnId.getLevel();
		
		if(thisLevel == otherLevel + 1) {
			// mnId is parent MN id
			return parentTable.getMainPeer();
		} else if(thisLevel == otherLevel - 1) {
			// mnId is a child MN id
			int childIndex = mnId.getLastLevelChildIndex();
			return childrenTable[childIndex].getMainPeer();
		} else {
			throw new Error("Given MN is not a neighbor");
		}
	}

	public boolean parentMnIsEmpty() {
		return parentTable.size() == 0;
	}
	
	public boolean childMnIsEmpty(int childIndex) {
		return childrenTable[childIndex].size() == 0;
	}

	public int getNextInsertChildIndex() {
		nextInsertChildIndex = (nextInsertChildIndex + 1) % numOfChildren;
		int childIndex = 0;
		int count = 0;
		while(count < nextInsertChildIndex) {
			if(childrenIds[childIndex] != null) {
				++count;
			}
			++childIndex;
		}
		return childIndex;
	}

	public boolean hasParent() {
		return parentId != null;
	}

	public void updateThisRoutingTable(DAId mainPeer, DAId[] peers) {
		thisTable.reset(mainPeer, peers);
	}

	public void updateParentRoutingTable(DAId mainPeer, DAId[] peers) {
		parentTable.reset(mainPeer, peers);
	}

	public void updateChildRoutingTable(int ci, DAId mainPeer,
			DAId[] peers) {
		childrenTable[ci].reset(mainPeer, peers);
	}

	public boolean isDisconnected() {
		return isDisconnected;
	}

	public void setDisconnectedFlag(boolean b) {
		isDisconnected = b;
	}

	public void getAllNeighbors(TreeSet<DAId> allNeighbors) {
		thisTable.getPeers(allNeighbors);
		if(parentTable != null)
			parentTable.getPeers(allNeighbors);
		for(MnRoutingTable rt : childrenTable) {
			if(rt != null) {
				rt.getPeers(allNeighbors);
			}
		}
	}

	public boolean contains(DAId id) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
