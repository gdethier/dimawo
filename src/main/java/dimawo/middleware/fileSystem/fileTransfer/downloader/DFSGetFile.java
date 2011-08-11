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
package dimawo.middleware.fileSystem.fileTransfer.downloader;

import java.io.File;

import dimawo.fileTransfer.FileTransferMessageFactory;
import dimawo.fileTransfer.client.FileTransferClientCallBack;
import dimawo.fileTransfer.client.events.GetFile;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.fileSystem.fileTransfer.DFSMessageFactory;



public class DFSGetFile implements GetFile {
	
	private DAId serverId;
	private String fileUID;
	private File destFile;
	private FileTransferClientCallBack cb;
	
	public DFSGetFile(DAId serverId, String fileUID, File destFile, FileTransferClientCallBack cb) {
		this.serverId = serverId;
		this.fileUID = fileUID;
		this.destFile = destFile;
		this.cb = cb;
	}

	@Override
	public DAId getServerDaId() {
		return serverId;
	}

	@Override
	public File getDestFile() {
		return destFile;
	}

	@Override
	public String getFileUID() {
		return fileUID;
	}

	@Override
	public FileTransferMessageFactory getMessageFactory() {
		return DFSMessageFactory.getInstance();
	}

	@Override
	public FileTransferClientCallBack getGetFileCallBack() {
		return cb;
	}

	@Override
	public boolean isFileName() {
		return false;
	}

}
