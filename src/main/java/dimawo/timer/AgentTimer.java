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
package dimawo.timer;

import java.util.Timer;
import java.util.TimerTask;

public class AgentTimer {

	/** Current Timer (if any). */
	private Timer timer;
	/** Agent to be notified in case of time-out. */
	private TimedAgent ta;


	/**
	 * Constructor.
	 * 
	 * @param ta Agent that will be notified when a time-out
	 * expires.
	 */
	public AgentTimer(TimedAgent ta) {

		this.ta = ta;

	}


	/**
	 * Starts this timer. If a timer was already running,
	 * it is canceled and a new timer is run with new
	 * time-out value.
	 * 
	 * @param timeOut Time-out given in milliseconds.
	 */
	public synchronized void setTimer(final long timeOut) {

		privateCancel();

		timer = new Timer("ResourceGraphBuilder timer", true);
		timer.schedule(new TimerTask() {

			public void run() {

				try {

					cancelTimer();
					ta.submitTimeOut(timeOut);

				} catch (InterruptedException e) {
				}
				
			}
			
		}, timeOut);
		
	}
	
	/**
	 * Cancels current timer. This method ensures a synchronized
	 * access to the timer in the canceling process.
	 * 
	 * @return True if the timer canceled. False if no timer was running.
	 */
	public synchronized boolean cancelTimer() {

		return privateCancel();

	}
	
	/**
	 * Cancels current timer. Synchronized access is not granted.
	 * 
	 * @return True if the timer canceled. False if no timer was running.
	 */
	private boolean privateCancel() {
		
		if(timer != null) {
			
			timer.cancel();
			timer = null;
			return true;
			
		}
		
		return false;
		
	}


	public synchronized boolean isSet() {
		return timer != null;
	}

}
