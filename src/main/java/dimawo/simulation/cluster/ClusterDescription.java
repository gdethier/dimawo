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
package dimawo.simulation.cluster;

import java.util.Collection;
import java.util.TreeMap;

import dimawo.simulation.HostDescription;


public class ClusterDescription {
	private TreeMap<String, HostDescription> hosts;

	public ClusterDescription() {
		hosts = new TreeMap<String, HostDescription>();
	}

	public void addHostDescription(HostDescription desc) throws Exception {
		if(hosts.containsKey(desc.getName()))
			throw new Exception("Host name already in use");

		hosts.put(desc.getName(), desc);
	}

	public HostDescription getHostDescription(String name) {
		return hosts.get(name);
	}
	
	public Collection<HostDescription> getHosts() {
		return hosts.values();
	}
}
