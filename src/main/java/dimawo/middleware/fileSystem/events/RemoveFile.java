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
package dimawo.middleware.fileSystem.events;

import dimawo.middleware.fileSystem.FileSystemCallBack;

public class RemoveFile {

	protected String fileUID;
	private String newFileName;
	private FileSystemCallBack cbH;
	
	public RemoveFile(String fileUID, String newFileName, FileSystemCallBack cbH) {
		this.fileUID = fileUID;
		this.newFileName = newFileName;
		this.cbH = cbH;
	}
	
	public String getFileUID() {
		return fileUID;
	}
	
	public String getNewFileName() {
		return newFileName;
	}
	
	public FileSystemCallBack getCallBack() {
		return cbH;
	}
	
}
