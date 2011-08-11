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
package dimawo.fileTransfer.client;

import java.io.File;

import dimawo.middleware.distributedAgent.DAId;



public class GetFileCallBack {
	
	public enum Error {noerror, serverDown, fileNotFound};
	
	private DAId serverDaId;
	private String fileUID;
	private File file;

	private Error error;
	
	public GetFileCallBack(DAId serverDaId, String fileUID, Error error) {
		this.serverDaId = serverDaId;
		this.fileUID = fileUID;
		this.error = error;
	}

	public GetFileCallBack(DAId serverDaId, String fileUID, File file) {
		this.serverDaId = serverDaId;
		this.fileUID = fileUID;
		this.file = file;
		this.error = Error.noerror;
	}
	
	public DAId getServerDaId() {
		return serverDaId;
	}
	
	public String getFileUID() {
		return fileUID;
	}

	public File getFile() {
		return file;
	}
	
	public boolean isSuccessful() {
		return error.equals(Error.noerror);
	}
	
	public Error getError() {
		return error;
	}

}
