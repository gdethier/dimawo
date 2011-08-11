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
package dimawo.middleware.fileSystem;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.fileSystem.events.AddFile;
import dimawo.middleware.fileSystem.events.DeleteDFSFiles;
import dimawo.middleware.fileSystem.events.DownloadFinished;
import dimawo.middleware.fileSystem.events.GetFile;
import dimawo.middleware.fileSystem.events.RemoveFile;
import dimawo.middleware.fileSystem.events.ReplicateFile;
import dimawo.middleware.fileSystem.fileDB.FileDBException;
import dimawo.middleware.fileSystem.fileDB.FileDescriptor;
import dimawo.middleware.fileSystem.fileDB.FileLocation;
import dimawo.middleware.fileSystem.fileDB.FileLocationUpdateValue;
import dimawo.middleware.fileSystem.fileDB.LocalFilesDB;
import dimawo.middleware.fileSystem.fileDB.FileDescriptor.FileMode;
import dimawo.middleware.fileSystem.fileTransfer.downloader.FileDownloader;
import dimawo.middleware.fileSystem.fileTransfer.downloader.FileDownloaderCallBack;
import dimawo.middleware.fileSystem.fileTransfer.downloader.messages.FileDownloaderMessage;
import dimawo.middleware.fileSystem.fileTransfer.uploader.FileUploader;
import dimawo.middleware.fileSystem.fileTransfer.uploader.messages.FileUploaderMessage;
import dimawo.middleware.fileSystem.messages.FileLocationUpdate;
import dimawo.middleware.fileSystem.messages.FileReplicated;
import dimawo.middleware.fileSystem.messages.FileReplicationRequest;
import dimawo.middleware.fileSystem.messages.FileSystemMessage;
import dimawo.middleware.overlay.SharedMapCallBackInterface;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.SharedMapGetResult;
import dimawo.middleware.overlay.SharedMapPutResult;
import dimawo.middleware.overlay.SharedMapRemoveResult;
import dimawo.middleware.overlay.SharedMapResult;
import dimawo.middleware.overlay.SharedMapUpdateResult;
import dimawo.middleware.overlay.faultdetection.DetectorEvent;
import dimawo.middleware.overlay.faultdetection.FaultDetectionServiceCallBackInterface;




public class FileSystemAgent
extends LoggingAgent
implements FileDownloaderCallBack, MOSCallBack, SharedMapCallBackInterface, FaultDetectionServiceCallBackInterface {
	
	////////////////////
	// Static members //
	////////////////////

	public static String getFileKey(String fileUID) {

		return "File_"+fileUID;

	}
	
	public static String getFileUID(String key) {
		return key.substring(5);
	}
	
	public static Pattern convertPattern(Pattern fileUIDPat) {
		return Pattern.compile("File_"+fileUIDPat.pattern());
	}


	private DistributedAgent da;
	private CommunicatorInterface com;
	
	private LocalFilesDB localFiles;
	private FileDownloader downloader;
	private FileUploader uploader;
	
	private TreeMap<String, FileSystemCallBack> waitingInsertions;
	private TreeMap<String, FileReplicationInfo> awaitedReplications;

//	private HashMap<Message, WaitingList> waitingDel;

	// Downloads data
	private TreeMap<String, TreeSet<DAId>> filesToReplicate;
	private TreeMap<String, LinkedList<GetFile>> currentDownloads;


	public FileSystemAgent(DistributedAgent da,
			FileSystemAgentParameters fsParams) {

		super(da, "DFS_Peer");

		this.da = da;
		
		setPrintStream(da.getFilePrefix());
		
		localFiles = new LocalFilesDB(this);

		int maxSimDownloads = fsParams.getNumberOfDown();
		agentPrintMessage("Downloader can have "+maxSimDownloads+" simultaneous downloads.");
		downloader = new FileDownloader(da.getFilePrefix(), this, maxSimDownloads);

		int maxSimUploads = fsParams.getNumberOfUp();
		int chunkSize = fsParams.getChunkSize();
		agentPrintMessage("Uploader can have "+maxSimUploads+" simultaneous uploads and will send chunks of "+(chunkSize/1024)+" KB.");
		uploader = new FileUploader(this, chunkSize, maxSimUploads);
		
		waitingInsertions = new TreeMap<String, FileSystemCallBack>();
		awaitedReplications = new TreeMap<String, FileReplicationInfo>();
//		waitingDel = new HashMap<Message, WaitingList>();
		
		filesToReplicate = new TreeMap<String, TreeSet<DAId>>();
		currentDownloads = new TreeMap<String, LinkedList<GetFile>>();

	}

	
	////////////////////
	// Public methods //
	////////////////////
	
	/**
	 * Requests the list of files whose UID corresponds to the given
	 * UID pattern.
	 * 
	 * @param p A UID pattern.
	 * @param cbH A call-back handler.
	 * @throws InterruptedException 
	 */
//	public void listDFSFiles(Pattern uidPattern, FileSystemCallBack cbH) {
//	
//		try {
//			submitMessage(new ListDFSFiles(uidPattern, cbH));
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//	}

	/**
	 * Requests the deletion of files whose UID corresponds to the given
	 * UID pattern.
	 * 
	 * @param p A UID pattern.
	 * @param cbH A call-back handler.
	 * @throws InterruptedException 
	 */
	public void deleteDFSFiles(Pattern uidPattern, FileSystemCallBack cbH) {
	
		try {
			submitMessage(new DeleteDFSFiles(uidPattern, cbH));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Requests a file from a given file UID. The result will be a File object
	 * if the file was found.
	 * 
	 * @param fileUID
	 * @param cbH A call-back handler.
	 * @throws InterruptedException 
	 */
	public void getFile(String fileUID, FileSystemCallBack cbH) {
		
		try {
			submitMessage(new GetFile(fileUID, cbH));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Adds a local file to the distributed file system without replication.
	 * 
	 * @param fileUID The UID to associate to the file.
	 * @param localFile The file descriptor.
	 * @param mode The file's mode in the distributed file system.
	 * @throws Exception 
	 * @throws InterruptedException 
	 */
	public void addFile(String fileUID, File file, FileMode mode, FileSystemCallBack cb) throws Exception {
		
		if(fileUID == null || file == null || mode == null)
			throw new Exception("Invalid arguments");
		
		try {
			submitMessage(new AddFile(fileUID, file, mode, cb));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Removes a file from the distributed file system without deleting it.
	 * 
	 * @param cbH A call-back handler.
	 * @throws InterruptedException 
	 */
	public void removeFile(String fileUID, String newFileName, FileSystemCallBack cbH) {

		try {
			submitMessage(new RemoveFile(fileUID, newFileName, cbH));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Replicates a locally available file it in the DFS network.
	 * 
	 * @param fileUID The UID to associate to the file.
	 * @param cbH A call-back handler.
	 * @throws InterruptedException 
	 */
	public void replicateFile(String fileUID, Set<DAId> backupNeighbors,
			FileSystemCallBack cbH) {

		try {
			submitMessage(new ReplicateFile(fileUID, backupNeighbors, cbH));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	} 

	public LocalFilesDB getFileDB() {

		return localFiles;

	}
	
	
	public void submitFileSystemMessage(FileSystemMessage m) {

		try {
			submitMessage(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	
	/////////////////////////////////////////
	// FileDownloadCallBack implementation //
	/////////////////////////////////////////
	
	@Override
	public void downloadFinished(String fileUID, File file,
			DFSException error) {

		try {
			submitMessage(new DownloadFinished(fileUID, file, error));
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
		exitActions();
	}

	@Override
	protected void init() throws Exception {
		agentPrintMessage("init");
		
		com = da.getCommunicator();

		downloader.start();
		uploader.start();
	}
	
	@Override
	protected void handleMessage(Object o) throws Exception {

		if(o instanceof ReplicateFile) {

			handleReplicateFile((ReplicateFile) o);

		} else if(o instanceof GetFile) {

			handleGetFile((GetFile) o);

		} else if(o instanceof FileReplicationRequest) {

			handleFileReplicationRequest((FileReplicationRequest) o);

		} else if(o instanceof FileReplicated) {

			handleFileReplicated((FileReplicated) o);

		} else if(o instanceof FileLocationUpdate) {

			handleFileLocationUpdate((FileLocationUpdate) o);

		} else if(o instanceof FileDownloaderMessage) {

			downloader.submitDownloaderMessage((FileDownloaderMessage) o);

		} else if(o instanceof FileUploaderMessage) {

			uploader.submitFileUploaderMessage((FileUploaderMessage) o);

		} else if(o instanceof AddFile) {

			handleAddFile((AddFile) o);

		} else if(o instanceof RemoveFile) {

			handleRemoveFile((RemoveFile) o);

		} else if(o instanceof DeleteDFSFiles) {

			handleDeleteDFSFiles((DeleteDFSFiles) o);

//		} else if(o instanceof ListDFSFiles) {
//
//			handleListDFSFiles((ListDFSFiles) o);

		} else if(o instanceof DeleteLocalFilesMessage) {

			handleDeleteLocalFilesMessage((DeleteLocalFilesMessage) o);

		} else if(o instanceof DownloadFinished) {

			handleDownloadFinished((DownloadFinished) o);

		} else if(o instanceof SharedMapResult) {

			handleDMapResult((SharedMapResult) o);

		} else if(o instanceof BrokenDA) {

			handleBrokenDA((BrokenDA) o);

//		} else if(o instanceof MessageSent) {
//
//			handleMessageSent((MessageSent) o);

		} else {

			throw new UnknownAgentMessage(o);

		}
		
	}
	
//	private void handleMessageSent(MessageSent o) throws Exception {
//		Message msg = o.getMessage();
//		
//		if(msg instanceof DeleteLocalFilesMessage) {
//			WaitingList wl = waitingDel.remove(msg);
//			if(wl != null)
//				wl.signalDel();
//			else
//				throw new Exception("Unknown message");
//		}
//	}

	private void handleDeleteLocalFilesMessage(DeleteLocalFilesMessage o) {
		Pattern p = o.getFileUIDPattern();
		deleteLocalFiles(p);
	}

	private void deleteLocalFiles(Pattern p) {
		// Delete local files and remove entries from DMap
		LinkedList<String> fileUIDs = localFiles.deleteFiles(p);
		for(Iterator<String> it = fileUIDs.iterator(); it.hasNext();) {
			String fileUID = it.next();
			da.getOverlayInterface().getMapInterface().removeAsync(getFileKey(fileUID), this);
		}
	}

	private void handleDMapResult(SharedMapResult result)
	throws Exception {
		
		if(result instanceof SharedMapUpdateResult) {
			
			SharedMapUpdateResult upRes = (SharedMapUpdateResult) result;
			String key = upRes.getKey();
			String fileUID = getFileUID(key);
			
			agentPrintMessage("Locations of file "+fileUID+" successfuly updated.");

			// Notify file added
			FileSystemCallBack cb = waitingInsertions.remove(fileUID);
			if(cb != null) {
				agentPrintMessage("-- add successful");
				FileDescriptor desc = localFiles.getFileDescriptor(fileUID);
				if(desc != null)
					cb.addFileCB(null, fileUID, desc.getFile(), null);
				else
					throw new Exception("File "+fileUID+" not available anymore locally");
			}
			
			// Notify file replicated
			TreeSet<DAId> daSourceIds = filesToReplicate.remove(fileUID);
			if(daSourceIds != null) {

				Iterator<DAId> it = daSourceIds.iterator();
				while(it.hasNext()) {

					DAId daSourceId = it.next();
					agentPrintMessage("-- replication signaled to "+daSourceId);
					com.sendDatagramMessage(new FileReplicated(daSourceId, fileUID));

				}

			}
			
		} else if(result instanceof SharedMapRemoveResult) {
			// SKIP
		} else {
			throw new Exception("Unhandled result: "+result.getClass().getName());
		}
		
	}

	/////////////////////
	// Private methods //
	/////////////////////

//	private void handleListDFSFiles(ListDFSFiles gfl) throws Exception {
//
//		Pattern p = gfl.getPattern();
//		agentPrintMessage("Listing files "+p.pattern());
//
//		Pattern keyPattern = convertPattern(p);
//		
//		WaitingList wl = new WaitingList(p, gfl.getCallBack());
//		waitingList.put(nextListId, wl);
//		
//		da.submitRequest(new ListKeysLocalRequest(keyPattern, nextListId), this);
//		
//		++nextListId;
//		
//	}

	private void handleBrokenDA(BrokenDA bd) throws AgentException, InterruptedException {

		DAId daId = bd.getDAId();

		clearBrokenDaFromFilesToReplicate(daId);
	}
	
	private void clearBrokenDaFromFilesToReplicate(DAId daId) throws AgentException, InterruptedException {

		Iterator<Entry<String, TreeSet<DAId>>> it =
			filesToReplicate.entrySet().iterator();
		while(it.hasNext()) {

			Entry<String, TreeSet<DAId>> e = it.next();
			TreeSet<DAId> sources = e.getValue();
			if(sources.remove(daId) && sources.isEmpty()) {

				it.remove();

			}

		}
		
		Iterator<Entry<String, FileReplicationInfo>> it2 =
			awaitedReplications.entrySet().iterator();
		while(it2.hasNext()) {

			Entry<String, FileReplicationInfo> e = it2.next();
			FileReplicationInfo rd = e.getValue();

			rd.signalFailure(daId,
					da.getOverlayInterface().getPingServiceInterface());
			
			if(! rd.cbsArePending()) {
				it2.remove();
			}
		}
		
	}

	private void handleRemoveFile(RemoveFile rr) throws Exception {
		
		String fileUID = rr.getFileUID();
		agentPrintMessage("Remove reference to file "+rr.getFileUID());

		FileSystemCallBack cb = rr.getCallBack();
		FileDescriptor desc = localFiles.removeFile(rr.getFileUID());
		if(desc == null) {
			if(cb != null) {
				cb.removeFileCB(new DFSException("File not available locally"),
						fileUID, null);
			}
		} else {
			
			FileLocationUpdateValue update = new FileLocationUpdateValue(fileUID);
			update.addLocationToRemove(da.getDaId());
//			da.submitRequest(new UpdateLocalRequest(getFileKey(fileUID),
//					update), this);
			da.getOverlayInterface().getMapInterface().updateAsync(getFileKey(fileUID), update, null, this);
			
			
			String newFileName = rr.getNewFileName();
			File localFile = desc.getFile();
			File newFile = new File(newFileName);
			if(localFile.renameTo(newFile)) {
				cb.removeFileCB(null, fileUID, newFile);
			} else {
				cb.removeFileCB(new DFSException("File could not be renamed"),
						fileUID, null);
			}
			
		}
			

	}

	private void handleDeleteDFSFiles(DeleteDFSFiles df) throws Exception {

		Pattern p = df.getPattern();
		agentPrintMessage("Delete files matching "+p.pattern());

		deleteLocalFiles(p);
		
		// Broadcast deletion request
		DeleteLocalFilesMessage delFiles = new DeleteLocalFilesMessage(p);
		delFiles.setCallBack(this);
		da.getOverlayInterface().getBroadcastInterface().broadcastMessage(delFiles);
		
//		WaitingList wl = new WaitingList(p, df.getCallBack());
//		waitingDel.put(delFiles, wl);
		
		df.getCallBack().deleteFilesCB(p);
		
	}

	private void handleReplicateFile(ReplicateFile sf) throws FileDBException, AgentException, InterruptedException, IOException {

		agentPrintMessage("Replicating file "+sf.getFileUID());

		String fileUID = sf.getFileUID();
		FileDescriptor desc = localFiles.getFileDescriptor(fileUID);
		if(desc == null) {
			ReplicateFileResult res = new ReplicateFileResult(sf.getFileUID(),
					new TreeSet<DAId>(), sf.getDestinations(),
					new DFSException("File is not available locally"));
			sf.getCallBack().replicateFileCB(res);

		} else {

			Set<DAId> dest = sf.getDestinations();
			if(dest == null || dest.size() == 0) {
				ReplicateFileResult res = new ReplicateFileResult(sf.getFileUID(),
						new TreeSet<DAId>(), dest,
						new DFSException("No replication destinations"));
				sf.getCallBack().replicateFileCB(res);

			} else {

				agentPrintMessage("Replication neighbors:");
				for(DAId daId : sf.getDestinations()) {
					agentPrintMessage("-- "+daId);
					this.da.getOverlayInterface().getPingServiceInterface().register(daId, this);
				}
				
				notifyFileForReplication(sf.getCallBack(), fileUID, dest);

			}
			
		}

	}
	
	private void handleAddFile(AddFile af) throws Exception {

		DAId daId = da.getDaId();
		String fileUID = af.getFileUID();
		File localFile = af.getFile();

		agentPrintMessage("Adding file "+fileUID);
		try {

			localFiles.addFile(fileUID, localFile, af.getFileMode());
			
			// Update locations
			FileLocationUpdateValue fl = new FileLocationUpdateValue(fileUID);
			fl.addLocationToAdd(daId);
			
			
			String key = getFileKey(fileUID);
			FileLocation loc = new FileLocation(fileUID);
			loc.addRemoteFileLocation(daId);
			SharedMapEntry entry = new SharedMapEntry(key, loc);
			da.getOverlayInterface().getMapInterface().updateAsync(getFileKey(fileUID), fl, entry, this);
			
			// Wait update successfull before signaling insertion
			FileSystemCallBack cb = af.getCallBack();
			if(cb != null)
				waitingInsertions.put(fileUID, cb);

		} catch (FileDBException e) {
			
			FileSystemCallBack cb = af.getCallBack();
			if(cb != null)
				cb.addFileCB(new DFSException("File already available locally"), fileUID, af.getFile(), af.getFileMode());
			
		}

	}

	private void handleGetFile(GetFile rf) throws Exception {

		String fileUID = rf.fileUID;
		FileDescriptor desc = localFiles.getFileDescriptor(fileUID);
		if(desc == null) {

			// No file descriptor available locally
			agentPrintMessage("Queuing download of file "+fileUID);
			queueDownload(rf);
			
			da.log("file-transfer", "download;"+da.getDaId()+";"+fileUID);

		} else {
			
			// File descriptor available
			agentPrintMessage("File "+fileUID+" available locally.");
			File f = desc.getFile();
			rf.cbH.getFileCB(null, rf.fileUID, f);

		}
		
	}
	
	private void queueDownload(GetFile rf) {
		
		String fileUID = rf.fileUID;

		LinkedList<GetFile> downList = currentDownloads.get(fileUID);
		if(downList == null) {
			downList = new LinkedList<GetFile>();
			currentDownloads.put(fileUID, downList);

			// Download is requested only if it was not already done.
			downloader.downloadFile(fileUID, this);
		} else {
			agentPrintMessage("File "+fileUID+" is already being downloaded.");
		}
		downList.addLast(rf);
	}


	private void handleFileReplicationRequest(FileReplicationRequest rf) throws Exception {

		String fileUID = rf.getFileUID();
		agentPrintMessage("Replication request for file "+fileUID);
		FileDescriptor desc = localFiles.getFileDescriptor(fileUID);
		DAId daSourceId = rf.getSender();
		if(desc != null) {
			agentPrintMessage("-- file "+fileUID+" is already localy available.");
			com.sendDatagramMessage(new FileReplicated(daSourceId, fileUID));
			return;
		}

		downloader.addRemoteFileLocation(fileUID, daSourceId);

		TreeSet<DAId> currentReplicationSources = filesToReplicate.get(fileUID);
		if(currentReplicationSources != null) {
			
			currentReplicationSources.add(daSourceId);
			agentPrintMessage("File "+fileUID+" is already being downloaded.");
			
		} else {
			
			currentReplicationSources = new TreeSet<DAId>();
			currentReplicationSources.add(daSourceId);
			filesToReplicate.put(fileUID, currentReplicationSources);

			if( ! currentDownloads.containsKey(fileUID)) {

				downloader.downloadFile(fileUID, this);

			} else {

				agentPrintMessage("File "+fileUID+" is already being downloaded.");

			}

		}

	}

	private void handleFileReplicated(FileReplicated rf) throws Exception {

		DAId sourceDaId = rf.getSender();
		String fileUID = rf.getFileUID();
		agentPrintMessage("File "+fileUID+" was replicated on DA "+sourceDaId);
		
		downloader.addRemoteFileLocation(fileUID, sourceDaId);
		da.getOverlayInterface().getPingServiceInterface().unregister(sourceDaId,
				this);
		
		// Remove replication from "todo-list"
		FileReplicationInfo rd = awaitedReplications.get(fileUID);
		if(rd == null) {
			agentPrintMessage("Replication was already signaled");
		} else {
			rd.signalSuccess(sourceDaId);
			if(! rd.cbsArePending()) {
				awaitedReplications.remove(fileUID);
			}
		}
	}

	private void handleFileLocationUpdate(FileLocationUpdate flu) throws Exception {

		String fileUID = flu.getFileUID();
		agentPrintMessage("File location update for "+fileUID);
		
		downloader.addRemoteFileLocation(fileUID, flu.getNewLocationId());

		LinkedList<GetFile> waiting = currentDownloads.remove(fileUID);
		if(waiting != null) {
			Iterator<GetFile> it = waiting.iterator();
			while(it.hasNext()) {
				GetFile gf = it.next();
				handleGetFile(gf);
			}
		}
	}

	private void handleDownloadFinished(DownloadFinished df) throws Exception {
		
		String fileUID = df.getFileUID();
		DFSException error = df.getError();

		if(error != null) {

			agentPrintMessage("Download failed for file "+fileUID);
			agentPrintMessage(error);

			LinkedList<GetFile> list = currentDownloads.remove(fileUID);
			if(list != null) {

				Iterator<GetFile> it = list.iterator();
				while(it.hasNext()) {
					GetFile fg = it.next();
					fg.cbH.getFileCB(error, fileUID, null);
				}

			}
			
			TreeSet<DAId> daSourceIds = filesToReplicate.remove(fileUID);
			if(daSourceIds != null) {

				Iterator<DAId> it = daSourceIds.iterator();
				while(it.hasNext()) {

					DAId daSourceId = it.next();
					com.sendDatagramMessage(new FileReplicated(daSourceId, fileUID));

				}

			}

		} else {
			
			agentPrintMessage("Download successfull for file "+fileUID);

			DAId thisDa = da.getDaId();
			File f = df.getFile();

			try {
				localFiles.addFile(fileUID, f, FileMode.rw);
			} catch (FileDBException e) {
				agentPrintMessage("File already localy available: "+fileUID);
				f.delete();
			}

			FileLocationUpdateValue loc = new FileLocationUpdateValue(fileUID);
			loc.addLocationToAdd(thisDa);

//			da.submitRequest(new UpdateLocalRequest(getFileKey(fileUID), loc), this);
			agentPrintMessage("Updating locations of file "+fileUID);
			String key = getFileKey(fileUID);
			FileLocation fLoc = new FileLocation(fileUID);
			fLoc.addRemoteFileLocation(thisDa);
			SharedMapEntry entry = new SharedMapEntry(key, fLoc);
			da.getOverlayInterface().getMapInterface().updateAsync(
					getFileKey(fileUID), loc, entry, this);

			LinkedList<GetFile> waitingRest = currentDownloads.remove(fileUID);
			if(waitingRest != null) {
				Iterator<GetFile> it = waitingRest.iterator();
				while(it.hasNext()) {
					GetFile gf = it.next();
					gf.cbH.getFileCB(null, fileUID, f);
				}
			}

		}

	}
	
	private void notifyFileForReplication(FileSystemCallBack cb, String fileUID,
			Set<DAId> dest) throws AgentException, InterruptedException, IOException {

		// All replica must be done initially
		FileReplicationInfo rd = awaitedReplications.get(fileUID);
		if(rd == null) {
			rd = new FileReplicationInfo(fileUID, this);
			awaitedReplications.put(fileUID, rd);
		}

		TreeSet<DAId> toDownload = new TreeSet<DAId>();
		rd.addCallBack(fileUID, cb, dest, toDownload);

		// Send replication notification to all neighbors
		Iterator<DAId> it = dest.iterator();
		while(it.hasNext()) {

			DAId daId = it.next();
			FileReplicationRequest frr = new FileReplicationRequest(daId, fileUID);
			frr.setCallBack(this);
			com.sendDatagramMessage(frr);

		}

	}
	
	private void exitActions() {
		try {

			try {
				downloader.stop();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				uploader.stop();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (AgentException e) {

			e.printStackTrace();

		}

		try {
			agentPrintMessage("Stopping downloader...");
			downloader.join();
			agentPrintMessage("Stopping uploader...");
			uploader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		clearAllFiles();
		
		agentPrintMessage("FSPeer exited.");
	}

	private void clearAllFiles() {

		localFiles.deleteAllFiles();

	}

	
	public void printMessage(String msg) {

		agentPrintMessage(msg);

	}


//	public CommunicatorInterface getCommunicator() {
//		return com;
//	}


	public DistributedAgent getHostingDA() {
		return da;
	}


	@Override
	public void signalBroken(BrokenDA bda) {
		try {
			submitMessage(bda);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success) {
			try {
				submitMessage(new BrokenDA(m.getRecipient()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void sharedMapGetCallBack(SharedMapGetResult res) {
		submitError(new Exception("Unhandled get result"));
	}

	@Override
	public void sharedMapPutCallBack(SharedMapPutResult res) {
		submitError(new Exception("Unhandled put result"));
	}

	@Override
	public void sharedMapRemoveCallBack(SharedMapRemoveResult res) {
		try {
			submitMessage(res);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sharedMapUpdateCallBack(SharedMapUpdateResult res) {
		try {
			submitMessage(res);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signalTargetTimeout(DetectorEvent e) {
		try {
			submitMessage(new BrokenDA(e.getTarget()));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

//	private void handleFlushed(Object o) {
//
//		if(o instanceof RemoveFile) {
//
//			RemoveFile rr = (RemoveFile) o;
//			removeReference(rr);
//
//		} else if(o instanceof DeleteFiles) {
//
//			DeleteFiles df = (DeleteFiles) o;
//			try {
//				deleteFiles(df);
//			} catch (AgentException e) {
//				e.printStackTrace();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			
//		} // else SKIP
//	}

}
