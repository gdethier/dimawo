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
package dimawo.middleware.overlay.mntree.messages;

import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.mntree.MnId;

public class MnTreeRemovePeersMessage extends MnTreeMessage {
	private static final long serialVersionUID = 1L;

	public enum Source {parentMn, thisMn, childMn};

	private DAId[] removedPeers;
	private Source src;
	private int srcChildId;
	private boolean isLeave;

	public MnTreeRemovePeersMessage(DAId daTo, MnId from, MnId to, Source src, DAId[] removedPeers, boolean isLeave) {
		super(daTo, from, to);
		this.removedPeers = removedPeers;
		this.src = src;
		this.isLeave = isLeave;
		
		if(from == null || to == null)
			throw new Error("MnIds not set");
	}
	
	public MnTreeRemovePeersMessage(DAId daTo, MnId from, MnId to, int srcChildId, DAId[] removedPeers, boolean isLeave) {
		super(daTo, from, to);
		this.removedPeers = removedPeers;
		this.src = Source.childMn;
		this.srcChildId = srcChildId;
		this.isLeave = isLeave;
		
		if(from == null || to == null)
			throw new Error("MnIds not set");
	}

	public MnTreeRemovePeersMessage(Source src, DAId[] removedPeers, boolean isLeave) {
		this.removedPeers = removedPeers;
		this.src = src;
		this.isLeave = isLeave;
	}

	public DAId[] getRemovedPeers() {
		return removedPeers;
	}
	
	public Source getSource() {
		return src;
	}
	
	public int getSourceChildId() {
		return srcChildId;
	}

	public boolean sourceIsParent() {
		return Source.parentMn.equals(src);
	}

	public boolean sourceIsChild() {
		return Source.childMn.equals(src);
	}

	public boolean sourceIsThis() {
		return Source.thisMn.equals(src);
	}

	public boolean isLeave() {
		return isLeave;
	}
}
