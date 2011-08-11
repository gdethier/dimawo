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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import dimawo.agents.AgentException;
import dimawo.simulation.ClusterException;
import dimawo.simulation.HostDescription;
import dimawo.simulation.host.VirtualHost;
import dimawo.simulation.middleware.VirtualTask;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.net.VirtualNetwork;




public class VirtualCluster {
	private VirtualNetwork net;
	private TreeMap<String, VirtualHost> reliableHosts;
	private TreeMap<String, VirtualHost> hosts;
	
	public VirtualCluster() {
		net = new VirtualNetwork();
		reliableHosts = new TreeMap<String, VirtualHost>();
		hosts = new TreeMap<String, VirtualHost>();
	}
	
	public synchronized void addHost(String hostName) throws ClusterException, NetworkException {
		if(hosts.containsKey(hostName) || reliableHosts.containsKey(hostName))
			throw new ClusterException("Host already part of the cluster: "+hostName);

		VirtualHost host = new VirtualHost(hostName, net);
		hosts.put(hostName, host);
		try {
			host.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void addReliableHost(String hostName) throws ClusterException, NetworkException {
		if(hosts.containsKey(hostName) || reliableHosts.containsKey(hostName))
			throw new ClusterException("Host already part of the cluster: "+hostName);

		VirtualHost host = new VirtualHost(hostName, net);
		reliableHosts.put(hostName, host);
		try {
			host.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void removeHost(String hostName) throws ClusterException, InterruptedException {
		if(! hosts.containsKey(hostName) && ! reliableHosts.containsKey(hostName))
			throw new ClusterException("Host is not part of the cluster: "+hostName);
		
		VirtualHost host = hosts.remove(hostName);
		if(host == null)
			host = reliableHosts.remove(hostName);
		try {
			host.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		host.join();
	}
	
	private VirtualHost getHost(String hostName) {
		VirtualHost host = hosts.get(hostName);
		if(host == null)
			host = reliableHosts.get(hostName);
		return host;
	}

	public synchronized void executeProcessOnHost(String hostName,
			VirtualTask task) throws Exception {
		if(! hosts.containsKey(hostName) && ! reliableHosts.containsKey(hostName))
			throw new ClusterException("Host is not part of the cluster: "+hostName);
		
		VirtualHost host = getHost(hostName);
		host.runTask(task);
	}

	public synchronized void interruptProcessOnHost(String hostName) {
		VirtualHost host = getHost(hostName);
		if(host != null) {
			host.killTask();
		}
	}

	public synchronized void fail(int numOfFails) {
		Iterator<Entry<String, VirtualHost>> it = hosts.entrySet().iterator();
		int i = 0;
		while(it.hasNext() && i < numOfFails) {
			Entry<String, VirtualHost> e = it.next();
			
			System.out.println("Failure of "+e.getKey());
			try {
				e.getValue().stop();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (AgentException e1) {
				e1.printStackTrace();
			}

			it.remove();
			++i;
		}
	}

	public void configure(ClusterDescription clusterDesc) throws ClusterException, NetworkException {
		Collection<HostDescription> hosts = clusterDesc.getHosts();
		for(HostDescription desc : hosts) {
			// TODO : take into account failure parameters
			addHost(desc.getName());
		}
	}
}
