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
package dimawo.simulation.middleware;

import java.util.Collection;
import java.util.TreeSet;

public class MiddlewareDescription {
	private TreeSet<String> hosts;
	
	public MiddlewareDescription() {
		hosts = new TreeSet<String>();
	}
	
	public void addHost(String name) throws Exception {
		if(! hosts.add(name))
			throw new Exception("Host already handled by middleware");
	}

	public Collection<String> getHosts() {
		return hosts;
	}
}
