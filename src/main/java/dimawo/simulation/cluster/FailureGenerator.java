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

import java.util.Timer;
import java.util.TimerTask;

import dimawo.simulation.FailureParameters;



public class FailureGenerator {
	private FailureParameters params;
	private VirtualCluster cluster;

	public FailureGenerator(FailureParameters params, VirtualCluster cluster) {
		this.params = params;
		this.cluster = cluster;
	}
	
	public void start() {
		Timer t = new Timer(true);
		TimerTask task = new TimerTask() {
			public void run() {
				cluster.fail(1);
			}
		};
		
		int period = params.getPeriod();
		int delay = params.getDelay();
		if(period == 0 && delay > 0)
			t.schedule(task, delay);
		else if(period > 0 && delay > 0)
			t.schedule(task, delay, period);
	}
}
