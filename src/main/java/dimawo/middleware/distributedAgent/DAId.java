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
package dimawo.middleware.distributedAgent;

import java.io.Serializable;

public class DAId implements Comparable<DAId>, Serializable {

	private static final long serialVersionUID = 1L;

	private String hostName;
	private int port;
	private long timeStamp;
	
	public DAId(String hostName, int port, long timeStamp) {
		if(hostName == null)
			throw new NullPointerException();
		this.hostName = hostName;
		this.port = port;
		this.timeStamp = timeStamp;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public int getPort() {
		return port;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public int compareTo(DAId o) {
		if(port != o.port) {
			return port - o.port;
		} else if(
//				timeStamp != 0 && o.timeStamp != 0 &&
				timeStamp != o.timeStamp) {
			return (int) (timeStamp - o.timeStamp);
		} else {
			return hostName.compareTo(o.hostName);
		}
	}
	
	public boolean equals(Object o) {
		if(! (o instanceof DAId))
			return false;
		
		DAId id = (DAId) o;
		return compareTo(id) == 0;
	}
	
	public String toString() {
		return hostName+":"+port+":"+timeStamp;
	}

}
