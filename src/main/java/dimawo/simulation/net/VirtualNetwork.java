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
package dimawo.simulation.net;

import java.util.TreeMap;

import dimawo.agents.AgentException;
import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.host.events.NetworkEvent;




public class VirtualNetwork {
	
	private TreeMap<String, VirtualHost> hosts;
	
	public VirtualNetwork() {
		hosts = new TreeMap<String, VirtualHost>();
	}
	
	public synchronized void connectHost(VirtualHost host) throws NetworkException {
		String hostname = host.getHostName();
		if(hosts.containsKey(hostname)) {
			throw new NetworkException("Host "+hostname+" already present in network");
		}
		
		hosts.put(hostname, host);
	}
	
	public synchronized void disconnectHost(String hostname) throws NetworkException {
		VirtualHost host = hosts.remove(hostname);
		if(host == null) {
			throw new NetworkException("No host associated to name "+hostname);
		}
	}
	
	public synchronized VirtualHost getHost(String hostname) throws NetworkException {
		VirtualHost host = hosts.get(hostname);
		if(host == null)
			throw new NetworkException("No host associated to name "+hostname);
		
		return host;
	}
	
	public synchronized void routeEvent(NetworkEvent ne) throws NetworkException {
		String hostname = ne.getHostname();
		VirtualHost host = hosts.get(hostname);
		if(host == null) {
			throw new NetworkException("Unknown host: "+hostname);
		}

		host.putEvent(ne);
	}

	public synchronized void killHost(String hostName) {
		VirtualHost host = hosts.remove(hostName);
		if(host != null) {
			try {
				host.stop();
			} catch (InterruptedException e) {
			} catch (AgentException e) {
			}
		}
	}

}
