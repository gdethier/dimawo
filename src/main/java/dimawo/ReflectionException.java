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
package dimawo;

/**
 * A reflection exception.
 * 
 * @author Gerard Dethier
 * 
 * @see dimawo.Reflection
 */
public class ReflectionException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 *  Constructs a new reflection exception with null as its detail message.
	 */
	public ReflectionException() {
	}

	/**
	 * Constructs a new reflection exception with the specified detail message.
	 * 
	 * @param m the detail message.
	 */
	public ReflectionException(String message) {
		super(message);
	}

	/**
	 * Constructs a new reflection exception with the specified detail message
	 * and cause.
	 * 
	 * @param m the detail message.
	 * @param cause the cause.
	 */
	public ReflectionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new reflection exception with the specified cause.
	 * 
	 * @param cause the cause.
	 */
	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
