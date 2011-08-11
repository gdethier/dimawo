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
package dimawo.middleware.fileSystem.fileTransfer.downloader.events;

import dimawo.middleware.fileSystem.fileTransfer.downloader.FileDownloaderCallBack;


public class DownloadRequest {

	protected String fileUID;
	protected int sourceDa;
	private FileDownloaderCallBack cb;
	
	public DownloadRequest() {
		fileUID = null;
		sourceDa = -2;
	}
	
	public DownloadRequest(String fileUID, FileDownloaderCallBack cb) {
		this.fileUID = fileUID;
		sourceDa = -2;
		this.cb = cb;
	}

	public String getFileUID() {
		return fileUID;
	}
	
	public FileDownloaderCallBack getCallBack() {
		return cb;
	}
	
	public void setSourceDa(int id) {
		sourceDa = id;
	}

	public int getSourceDa() throws Exception {
		if(sourceDa < 0)
			throw new Exception("Source DA unknown");
		return sourceDa;
	}

	public boolean isCloseRequest() {
		return fileUID == null;
	}

}
