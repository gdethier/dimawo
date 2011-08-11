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
package dimawo.middleware.fileSystem.fileTransfer.downloader.messages;

import dimawo.fileTransfer.client.messages.ChunkMessage;
import dimawo.middleware.distributedAgent.DAId;


public class FileChunk extends FileDownloaderMessage implements ChunkMessage {

	private static final long serialVersionUID = 1L;

	protected String fileUID;
	protected byte[] fileChunk;
	protected boolean isLastChunk;
	
	/**
	 * Constructor used when data is transfered.
	 * @param t
	 * @param fileUID
	 * @param fileChunk
	 * @param isLastChunk
	 */
	public FileChunk(String fileUID, byte[] fileChunk, boolean isLastChunk) {
		this.fileUID = fileUID;
		this.fileChunk = fileChunk;
		this.isLastChunk = isLastChunk;
	}

	public String getFileUID() {
		return fileUID;
	}

	public boolean isLast() {
		return isLastChunk;
	}

	@Override
	public DAId getServerDaId() {
		return this.getSender();
	}

	@Override
	public byte[] getData() {
		return fileChunk;
	}

}
