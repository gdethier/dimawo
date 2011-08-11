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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.fileTransfer.client.FileTransferClientAgent;
import dimawo.fileTransfer.client.FileTransferClientCallBack;
import dimawo.fileTransfer.client.GetFileCallBack;
import dimawo.fileTransfer.client.messages.FileTransferClientMessage;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.MessageHandler;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.fileSystem.DFSException;
import dimawo.middleware.fileSystem.FileSystemAgent;
import dimawo.middleware.fileSystem.fileDB.FileLocation;
import dimawo.middleware.fileSystem.fileDB.FileLocationUpdateValue;
import dimawo.middleware.fileSystem.fileDB.RemoteFilesDB;
import dimawo.middleware.fileSystem.fileTransfer.downloader.events.AddFileLocation;
import dimawo.middleware.fileSystem.fileTransfer.downloader.events.DownloadRequest;
import dimawo.middleware.fileSystem.fileTransfer.downloader.messages.FileDownloaderMessage;
import dimawo.middleware.overlay.SharedMapAgentInterface;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.SharedMapGetResult;
import dimawo.middleware.overlay.SharedMapPutResult;
import dimawo.middleware.overlay.SharedMapRemoveResult;
import dimawo.middleware.overlay.SharedMapUpdateResult;





public class FileDownloader extends LoggingAgent
implements SharedMapCallBackInterface, FileTransferClientCallBack, MessageHandler {
	private static final int MAX_GET_RETRIES = 3;
	private static final int WAIT_BEFORE_RETRY_GET = 5000;
	
	protected String workingDir;
	protected int maxSimDownloads;
	
	private DistributedAgent da;
	protected FileSystemAgent dfsPeer;
	protected CommunicatorInterface com;
	
	private RemoteFilesDB fileLocationsCache;
	
	private WaitingLocationDownloads waitingLocDown;
	private DownloadCallBacks waitingDown;

	/** Next ID for next downloaded file */
	private int currentDownID;
	private FileTransferClientAgent client;
	
	private TreeSet<DAId> serversBlackList;

	
	public FileDownloader(String workingDir, FileSystemAgent dfsPeer,
			int maxSimDownloads) {

		super(dfsPeer, "FileDownloader");

		this.workingDir = workingDir;
		this.maxSimDownloads = maxSimDownloads;

		this.da = dfsPeer.getHostingDA();
		this.dfsPeer = dfsPeer;
		
		fileLocationsCache = new RemoteFilesDB();
		
		setPrintStream(dfsPeer.getHostingDA().getFilePrefix());

		waitingLocDown = new WaitingLocationDownloads();
		waitingDown = new DownloadCallBacks();
		currentDownID = 0;

		client = new FileTransferClientAgent(this, "FileDownloaderClient");
		client.setPrintStream(dfsPeer.getHostingDA().getFilePrefix());
		
		serversBlackList = new TreeSet<DAId>();
	}


	////////////////////
	// Public methods //
	////////////////////
	
	/**
	 * Submits a FileDownloaderMessage to downloader.
	 */
	public void submitDownloaderMessage(FileDownloaderMessage m) throws InterruptedException {
		submitMessage(m);
	}
	
	/**
	 * @throws InterruptedException 
	 * 
	 */
	public void downloadFile(String fileUID, FileDownloaderCallBack cb) {
		try {
			submitMessage(new DownloadRequest(fileUID, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	//////////////////////////////////
	// AbstractAgent implementation //
	//////////////////////////////////

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
		
		da.getCommunicator().unregisterMessageHandler(
				FileDownloaderMessage.getHandlerId(da.getDaId()), this);
		
		try {
			client.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void init() throws Exception {
		agentPrintMessage("init");
		
		this.com = dfsPeer.getHostingDA().getCommunicator();
		com.registerMessageHandler(
				FileDownloaderMessage.getHandlerId(da.getDaId()), this);
		client.setCommunicator(com);
		client.start();
	}
	
	protected void handleMessage(Object o) throws Exception {
		
		if(o instanceof FileTransferClientMessage) {
			client.submitClientMessage((FileTransferClientMessage) o);
		} else if(o instanceof GetFileCallBack) {
			handleGetFileCallBack((GetFileCallBack) o);
		} else if(o instanceof DownloadRequest) {
			handleDownloadRequest((DownloadRequest) o);
		} else if(o instanceof SharedMapGetResult) {
			handleGetResult((SharedMapGetResult) o);
		} else if(o instanceof AddFileLocation) {
			handleAddFileLocation((AddFileLocation) o);
		} else {
			throw new UnknownAgentMessage(o);
		}

	}

	/////////////////////
	// Private methods //
	/////////////////////

	private void handleGetFileCallBack(GetFileCallBack o) {
		DAId serverId = o.getServerDaId();
		String fileUID = o.getFileUID();
		LinkedList<DownloadRequest> pend = waitingDown.dequeueRequests(serverId, fileUID);
		if(o.isSuccessful()) {
			agentPrintMessage("Download successful for file "+fileUID+" from "+serverId);
			File dest = o.getFile();
			signalDownloadFinished(fileUID, pend, dest);
		} else {
			agentPrintMessage("Download unsuccessful for file "+fileUID+" from "+serverId);
			if(o.getError().equals(GetFileCallBack.Error.serverDown)) {
				agentPrintMessage("-- adding "+serverId+" to blacklist.");
				serversBlackList.add(serverId);
			}
			handleDownloadFailure(serverId, fileUID, pend);
		}
	}


	private void handleDownloadFailure(DAId serverId, String fileUID,
			LinkedList<DownloadRequest> pend) {
		// Remove location from pFLT		
		FileLocation loc = fileLocationsCache.getLocation(fileUID);
		loc.removeRemoteFileLocation(serverId);
		
		// Remove location from gFLT
		SharedMapAgentInterface mapInt = da.getOverlayInterface().getMapInterface();
		String fileKey = FileSystemAgent.getFileKey(fileUID);
		FileLocationUpdateValue up = new FileLocationUpdateValue(fileUID);
		up.addLocationToRemove(serverId);
		mapInt.updateAsync(fileKey, up, null, this);
		
		if(pend.isEmpty())
			return;
		
		if(loc.isEmpty()) {
			agentPrintMessage("Request new locations for file "+fileUID);
			Iterator<DownloadRequest> it = pend.iterator();
			DownloadRequest req = it.next();
			if( ! waitingLocDown.queueRequest(req)) {
				da.getOverlayInterface().getMapInterface().getAsync(
						FileSystemAgent.getFileKey(fileUID), this);
			}
			
			while(it.hasNext()) {
				req = it.next();
				 waitingLocDown.queueRequest(req);
			}
		} else {
			agentPrintMessage("Restart downloads of file "+fileUID+
					" from another location");
			for(Iterator<DownloadRequest> it = pend.iterator(); it.hasNext();) {
				DownloadRequest req = it.next();
				
				DAId id = loc.getRandomLocation();
				startDownload(id, req);
			}
		}
	}


	private void signalDownloadFinished(String fileUID, LinkedList<DownloadRequest> pend,
			File dest) {
		for(Iterator<DownloadRequest> it = pend.iterator(); it.hasNext();) {
			DownloadRequest req = it.next();
			
			req.getCallBack().downloadFinished(fileUID, dest, null);
		}
	}

	/**
	 * Starts or queues the download request.
	 * 
	 * @param dr A download request.
	 * @throws Exception 
	 * 
	 */
	private void handleDownloadRequest(DownloadRequest dr) throws Exception {

		String fileUID = dr.getFileUID();

		agentPrintMessage("Download request for file "+fileUID);
		FileLocation loc = fileLocationsCache.getLocation(fileUID);
		if(loc == null) {
			agentPrintMessage("-- No cached location available.");
			if( ! waitingLocDown.queueRequest(dr)) {
				agentPrintMessage("-- Request locations from DMap.");
				da.getOverlayInterface().getMapInterface().getAsync(
						FileSystemAgent.getFileKey(fileUID), this);
			}
		} else {
			DAId sourceDAID = loc.getRandomLocation();
			startDownload(sourceDAID, dr);
		}
	}
	
	private void startDownload(DAId sourceDAID, DownloadRequest dr) {
		if(sourceDAID == null)
			throw new NullPointerException();
		
		String fileUID = dr.getFileUID();
		File destFile = new File(getNextFileName());
		
		waitingDown.queueRequest(sourceDAID, fileUID, dr);
		
		agentPrintMessage("Request download of file "+fileUID+" from "+sourceDAID);
		client.getFile(new DFSGetFile(sourceDAID, fileUID, destFile, this));
	}
	
	protected synchronized String getNextFileName() {
		return workingDir + (currentDownID++) + ".down";
	}

	private void handleGetResult(SharedMapGetResult getRes)
	throws Exception {
		String key = getRes.getKey();
		SharedMapEntry res = getRes.getEntry();

		String fileUID = FileSystemAgent.getFileUID(key);
		if(! waitingLocDown.containsQueue(fileUID)) {
			agentPrintMessage("No waiting location for "+fileUID);
			return;
		}
		
		if(res == null) {
			agentPrintMessage("No location found for file "+fileUID);
			if(! retryGetLocations(fileUID, key)) {
				signalDownloadFailureToCBs(key, fileUID);
			} else {
				agentPrintMessage("Retry get "+fileUID);
			}
		} else {
			agentPrintMessage("Got location for file "+fileUID);
			FileLocation loc = (FileLocation) res.getValue();
			assert fileUID.equals(loc.getFileUID());
			
			loc.removeRemoteFileLocations(serversBlackList);
			
			if(loc.isEmpty()) {
				agentPrintMessage("No more location available for file "+fileUID);
				if(! retryGetLocations(fileUID, key)) {
					signalDownloadFailureToCBs(key, fileUID);
				} else {
					agentPrintMessage("Retry get "+fileUID);
				}
			} else {
				agentPrintMessage("Start download for file "+fileUID);
				fileLocationsCache.updateFile(fileUID, loc);
				startWaitingDownloads(fileUID, loc);
			}
		}
	}

	/**
	 * 
	 * @param fileUID
	 * @param key
	 * @return True if locations are requested again, false otherwise.
	 */
	private boolean retryGetLocations(final String fileUID, String key) {
		if(waitingLocDown.newGetRetry(fileUID, MAX_GET_RETRIES)) {
			final FileDownloader down = this;
			new Thread() {
				public void run() {
					try {
						Thread.sleep(WAIT_BEFORE_RETRY_GET);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
					da.getOverlayInterface().getMapInterface().getAsync(
							FileSystemAgent.getFileKey(fileUID), down);
				}
			}.start();
			return true;
		}
		return false;
	}


	private void signalDownloadFailureToCBs(String key, String fileUID) {
		LinkedList<DownloadRequest> w = waitingLocDown.remove(fileUID);
		if(w != null) {
			Iterator<DownloadRequest> reqIt = w.iterator();
			while(reqIt.hasNext()) {
				DownloadRequest dr = reqIt.next();
				
				dr.getCallBack().downloadFinished(fileUID, null,
						new DFSException("Get transaction error with key "+
								key));
			}
		}
	}
	
	private void startWaitingDownloads(String fileUID, FileLocation loc) throws Exception {
		LinkedList<DownloadRequest> wait = waitingLocDown.remove(fileUID);
		if(wait == null)
			return;

		Iterator<DownloadRequest> it = wait.iterator();
		while(it.hasNext()) {
			DownloadRequest dr = it.next();
			DAId sourceDAID = loc.getRandomLocation();
			startDownload(sourceDAID, dr);
		}
	}

	public void addRemoteFileLocation(String fileUID, DAId id) {
		try {
			submitMessage(new AddFileLocation(fileUID, id));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handleAddFileLocation(AddFileLocation o) throws Exception {
		String fileUID = o.getFileUID();
		DAId daId = o.getDaId();

		FileLocation loc =
			fileLocationsCache.addRemoteFileLocation(fileUID, daId);

		startWaitingDownloads(fileUID, loc);
	}

	@Override
	public void sharedMapGetCallBack(SharedMapGetResult res) {
		try {
			submitMessage(res);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sharedMapPutCallBack(SharedMapPutResult res) {
		submitError(new Exception("Unhandled put result"));
	}


	@Override
	public void sharedMapRemoveCallBack(SharedMapRemoveResult res) {
		submitError(new Exception("Unhandled remove result"));
	}


	@Override
	public void sharedMapUpdateCallBack(SharedMapUpdateResult res) {
		// SKIP
	}

	@Override
	public void submitFile(GetFileCallBack cb) {
		try {
			submitMessage(cb);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
