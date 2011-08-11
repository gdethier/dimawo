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
package dimawo.fileTransfer;

import dimawo.fileTransfer.client.messages.ChunkMessage;
import dimawo.fileTransfer.client.messages.ErrorMessage;
import dimawo.fileTransfer.client.messages.PingClientMessage;
import dimawo.fileTransfer.server.messages.GetFileRequest;
import dimawo.fileTransfer.server.messages.GetNextChunkRequest;
import dimawo.fileTransfer.server.messages.PingServerMessage;

/**
 * This interface describes the methods that must be implemented by a file
 * transfer message factory.
 * <p>
 * The message factory is required in order to
 * instantiate messages that will be correctly routed on reception. Indeed,
 * there are 2 types of routing implemented by
 * {@link dimawo.middleware.communication.Communicator Communicator} and 
 * {@link dimawo.middleware.distributedAgent.DistributedAgent DistributedAgent}
 * (see {@link be.ulg.montefiore.dimawo.middleware.communication.Communicator.submitIncomingMessage(Message)
 * Communicator's message handling method}).
 * In the case where routing is delegated to DistributedAgent, the type (i.e.
 * the class) of a message is used to route it to the good agent. The message
 * factory is therefore used by file transfer components in order to instantiate
 * messages with the right type in order to be routed correctly.
 * 
 * @author Gerard Dethier
 */
public interface FileTransferMessageFactory {
	public GetFileRequest newGetFileRequest(String fileUID, boolean isFileName);
	public GetNextChunkRequest newGetNextChunkMessage(String fileUID);
	public ChunkMessage newChunkMessage(String fileUID, byte[] data, boolean isLast);
	public ErrorMessage newErrorMessage(String msg, String fileUID);
	public PingClientMessage newPingClientMessage();
	public PingServerMessage newPingServerMessage();
}
