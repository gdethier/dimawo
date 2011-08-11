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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DownloadOutputFile {

	protected String fileUID;
	protected File file;
	protected OutputStream os;
	protected int sourceDa;
	private FileDownloaderCallBack cb;
	
	public DownloadOutputFile(String fileUID, File file, int sourceDa, FileDownloaderCallBack cb)
	throws FileNotFoundException {
		this.fileUID = fileUID;
		this.file = file;
		this.os = new FileOutputStream(file);
		this.sourceDa = sourceDa;
		this.cb = cb;
	}

	public OutputStream getOutputStream() {
		return os;
	}
	
	public String getFileUID() {
		return fileUID;
	}

	public File getFile() {
		return file;
	}
	
	public int getSourceDa() {
		return sourceDa;
	}
	
	public FileDownloaderCallBack getCallBack() {
		
		return cb;
		
	}
	
}
