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
package dimawo.middleware.fileSystem.fileTransfer.uploader;

import java.io.File;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.fileTransfer.server.FileProvider;
import dimawo.fileTransfer.server.FileTransferServerAgent;
import dimawo.fileTransfer.server.messages.SimpleFTPServerMessage;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.fileSystem.FileSystemAgent;
import dimawo.middleware.fileSystem.fileDB.FileDescriptor;
import dimawo.middleware.fileSystem.fileDB.LocalFilesDB;
import dimawo.middleware.fileSystem.fileTransfer.downloader.messages.FileDownloaderMessage;
import dimawo.middleware.fileSystem.fileTransfer.uploader.messages.FileUploaderMessage;



public class FileUploader extends LoggingAgent
implements FileProvider, MessageHandler {
	
	private FileTransferServerAgent server;
	private FileSystemAgent dfs;
	protected LocalFilesDB fileDB;
	private DistributedAgent da;


	public FileUploader(FileSystemAgent dfsPeer,
			int chunkSize, int maxSimUploads) {

		super(dfsPeer, "FileUploader");
		
		da = dfsPeer.getHostingDA();
		server = new FileTransferServerAgent(this, "FileUploaderServer");
		server.setFileProvider(this);
		this.dfs = dfsPeer;
		fileDB = dfsPeer.getFileDB();

		setPrintStream(dfsPeer.getHostingDA().getFilePrefix());
		server.setPrintStream(dfsPeer.getHostingDA().getFilePrefix());
		
	}
	
	////////////////////
	// Public methods //
	////////////////////

	/**
	 * Submits a FileUploaderMessage.
	 * 
	 * @param msg
	 * 
	 * @throws InterruptedException
	 */
	public void submitFileUploaderMessage(FileUploaderMessage msg) throws InterruptedException {
		
		submitMessage(msg);
		
	}
	
	
	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
		
		da.getCommunicator().registerMessageHandler(
				FileDownloaderMessage.getHandlerId(da.getDaId()), this);
		
		try {
			server.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void init() throws Exception {
		agentPrintMessage("init");
		da.getCommunicator().registerMessageHandler(
				FileUploaderMessage.getHandlerId(da.getDaId()), this);
		server.setCommunicator(dfs.getHostingDA().getCommunicator());
		server.start();
	}
	
	protected void handleMessage(Object o) throws Exception {
		if(o instanceof SimpleFTPServerMessage) {
			server.submitServerMessage((SimpleFTPServerMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}


	/////////////////////
	// Private methods //
	/////////////////////

	@Override
	public File getFile(String fileUID) {
		FileDescriptor fileDesc = fileDB.getFileDescriptor(fileUID);
		if(fileDesc != null)
			return fileDesc.getFile();
		return null;
	}

	@Override
	public void submitIncomingMessage(Message msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
