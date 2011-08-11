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
package dimawo.middleware.communication;

import java.io.IOException;

import dimawo.agents.ErrorHandler;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.communication.outputStream.MessageOutputStream;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.simulation.socket.SocketFactory;



public interface CommunicatorInterface extends ErrorHandler {
	public SocketFactory getSocketFactory();
	public DAId getHostingDaId();
	
	public void submitIncomingMessage(Message tm);
	public void registerMessageHandler(Object messageHandlerId, MessageHandler mh);
	public void unregisterMessageHandler(Object messageHandlerId, MessageHandler mh);
	
	public void signalBrokenOutputStream(MessageOutputStream mos);
	public void signalClosedOutputStream(MessageOutputStream mos);
	
	public void asyncConnect(DAId daId, ConnectionRequestCallBack cb, MOSCallBack errCB, Object attachment) throws InterruptedException;
	public MOSAccessorInterface syncConnect(DAId daId, MOSCallBack errCB) throws InterruptedException, IOException;
	
	public void sendDatagramMessage(Message m);
	public void multicastMessage(DAId[] ids, Message msg);
	
	public void printMessage(String msg);
	public void printMessage(Throwable t);
}
