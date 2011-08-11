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
package dimawo.middleware.fileSystem.fileTransfer.messages;

import dimawo.fileTransfer.client.messages.ErrorMessage;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.fileSystem.fileTransfer.downloader.messages.FileDownloaderMessage;

public class DFSErrorMessage extends FileDownloaderMessage implements
		ErrorMessage {
	private static final long serialVersionUID = 1L;
	private String msg;
	private String fileUID;

	public DFSErrorMessage(String msg, String fileUID) {
		this.msg = msg;
		this.fileUID = fileUID;
	}

	public DFSErrorMessage(DAId t, String msg, String fileUID) {
		super(t);
		this.msg = msg;
		this.fileUID = fileUID;
	}

	@Override
	public String getMessage() {
		return msg;
	}
	
	@Override
	public String getFileUID() {
		return fileUID;
	}

	@Override
	public DAId getServerId() {
		return this.getSender();
	}

}
