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

import java.util.Timer;
import java.util.TimerTask;

/**
 * A DATimer is able to call a method of a DistributedAgent
 * after some amount of time. The delay and the method called depend on
 * the requested Timer.
 * 
 * Currently, only the initial timer used by a DA waiting its insertion
 * in the DA tree is implemented.
 * 
 * @author Gérard Dethier
 *
 */
public class DATimer {
	
	private DistributedAgent da;
	private long daTreeTO;
	
	private Timer daTreeTimer;
	
	
	public DATimer(DistributedAgent da, long daTreeTO) {

		this.da = da;
		this.daTreeTO = daTreeTO;

	}

}
