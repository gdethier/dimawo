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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import dimawo.fileTransfer.FileTransferMessageFactory;
import dimawo.fileTransfer.client.events.GetFile;
import dimawo.fileTransfer.client.messages.ChunkMessage;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSAccessorInterface;
import dimawo.middleware.distributedAgent.DAId;

public class DownloadSession {

  private DAId serverDaId;
  private FileTransferClientAgent client;

  private MOSAccessorInterface access;

  private GetFile initReq;

  private FileTransferMessageFactory mFact;
  private String fileUID;
  private File file;
  private FileOutputStream fos;
  private FileTransferClientCallBack cb;

  private LinkedList<GetFile> pendingRequests;

  private Logger logger;

  public DownloadSession(DAId serverDaId, FileTransferClientAgent client,
      GetFile o) {
    this.serverDaId = serverDaId;
    this.client = client;
    this.initReq = o;

    pendingRequests = new LinkedList<GetFile>();

    logger = Logger.getLogger(getClass());
  }

  public void cancelCurrentDownload(String fileUID) throws IOException {
    if (!fileUID.equals(this.fileUID)) {
      throw new Error("Not currently downloading " + fileUID);
    }

    fos.close();
    file.delete();

    prepareNextDownload();

    cb.submitFile(new GetFileCallBack(access.getDestinationDAId(), fileUID,
        GetFileCallBack.Error.fileNotFound));
  }

  public void close() {
    if (fos != null) {
      try {
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (file != null) {
      file.delete();
    }

    if (initReq != null) {
      String fileUID = initReq.getFileUID();
      FileTransferClientCallBack cb = initReq.getGetFileCallBack();
      logger.info("Signal unsuccessful download of " + fileUID + " to "
          + cb.getClass().getName());
      cb.submitFile(new GetFileCallBack(serverDaId, fileUID,
          GetFileCallBack.Error.serverDown));
    }

    if (cb != null) {
      logger.info("Signal unsuccessful download of " + fileUID + " to "
          + cb.getClass().getName());
      cb.submitFile(new GetFileCallBack(serverDaId, fileUID,
          GetFileCallBack.Error.serverDown));
    }

    for (GetFile gf : pendingRequests) {
      String fileUID = gf.getFileUID();
      FileTransferClientCallBack cb = gf.getGetFileCallBack();
      logger.warn("Signal unsuccessful download of " + fileUID + " to "
          + cb.getClass().getName());
      cb.submitFile(new GetFileCallBack(serverDaId, fileUID,
          GetFileCallBack.Error.serverDown));
    }

    if (access != null) {
      try {
        access.close();
      } catch (IOException e) {
      } catch (InterruptedException e) {
      }
    }

    logger.info("Session closed.");
  }

  private void initDownload(GetFile gf) throws FileNotFoundException {
    mFact = gf.getMessageFactory();
    fileUID = gf.getFileUID();
    file = gf.getDestFile();
    cb = gf.getGetFileCallBack();

    fos = new FileOutputStream(file);

    Message m = (Message) mFact.newGetFileRequest(fileUID, gf.isFileName());
    m.setCallBack(client);
    access.writeNonBlockingMessage(m);
  }

  public boolean isActive() {
    return fos != null;
  }

  public void pingServer() {
    if (access != null) {
      access.writeNonBlockingMessage((Message) mFact.newPingServerMessage());
    }
  }

  private void prepareNextDownload() throws FileNotFoundException {
    file = null;
    fos = null;
    cb = null;
    mFact = null;
    fileUID = null;

    if (!pendingRequests.isEmpty()) {
      GetFile gf = pendingRequests.removeFirst();

      initDownload(gf);
    }
  }

  public void queue(GetFile o) {
    pendingRequests.addLast(o);
  }

  public void setAccess(MOSAccessorInterface access)
      throws FileNotFoundException {
    this.access = access;

    initDownload(initReq);
    initReq = null;
  }

  public void writeChunk(ChunkMessage o) throws IOException {
    byte[] data = o.getData();
    boolean isLast = o.isLast();

    fos.write(data);
    if (isLast) {
      fos.close();
      cb.submitFile(new GetFileCallBack(access.getDestinationDAId(), fileUID,
          file));

      prepareNextDownload();
    } else {
      Message m = (Message) mFact.newGetNextChunkMessage(fileUID);
      m.setCallBack(client);
      access.writeNonBlockingMessage(m);
    }
  }

}
