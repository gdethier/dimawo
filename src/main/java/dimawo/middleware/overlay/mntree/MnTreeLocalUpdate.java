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

import dimawo.middleware.distributedAgent.DAId;



public class MnTreeLocalUpdate implements Serializable {
	public enum Cause {join, leave, move, init};
	public enum Target {thisMn, parentMn, childMn};
	public enum Action {add, remove, none};

	private DAId subject;
	private Cause cause;
	private Target target;
	private Action act;
	private MnPeerState state;
	
	public MnTreeLocalUpdate(DAId subject, Cause cause, Target targ, Action act, MnPeerState newState) {
		this.subject = subject;
		this.cause = cause;
		this.target = targ;
		this.act = act;
		this.state = newState;
		
		if(newState == null)
			throw new Error("A state must be given");
		
		if((cause.equals(Cause.join) ||
				cause.equals(Cause.leave) || 
				cause.equals(Cause.move)) && target == null)
			throw new Error("Target must be set");
	}
	
	public DAId getSubject() {
		return subject;
	}
	
	public Cause getCause() {
		return cause;
	}
	
	public Target getTarget() {
		return target;
	}
	
	public Action getAction() {
		return act;
	}

	public MnPeerState getNewState() {
		return state;
	}

	public boolean targetIsThisMn() {
		return target.equals(Target.thisMn);
	}
}
