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
package dimawo.agents;

import java.io.PrintStream;

/**
 * @author Gerard Dethier
 * 
 * A special exception class used by the agents in order to easily track the
 * source (i.e. the component that caused the error) of the error in the context
 * of many nested components.
 * <p>
 * This class essentially overrides stack trace printing methods in order to
 * add to a throwable's stack trace, an additional stack representing the
 * nesting of components identified by their name (see
 * {@link ErrorHandler} class). For example, if a component
 * A is nested into
 * a component B (but may be nested into another component) and an uncaught
 * exception is produced by the execution of component A, an
 * <code>UncaughtThrowable</code> stack trace will contain informations about
 * the fact that A is nested into B, in addition to the traditional stack trace.
 */
public class UncaughtThrowable extends Exception {
	private static final long serialVersionUID = 1L;

	/** The unhandled <code>Throwable</code>. */
	protected Throwable t;
	
	/** The name of the component that did not handle the Throwable. */
	protected String componentName;
	
	/**
	 * Constructs an <code>UncaughtThrowable</code> in function of given
	 * <code>Throwable</code> and component name.
	 * 
	 * @param t The <code>Throwable</code>.
	 * @param childName The name of source component.
	 */
	public UncaughtThrowable(Throwable t, String childName) {
		this.t = t;
		this.componentName = childName;
	}
	
	
	/**
	 * Returns the name of the source component.
	 * @return The name of the source component.
	 */
	public String getComponentName() {
		return componentName;
	}

	
	@Override
	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Prints the stack trace to the specified print stream.
	 * This method is equivalent to <code>printAgentStackTrace(out)</code> call.
	 * 
	 * @see #printAgentStackTrace(PrintStream)
	 */
	@Override
	public void printStackTrace(PrintStream out) {
		printAgentStackTrace(out);
	}

	/**
	 * Prints the source component and the stack trace associated to
	 * the unhandled <code>Throwable</code>. Note that the unhandled
	 * Throwable may also be an <code>UncaughtThrowable</code> thus,
	 * this method may display a stack of components before throwable's
	 * stack trace is actually printed. The lowest component of the stack
	 * is the most deeply nested component. 
	 * 
	 * @param out The print stream the stack trace is printed to.
	 */
	private void printAgentStackTrace(PrintStream out) {
		out.println("-> " + componentName);
		t.printStackTrace(out);
	}
}
