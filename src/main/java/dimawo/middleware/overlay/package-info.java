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
/**
 * Contains all interfaces related to the overlay agent. An overlay is a network
 * built on top of another network. In the context of DiMaWo, we consider the
 * Distributed Agents are part of an overlay network built on top of the
 * TCP/IP protocol. The overlay agent handles all common overlay related operations
 * (join, leave, etc.) as well as higher level services particular to
 * DiMaWo:
 * <ul>
 * <li>shared-map</li>
 * <li>broadcast</li>
 * <li>leader election</li>
 * <li>barrier synchronization</li>
 * <li>failure detection</li>
 * </ul>
 * <p>
 * The interfaces related to above high level services are also provided in
 * this package.
 */
package dimawo.middleware.overlay;
