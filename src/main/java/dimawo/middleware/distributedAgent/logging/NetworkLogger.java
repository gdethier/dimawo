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
package dimawo.middleware.distributedAgent.logging;

import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.distributedAgent.DAId;

/**
 * A NetworkLogger sends all logged messages to a remote host. The remote
 * host should run an application able to receive these messages and handle
 * them (for example, by printing them into a file).
 * 
 * @author Gerard Dethier
 */
public class NetworkLogger implements LoggerInterface {
	/** The communicator used to send log messages. */
	private CommunicatorInterface com;
	/** The ID of destination host log messages are sent to. */
	private DAId dest;
	
	/**
	 * Instantiates a NetworkLogger that sends log messages to
	 * a host described by given host name and server port.
	 * 
	 * @param hostName Log messages destination host name
	 * @param port Log messages destination server port
	 */
	public NetworkLogger(String hostName, int port) {
		this.dest = new DAId(hostName, port, 0);
	}

	/**
	 * Instantiates a NetworkLogger that sends log messages to
	 * a host described by a given ID.
	 * 
	 * @param dest Log messages destination ID
	 */
	public NetworkLogger(DAId dest) {
		this.dest = dest;
	}

	@Override
	public void log(String id, String msg) {
		com.sendDatagramMessage(new NetworkLoggerMessage(dest, id, msg));
	}

	@Override
	public void setCommunicator(CommunicatorInterface com) {
		this.com = com;
	}

}
