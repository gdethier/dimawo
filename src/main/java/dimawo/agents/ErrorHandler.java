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

/**
 * @author Gerard Dethier
 * 
 * An error handler can be notified about the occurrence of an error. The error
 * may be caused by another component. The component that caused the error
 * is described by a string (for example, a class name).
 *
 */
public interface ErrorHandler {

	/**
	 * Signals an error.
	 * 
	 * @param t The error.
	 * @param errorSourceId A name describing the error's source i.e. the
	 * component that caused the error.
	 */
	public void signalChildError(Throwable t, String errorSourceId);
}
