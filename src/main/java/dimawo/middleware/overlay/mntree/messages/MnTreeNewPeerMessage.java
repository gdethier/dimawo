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

public class MnTreeNewPeerMessage extends MnTreeMessage {
	private static final long serialVersionUID = 1L;
	
	public enum Source {parentMn, thisMn, childMn};

	private DAId newPeer;
	private Source src;
	private int srcChildId;
	private boolean isJoin;

	public MnTreeNewPeerMessage(DAId daId, MnId from, MnId to, DAId newPeer,
			Source src, boolean isJoin) {
		super(daId, from, to);
		
		this.newPeer = newPeer;
		this.src = src;
		if(src.equals(Source.childMn))
			throw new Error("Child source is specified with child id");
		this.isJoin = isJoin;
	}
	
	public MnTreeNewPeerMessage(DAId daId, MnId from, MnId to, DAId newPeer,
			int childId, boolean isJoin) {
		super(daId, from, to);
		
		this.newPeer = newPeer;
		src = Source.childMn;
		srcChildId = childId;
		this.isJoin = isJoin;
	}
	
	public MnTreeNewPeerMessage(DAId daId, Source src,
			boolean isJoin) {
		this.newPeer = daId;
		this.src = src;
		this.isJoin = isJoin;
	}

	public DAId getNewPeer() {
		return newPeer;
	}
	
	public Source getSource() {
		return src;
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
	
	public int getSourceChildId() {
		return srcChildId;
	}

	public boolean isJoin() {
		return isJoin;
	}

}
