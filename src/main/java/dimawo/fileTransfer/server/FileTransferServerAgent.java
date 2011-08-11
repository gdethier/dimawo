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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.fileTransfer.server.events.CancelSession;
import dimawo.fileTransfer.server.events.PingClients;
import dimawo.fileTransfer.server.messages.GetFileRequest;
import dimawo.fileTransfer.server.messages.GetNextChunkRequest;
import dimawo.fileTransfer.server.messages.PingServerMessage;
import dimawo.fileTransfer.server.messages.SimpleFTPServerMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.ConnectCallBack;
import dimawo.middleware.communication.ConnectionRequestCallBack;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;



public class FileTransferServerAgent
extends LoggingAgent
implements MOSCallBack, ConnectionRequestCallBack {
	
	private int maxSessions = 0; // 0 = no limit
	private int chunkSize = 524288; // in Bytes
	private CommunicatorInterface com;
	private FileProvider fileProv;

	private TreeMap<DAId, UploadSession> sessions;
	private LinkedList<GetFileRequest> pendingRequests;
	
	private long pingPeriod = 3000;
	private Timer pingTimer;


	public FileTransferServerAgent(ErrorHandler parent, String name) {
		super(parent, name);
		
		sessions = new TreeMap<DAId, UploadSession>();
		pendingRequests = new LinkedList<GetFileRequest>();
		
		pingTimer = new Timer(true);
	}
	
	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	/**
	 * Must be called before agent is started to set Communicator.
	 * @param com
	 */
	public void setCommunicator(CommunicatorInterface com) {
		this.com = com;
	}
	
	public void setFileProvider(FileProvider fileProv) {
		this.fileProv = fileProv;
	}

	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof GetFileRequest) {
			handleGetFileRequest((GetFileRequest) o);
		} else if(o instanceof GetNextChunkRequest) {
			handleGetNextChunkRequest((GetNextChunkRequest) o);
		} else if(o instanceof CancelSession) {
			handleCancelSession((CancelSession) o);
		} else if(o instanceof ConnectCallBack) {
			handleConnectCallBack((ConnectCallBack) o);
		} else if(o instanceof PingClients) {
			handlePingClients((PingClients) o);
		} else if(o instanceof PingServerMessage) {
			// SKIP
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		if(com == null)
			throw new Error("No communicator set");
		
		if(fileProv == null)
			throw new Error("No file provider set");
		
		pingTimer.schedule(new TimerTask() {
			public void run() {
				try {
					submitMessage(new PingClients());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, pingPeriod, pingPeriod);
	}
	
	private void handleGetFileRequest(GetFileRequest o) {
		if(maxSessions > 0 && sessions.size() >= maxSessions) {
			agentPrintMessage("Queued file request");
			pendingRequests.addLast(o);
		} else {
			openSession(o);
		}
	}
	
	private void openSession(GetFileRequest o) {
		DAId sourceDaId = o.getClientDaId();
		
		UploadSession session = sessions.get(sourceDaId);
		if(session != null) {
			agentPrintMessage("Queued file request in existing session ("+sourceDaId+").");
			session.queueRequest(o);
		} else {
			agentPrintMessage("Opened a new session for file "+o.getFileUID()+" ("+sourceDaId+").");
			try {
				com.asyncConnect(sourceDaId, this, this, null);
				session = new UploadSession(this, chunkSize, fileProv, o);
				sessions.put(sourceDaId, session);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleCancelSession(CancelSession o) {
		DAId daId = o.getDaId();
		
		UploadSession cancelledSession = sessions.remove(daId);
		if(cancelledSession != null) {
			cancelledSession.close();
			schedulePendingRequests();
		}
	}

	private void handleGetNextChunkRequest(GetNextChunkRequest o) throws IOException {
		DAId sourceDaId = o.getClientDaId();
		
		agentPrintMessage("Received chunk query from client "+sourceDaId);
		
		UploadSession session = sessions.get(sourceDaId);
		if(session == null) {
			throw new Error("No session available");
		}

		session.sendNextChunk();
		if(! session.isActive()) {
			agentPrintMessage("Session "+sourceDaId+" is not active anymore, closed.");
			sessions.remove(sourceDaId);
			session.close();
			schedulePendingRequests();
		}
	}
	
	private void handleConnectCallBack(ConnectCallBack o) throws IOException {
		DAId daId = o.getDaId();

		if(o.isSuccessful()) {
			MOSAccessorInterface access = o.getAccess();
			agentPrintMessage("Connection to client "+daId+" successfuly established.");
			
			UploadSession session = sessions.get(daId);
			if(session != null) {
				session.setAccess(access);
				if(! session.isActive()) {
					agentPrintMessage("Session "+daId+" is not active anymore, closed.");
					session.close();
					sessions.remove(daId);
				}
			} else {
				agentPrintMessage("No session for client "+daId);
				try {
					access.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		} else {
			agentPrintMessage("Unable to connect to client "+daId);
			UploadSession session = sessions.remove(daId);
			if(session != null) {
				session.close();
			}
			
		}
	}

	private void schedulePendingRequests() {
		while(sessions.size() < maxSessions && ! pendingRequests.isEmpty()) {
			GetFileRequest o = pendingRequests.removeFirst();
			openSession(o);
		}
	}

	@Override
	public void signalBroken(BrokenDA bda) {
		try {
			submitMessage(new CancelSession(bda.getDAId()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success) {
			try {
				submitMessage(new CancelSession(m.getRecipient()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void connectCallBack(ConnectCallBack cb) {
		try {
			submitMessage(cb);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void submitServerMessage(SimpleFTPServerMessage m) {
		try {
			submitMessage(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handlePingClients(PingClients o) {
		for(UploadSession s : sessions.values()) {
			s.pingClient();
		}
	}
}
