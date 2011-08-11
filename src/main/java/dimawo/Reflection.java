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
 * This class provides helpers that simplify the use of reflection.
 * 
 * @author Gerard Dethier
 *
 */
public class Reflection {
	
	/**
	 * Instantiates a new instance of a given class using the default
	 * class loader.
	 * 
	 * @param className A class name.
	 * 
	 * @return An instance of the given class.
	 * 
	 * @throws ReflectionException If an error occured during object's
	 * instantiation.
	 */
	public static Object newInstance(String className) throws ReflectionException {

		try {
			return ClassLoader.getSystemClassLoader().loadClass(className).newInstance();
		} catch (Exception e) {
			throw new ReflectionException(e);
		}

	}

}
