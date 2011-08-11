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

import java.io.File;

import dimawo.fileTransfer.FileTransferMessageFactory;
import dimawo.fileTransfer.client.FileTransferClientCallBack;
import dimawo.fileTransfer.client.events.GetFile;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;



public class TestGetFile extends Message implements GetFile {

	private static final long serialVersionUID = 1L;
	
	private DAId serverDaId;
	private String fileUID;
	private File dest;
	private boolean isFileName;
	private FileTransferClientCallBack cb;

	
	public TestGetFile(DAId serverDaId, String fileUID,
			boolean isFileName,
			File dest,
			FileTransferClientCallBack cb) {
		this.serverDaId = serverDaId;
		this.fileUID = fileUID;
		this.isFileName = isFileName;
		this.dest = dest;
		this.cb = cb;
	}

	@Override
	public String getFileUID() {
		return fileUID;
	}

	@Override
	public DAId getServerDaId() {
		return serverDaId;
	}

	@Override
	public File getDestFile() {
		return dest;
	}

	@Override
	public FileTransferClientCallBack getGetFileCallBack() {
		return cb;
	}

	@Override
	public FileTransferMessageFactory getMessageFactory() {
		return TestFactory.getInstance();
	}

	@Override
	public boolean isFileName() {
		return isFileName;
	}

}
