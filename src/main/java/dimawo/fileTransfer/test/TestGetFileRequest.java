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
package dimawo.fileTransfer.test;

import dimawo.fileTransfer.FileTransferMessageFactory;
import dimawo.fileTransfer.server.messages.GetFileRequest;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;

public class TestGetFileRequest extends Message implements GetFileRequest {

	private static final long serialVersionUID = 1L;
	private String fileUID;
	private boolean isFileName;

	public TestGetFileRequest(String fileUID, boolean isFileName) {
		this.fileUID = fileUID;
		this.isFileName = isFileName;
	}

	@Override
	public DAId getClientDaId() {
		return this.getSender();
	}

	@Override
	public String getFileUID() {
		return fileUID;
	}

	@Override
	public FileTransferMessageFactory getMessageFactory() {
		return TestFactory.getInstance();
	}

	@Override
	public boolean fileNameIsGiven() {
		return isFileName;
	}

}
