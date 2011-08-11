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

import java.io.Serializable;

public class SourceMn implements Serializable {
	private enum Source {childMn,parentMn,thisMn};
	
	private Source src;
	private int childIndex;
	
	public SourceMn() {
		src = null;
		childIndex = -1;
	}
	
	public void setParent() {
		src = Source.parentMn;
	}
	
	public boolean isParent() {
		return src.equals(Source.parentMn);
	}
	
	
	public void setThis() {
		src = Source.thisMn;
	}

	public boolean isThis() {
		return src.equals(Source.thisMn);
	}
	
	
	public void setChild(int childIndex) {
		src = Source.childMn;
		this.childIndex = childIndex;
	}
	
	public boolean isChild() {
		return src.equals(Source.childMn);
	}
	
	public int getChildIndex() {
		return childIndex;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(src.name());
		if(src.equals(Source.childMn)) {
			sb.append(", ").append(childIndex).append(']');
		} else {
			sb.append(']');
		}
		return sb.toString();
	}
}
