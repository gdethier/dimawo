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
package dimawo.middleware.overlay.impl.decentral;

import java.io.File;
import java.io.IOException;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UncaughtThrowable;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.BarrierSyncInterface;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.LeaderElectionInterface;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayException;
import dimawo.middleware.overlay.OverlayMessage;
import dimawo.middleware.overlay.SharedMapAgentInterface;
import dimawo.middleware.overlay.faultdetection.FaultDetectionAgent;
import dimawo.middleware.overlay.impl.central.DummyMessage;
import dimawo.middleware.overlay.impl.central.events.ShutdownOverlay;
import dimawo.middleware.overlay.impl.decentral.barriersync.BarrierSyncAgent;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdateCallBackInterface;
import dimawo.middleware.overlay.mntree.MnTreePeerAgent;
import dimawo.middleware.overlay.mntree.messages.MnTreeMessage;
import dimawo.middleware.sharedMap.dht.chord.ChordAgent;
import dimawo.middleware.sharedMap.messages.SharedMapAgentMessage;
import dimawo.simulation.socket.SocketFactory;




public class DecentralOverlay extends LoggingAgent implements OverlayAgentInterface, MOSCallBack {

	private FaultDetectionAgent pingSvc;
	private Communicator com;
	private DistributedAgent da;

	private JoinParameters joinParams;

	// DHT
	private ChordAgent mapAgent;
	
	private int maxNumOfChildren, reliabilityThresh;
	private MnTreePeerAgent mnTreeAgent;
	
	private BarrierSyncAgent syncAgent;


	public DecentralOverlay(DistributedAgent da,
			int maxNumOfChildren, int reliabilityThresh,
			SocketFactory sockFact) throws IOException {
		super(null, "DecentralOverlay");
		
		this.setPrintStream(da.getFilePrefix());

		com = new Communicator(this, da, sockFact);
		com.setOverlayInterface(this);
		
		pingSvc = new FaultDetectionAgent(this, com, 1000, 10000);
		pingSvc.setPrintStream(da.getFilePrefix());

		this.da = da;
		da.setOverlayInterface(this);
		
		this.maxNumOfChildren = maxNumOfChildren;
		this.reliabilityThresh = reliabilityThresh;
		
		syncAgent = new BarrierSyncAgent(this, com);
		syncAgent.setPrintStream(da.getFilePrefix());
	}


	@Override
	public void initOverlay() throws OverlayException {
		try {
			com.start();
		} catch (AgentException e1) {
			throw new OverlayException("Peer already connected");
		}

		String dir = new File(da.getFilePrefix()).getParent();
//		try {
//			agentPrintMessage("Creating DHT on port "+dhtPort);
//			dht = OWDHTAgent.initDHT(dhtPort, dhtPortRange, dir);
//			
//			// Set levels for OW framework loggers
//			Logger log = Logger.getLogger("dht");
//			log.setLevel(Level.WARNING);
//			log = Logger.getLogger("messaging");
//			log.setLevel(Level.WARNING);
//			log = Logger.getLogger("routing");
//			log.setLevel(Level.WARNING);
//		} catch (Exception e1) {
//			throw new OverlayException("Could not create OW DHT", e1);
//		}
//		mapAgent = new OWDHTAgent(this, dht, da.getFilePrefix());
		mapAgent = new ChordAgent(this, 1, true, null);
		mapAgent.setPrintStream(da.getFilePrefix());
		try {
			mapAgent.start();
		} catch (AgentException e1) {
			e1.printStackTrace();
		}
		
		mnTreeAgent = new MnTreePeerAgent(this, com, da);
		mnTreeAgent.setPrintStream(da.getFilePrefix());
		try {
			mnTreeAgent.initOverlay(this.maxNumOfChildren, this.reliabilityThresh);
		} catch (AgentException e) {
			throw new OverlayException("Could not create MN-tree", e);
		}

		try {
			super.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		startAgents();
	}
	
	private void startAgents() throws OverlayException {
//		try {
//			mapAgent.start();
//		} catch (Exception e) {
//			throw new OverlayException(e);
//		}
		
		try {
			pingSvc.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		
		try {
			syncAgent.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}
	
	public void setJoinParameters(JoinParameters joinParams) {
		this.joinParams = joinParams;
	}

	@Override
	public void joinOverlay() throws OverlayException {
		try {
			com.start();
		} catch (AgentException e1) {
			throw new OverlayException("Peer already connected");
		}
		
		try {
			super.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		
		String dir = new File(da.getFilePrefix()).getParent();
//		try {
//			agentPrintMessage("Creating DHT on port "+dhtPort+", joining...");
//			dht = OWDHTAgent.joinDHT(dhtPort, dhtPortRange, joinParams.getDhtHostName(), joinParams.getDhtPort(), dir);
//		} catch (Exception e1) {
//			throw new OverlayException("Could not create OW DHT", e1);
//		}
//		agentPrintMessage("DHT joined.");
//		mapAgent = new OWDHTAgent(this, dht, da.getFilePrefix());
		mapAgent = new ChordAgent(this, 1, false,
				new DAId(joinParams.getHostName(), joinParams.getPort(), 0));
		mapAgent.setPrintStream(da.getFilePrefix());
		try {
			mapAgent.start();
		} catch (AgentException e1) {
			e1.printStackTrace();
		}
		
		agentPrintMessage("Joining MN-tree");
		mnTreeAgent = new MnTreePeerAgent(this, com, da);
		mnTreeAgent.setPrintStream(da.getFilePrefix());
		try {
			mnTreeAgent.joinOverlay(20000, joinParams.getHostName(), joinParams.getPort());
		} catch (Exception e) {
			throw new OverlayException("Could not join MN-tree", e);
		}
		agentPrintMessage("MN-tree joined.");
		
		agentPrintMessage("Joining DHT");
		try {
			mapAgent.waitForJoin(20000);
		} catch (Exception e) {
			throw new OverlayException("Could not join DHT", e);
		}
		agentPrintMessage("DHT joined.");

		startAgents();
	}

	@Override
	public void leaveOverlay() throws OverlayException {
		try {
			this.stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public DistributedAgent getDA() {
		return da;
	}

	@Override
	public CommunicatorInterface getCommunicator() {
		return com;
	}

	@Override
	public SharedMapAgentInterface getMapInterface() {
		return mapAgent;
	}

	@Override
	public BroadcastingAgentInterface getBroadcastInterface() {
		return mnTreeAgent;
	}

	@Override
	public LeaderElectionInterface getLeaderElectionInterface() {
		return mnTreeAgent;
	}

	@Override
	public void submitOverlayMessage(OverlayMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
	protected void logAgentExit() {
		stopAgents();
		da.signalOverlayDisconnected();
		agentPrintMessage("exit");
	}
	
	private void stopAgents() {
//		if(dht != null) {
//			agentPrintMessage("Stopping DHT");
//			HighLevelService hls = dht;
//			hls.stop();
//		}
		try {
			if(mapAgent != null) {
				mapAgent.stop();
			}
		} catch (Exception e) {
		}
		try {
			if(mnTreeAgent != null) {
				mnTreeAgent.stop();
			}
		} catch (Exception e) {
		}
		
		try {
			if(pingSvc != null)
				pingSvc.stop();
		} catch (Exception e1) {
		}
		
		try {
			if(syncAgent != null)
				syncAgent.stop();
		} catch (Exception e1) {
		}

		try {
			if(mapAgent != null) {
				agentPrintMessage("Joining map agent");
				mapAgent.join();
			}
		} catch (InterruptedException e) {
		}
		try {
			if(mnTreeAgent != null) {
				agentPrintMessage("Joining MN-tree");
				mnTreeAgent.join();
			}
		} catch (InterruptedException e) {
		}
		try {
			if(pingSvc != null)
				pingSvc.join();
		} catch (InterruptedException e1) {
		}
		
		try {
			if(syncAgent != null)
				syncAgent.join();
		} catch (InterruptedException e1) {
		}
		
		com.stop();
		try {
			agentPrintMessage("Joining communicator");
			com.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
	}

	@Override
	protected void handleMessage(Object msg) throws Throwable {
		if(msg instanceof MnTreeMessage) {
			mnTreeAgent.submitMnTreeMessage((MnTreeMessage) msg);
		} else if(msg instanceof SharedMapAgentMessage) {
			mapAgent.submitDMapAgentMessage((SharedMapAgentMessage) msg);
		} else if(msg instanceof BrokenDA) {
			handleBrokenDA((BrokenDA) msg);
		} else if(msg instanceof ShutdownOverlay) {
			handleShutdownOverlay((ShutdownOverlay) msg);
		} else if(msg instanceof DummyMessage) {
			// SKIP
		} else {
			throw new OverlayException("Unknown overlay message: "+msg.getClass().getName());
		}
	}
	
	private void handleShutdownOverlay(ShutdownOverlay msg) {
		mnTreeAgent.signalShutdown();

		agentPrintMessage("Shutdown in progress...");
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success) {
			signalBroken(new BrokenDA(m.getRecipient()));
		}
	}
	
	private void handleBrokenDA(BrokenDA bda) {
		DAId id = bda.getDAId();
		agentPrintMessage("Broken DA "+id);
	}

	@Override
	public BarrierSyncInterface getBarrierSyncInterface() {
		return syncAgent;
	}
	
	@Override
	protected void handleChildException(UncaughtThrowable t) {
		agentPrintMessage("Received error from child:");
		agentPrintMessage(t);

		try {
			stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void registerForMnTreeUpdates(
			MnTreeLocalUpdateCallBackInterface cbInt) {
		mnTreeAgent.register(cbInt);
	}


	@Override
	public void shutdownOverlay() throws OverlayException {
		try {
			submitMessage(new ShutdownOverlay());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void signalMnTreeDisconnected() {
		try {
			submitMessage(new ShutdownOverlay());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Override
	public FaultDetectionAgent getPingServiceInterface() {
		return pingSvc;
	}

}
