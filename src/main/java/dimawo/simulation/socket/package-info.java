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
 * Contains the implementation of virtal and real sockets used by simulated
 * or non simulated processes to communicate.
 * DiMaWo's sockets are instantiated using a
 * {@link dimawo.simulation.socket.SocketFactory socket factory}.
 * This factory is given to the
 * {@link dimawo.middleware.communication.Communicator Communicator}
 * which instantiates real or
 * virtual sockets in order to communicate in function of the factory's
 * configuration.
 * 
 * @see dimawo.simulation.socket.RealSocket
 * @see dimawo.simulation.socket.RealServerSocket
 * @see dimawo.simulation.socket.VirtualSocket
 * @see dimawo.simulation.socket.VirtualServerSocket
 * @see dimawo.simulation.host.VirtualHost
 * @see dimawo.simulation.cluster.VirtualCluster
 */
package dimawo.simulation.socket;