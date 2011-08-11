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
package dimawo.middleware.stabilizer;

import java.util.Timer;
import java.util.TimerTask;

public class Stabilizer {
	private StabilizerCallbackInterface cbInt;
	
	private long waitTime;
	private Timer stabTimer;
	private boolean isClosed;
	
	public Stabilizer(long stabTimeout, StabilizerCallbackInterface cbInt) {
		this.cbInt = cbInt;
		
		this.waitTime = stabTimeout;
		isClosed = false;
	}
	
	public synchronized void startStabilizer() {
		stabTimer = new Timer("Stabilizer timer", true);
		stabTimer.schedule(new TimerTask() {
			public void run() {
				stabTimer = null;
				timeOutHandling();
			}
		}, waitTime);
	}
	
	public synchronized void signalTopologyChange() {
		if(stabTimer != null) {
			stabTimer.cancel();
		}
		
		startStabilizer();
	}
	
	private synchronized void timeOutHandling() {
		if(! isClosed)
			cbInt.signalStableTopology(new StableTopology());
	}

	public synchronized void close() {
		isClosed = true;
		if(stabTimer != null)
			stabTimer.cancel();
	}
}
