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

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import dimawo.agents.AbstractAgent;
import dimawo.agents.AgentException;
import dimawo.agents.ErrorHandler;
import dimawo.agents.MessageHandler;
import dimawo.agents.UnknownAgentMessage;
import dimawo.fileTransfer.client.events.CancelSession;
import dimawo.fileTransfer.client.events.GetFile;
import dimawo.fileTransfer.client.events.PingServers;
import dimawo.fileTransfer.client.messages.ChunkMessage;
import dimawo.fileTransfer.client.messages.ErrorMessage;
import dimawo.fileTransfer.client.messages.FileTransferClientMessage;
import dimawo.fileTransfer.client.messages.PingClientMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.ConnectCallBack;
import dimawo.middleware.communication.ConnectionRequestCallBack;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;

public class FileTransferClientAgent extends AbstractAgent<ClientState>
    implements MOSCallBack, ConnectionRequestCallBack {

  private CommunicatorInterface com;

  private long pingPeriod = 3000;
  private Timer pingTimer;

  private final Logger logger;

  public FileTransferClientAgent(ErrorHandler err, String name) {
    super(new ClientState(), err, name);

    pingTimer = new Timer(true);

    logger = Logger.getLogger(getClass());

    try {
      registerHandlers();
    } catch (AgentException e) {
      logger.error(e.getMessage(), e);
    }
  }

  @Override
  public void connectCallBack(ConnectCallBack cb) {
    if (cb.isSuccessful()) {
      try {
        submitMessage(cb);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } else {
      signalBroken(new BrokenDA(cb.getDaId()));
    }
  }

  public void getFile(GetFile getFile) {
    try {
      submitMessage(getFile);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void handleErrorMessage(ErrorMessage o) throws IOException {
    DAId serverId = o.getServerId();
    String fileUID = o.getFileUID();
    agentPrintMessage("Error from server " + serverId + ": " + o.getMessage());

    DownloadSession session = sessions.get(serverId);
    if (session != null) {
      session.cancelCurrentDownload(fileUID);
      if (!session.isActive()) {
        sessions.remove(fileUID);
        session.close();
      }
    }
  }

  private void handleGetFile(GetFile o) {
    DAId serverDaId = o.getServerDaId();
    agentPrintMessage("Getting file " + o.getFileUID() + " from " + serverDaId);
    DownloadSession session = sessions.get(serverDaId);
    if (session != null) {
      agentPrintMessage("Download queued for server " + serverDaId);
      session.queue(o);
    } else {
      agentPrintMessage("Creating session and connecting to server "
          + serverDaId);
      try {
        com.asyncConnect(serverDaId, this, this, null);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      session = new DownloadSession(serverDaId, this, o);
      sessions.put(serverDaId, session);
    }
  }

  @Override
  protected void handleMessage(Object o) throws Throwable {
    if (o instanceof GetFile) {
      handleGetFile((GetFile) o);
    } else if (o instanceof ChunkMessage) {
      handleChunkMessage((ChunkMessage) o);
    } else if (o instanceof ConnectCallBack) {
      handleConnectCallBack((ConnectCallBack) o);
    } else if (o instanceof ErrorMessage) {
      handleErrorMessage((ErrorMessage) o);
    } else if (o instanceof CancelSession) {
      handleCancelSession((CancelSession) o);
    } else if (o instanceof PingClientMessage) {
      // SKIP
    } else if (o instanceof PingServers) {
      handlePingServers((PingServers) o);
    } else {
      throw new UnknownAgentMessage(o);
    }
  }

  private void handlePingServers(PingServers o) {
    for (DownloadSession s : sessions.values()) {
      s.pingServer();
    }
  }

  @Override
  protected void init() throws Throwable {
    agentPrintMessage("init");

    if (com == null) {
      throw new Error("Communicator is not set");
    }

    pingTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          submitMessage(new PingServers());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }, pingPeriod, pingPeriod);
  }

  private void registerHandlers() throws AgentException {
    // Register cancel session message handler
    registerHandler(CancelSession.class,
        new MessageHandler<ClientState, CancelSession>() {
          @Override
          public void handle(ClientState state, CancelSession o) {
            DAId daId = o.getDaId();
            DownloadSession session = state.sessions.remove(daId);
            if (session != null) {
              logger.info("Cancelling session " + daId);
              session.close();
            }
          }
        });

    // Register chunk message handler
    registerHandler(ChunkMessage.class,
        new MessageHandler<ClientState, ChunkMessage>() {
          @Override
          public void handle(ClientState state, ChunkMessage o)
              throws IOException {
            DAId daId = o.getServerDaId();
            logger.trace("Received chunk from server " + daId);
            DownloadSession session = state.sessions.get(daId);
            if (session == null) {
              logger.warn("Ignored chunk from " + daId);
              return;
            }

            session.writeChunk(o);
            if (!session.isActive()) {
              logger.trace("Session " + daId
                  + " is not active anymore, closed.");
              session.close();
              state.sessions.remove(daId);
            }
          }
        });

    // Register connect call back handler
    registerHandler(ConnectCallBack.class,
        new MessageHandler<ClientState, ConnectCallBack>() {
          @Override
          public void handle(ClientState state, ConnectCallBack o)
              throws IOException {
            DAId daId = o.getDaId();
            MOSAccessorInterface access = o.getAccess();
            if (access == null) {
              logger.warn("Could not connect to server " + daId);
              DownloadSession session = state.sessions.remove(daId);
              if (session != null) {
                session.close();
              }
              return;
            }

            logger.trace("Connected to server " + daId);
            DownloadSession session = state.sessions.get(daId);
            if (session != null) {
              session.setAccess(access);
            } else {
              logger.trace("No session for server " + daId);
              try {
                access.close();
              } catch (IOException e) {
                e.printStackTrace();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        });
  }

  public void setCommunicator(CommunicatorInterface com) {
    this.com = com;
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
    if (!success) {
      try {
        submitMessage(new CancelSession(m.getRecipient()));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void submitClientMessage(FileTransferClientMessage m) {
    try {
      submitMessage(m);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
