package dimawo.fileTransfer.client;

import java.util.Map;
import java.util.TreeMap;

import dimawo.middleware.distributedAgent.DAId;

public class ClientState {

  Map<DAId, DownloadSession> sessions = new TreeMap<DAId, DownloadSession>();

}
