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
package dimawo.agents.events;

import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An instance of this class represents a call to be submitted to a handler
 * (for example, an {@link dimawo.agents.AbstractAgent AbstractAgent} instance).
 * <p>
 * This call can be asynchronously handled, methods ({@link #waitOn() waitOn()}
 * and {@link #waitOn(long) waitOn(long)}) are therefore provided
 * to give the ability of waiting for the call to be handled.
 * <p>
 * The handling of a call can be successful or not. In case of success,
 * a result can be attached. The result of a call is defined by a
 * subclasses of AsynchronousCall.
 * 
 * @author DiMaWo Team
 * @serial exclude
 *
 */
public class AsynchronousCall implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The Exception generated during call's handling. This field is set only
	 * after the call has been handled.
	 * @see #getError() */
	private Exception error;
	/** The Semaphore used to implement synchronization on call handling.
	 * @see #waitOn()
	 * @see #waitOn(long) */
	private transient Semaphore sync;

	/** Constructor. Instantiates an AsynchronousCall ready to be submitted to
	 * a handler. */
	public AsynchronousCall() {
		sync = new Semaphore(0);
	}
	
	/**
	 * Waits until the associated instance has been handled.
	 * 
	 * @throws InterruptedException Thrown if the thread executing this method
	 * is interrupted before the method returns.
	 */
	public void waitOn() throws InterruptedException {
		sync.acquire();
		sync.release();
	}
	
	/**
	 * Waits until the associated instance has been handled or a time-out
	 * occurs.
	 * 
	 * @param millis The maximum number of milliseconds this method waits. If
	 * the call was not handled <code>millis</code> milliseconds after this
	 * method was called, a TimeoutException is thrown. In this case, no result may
	 * be associated to the call.
	 * 
	 * @throws InterruptedException Thrown if the thread executing this method
	 * is interrupted before the method returns.
	 * @throws TimeoutException Thrown if the call is not handled
	 * a given number of milliseconds after this method was called.
	 */
	public void waitOn(long millis) throws InterruptedException, TimeoutException {
		if( ! sync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException();
		}
		sync.release();
	}
	
	/**
	 * Used by the handler to signal the call has been successfully handled.
	 * A thread's execution paused by a call to {@link #waitOn() waitOn()} or
	 * {@link #waitOn(long) waitOn(long)} is then resumed.
	 */
	public void signalSuccess() {
		sync.release();
	}

	/**
	 * Indicates if the call was successfully handled.
	 * 
	 * @return True if the call was successfully handled or false otherwise.
	 */
	public boolean isSuccessful() {
		return error == null;
	}
	
	/**
	 * Used by the handler to signal an error occured while the call was handled.
	 * A thread's execution paused by a call to {@link #waitOn() waitOn()} or
	 * {@link #waitOn(long) waitOn(long)} is then resumed.
	 * @param error The Exception that was thrown while the call was handled.
	 */
	public void signalError(Exception error) {
		this.error = error;
		sync.release();
	}
	
	/**
	 * Throws the Exception that was thrown while the call was handled.
	 * @throws Exception The Exception that was thrown while the call was handled.
	 */
	public void throwError() throws Exception {
		throw error;
	}
	
	/**
	 * Returns the Exception that was thrown while the call was handled.
	 * @return The Exception that was thrown while the call was handled.
	 */
	public Exception getError() {
		return error;
	}
	
	/**
	 * Sets the Exception thrown while the call was handled.
	 * @param e The Exception that was thrown while the call was handled.
	 */
	public void setError(Exception e) {
		this.error = e;
	}

}
