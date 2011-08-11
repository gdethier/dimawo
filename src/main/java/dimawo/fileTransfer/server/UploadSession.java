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
package dimawo.fileTransfer.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import dimawo.fileTransfer.FileTransferMessageFactory;
import dimawo.fileTransfer.server.messages.GetFileRequest;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;



public class UploadSession {
	
	private FileTransferServerAgent server;

	private int maxChunkSize;
	private MOSAccessorInterface access;
	private FileProvider fileProv;
	
	private GetFileRequest initReq;

	private FileTransferMessageFactory mFact;
	private String fileUID;
	private File file;
	private int availableBytes;
	private FileInputStream fis;
	
	private LinkedList<GetFileRequest> pendingRequests;


	public UploadSession(
			FileTransferServerAgent server,
			int maxChunkSize,
			FileProvider fileProv,
			GetFileRequest req) {
		this.server = server;
		this.maxChunkSize = maxChunkSize;
		this.fileProv = fileProv;
		initReq = req;
		
		pendingRequests = new LinkedList<GetFileRequest>();
	}
	
	public void setAccess(MOSAccessorInterface access) throws IOException {
		this.access = access;
		
		if(! initUpload(initReq)) {
			prepareNextUpload();
		}
		initReq = null;
	}


	public void sendNextChunk() throws IOException {
		int chunkSize = Math.min(availableBytes, maxChunkSize);
		byte[] data = new byte[chunkSize];
		fis.read(data);
		availableBytes -= chunkSize;
		boolean isLast = (availableBytes == 0);
		
		Message m = (Message) mFact.newChunkMessage(fileUID, data, isLast);
		m.setCallBack(server);
		access.writeNonBlockingMessage(m);
		
		if(isLast) {
			fis.close();
			
			prepareNextUpload();
		}
	}

	private void prepareNextUpload() throws IOException {
		fis = null;
		file = null;

		while(! pendingRequests.isEmpty()) {
			GetFileRequest req = pendingRequests.removeFirst();
			if(initUpload(req))
				break;
		}
	}

	private boolean initUpload(GetFileRequest req) throws IOException {
		mFact = req.getMessageFactory();
		fileUID = req.getFileUID();
		if(req.fileNameIsGiven())
			file = new File(req.getFileUID());
		else
			file = fileProv.getFile(fileUID);
		
		if(file == null || ! file.exists()) {
			String msg = "File "+fileUID+" not found.";
			printMessage(msg);
			try {
				Message m = (Message) mFact.newErrorMessage(msg, fileUID);
				access.writeNonBlockingMessage(m);
				access.close();
			} catch (IOException e) {
			} catch (InterruptedException e) {
			}
			
			return false;
		} else {
			availableBytes = (int) file.length();
			fis = new FileInputStream(file);
			
			sendNextChunk();
			
			return true;
		}
	}

	private void printMessage(String msg) {
		server.agentPrintMessage("[UploadSession] "+msg);
	}

	public void queueRequest(GetFileRequest o) {
		pendingRequests.addLast(o);
	}


	public void close() {
		if(fis != null) {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fis = null;
		}
		
		if(access != null) {
			try {
				access.close();
			} catch (IOException e) {
			} catch (InterruptedException e) {
			}
		}
	}

	public boolean isActive() {
		return fis != null;
	}

	public void pingClient() {
		if(access != null)
			access.writeNonBlockingMessage((Message) mFact.newPingClientMessage());
	}
}
