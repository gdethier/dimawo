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
package dimawo.middleware.overlay.impl.central;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UncaughtThrowable;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.Communicator;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.FailureDetectionOutputStream;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.BarrierSyncInterface;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.BroadcastingAgentMessage;
import dimawo.middleware.overlay.JoinParameters;
import dimawo.middleware.overlay.LeaderElectionInterface;
import dimawo.middleware.overlay.OverlayAgentInterface;
import dimawo.middleware.overlay.OverlayException;
import dimawo.middleware.overlay.OverlayJoin;
import dimawo.middleware.overlay.OverlayMessage;
import dimawo.middleware.overlay.PeerJoinRequest;
import dimawo.middleware.overlay.SharedMapAgentInterface;
import dimawo.middleware.overlay.faultdetection.FaultDetectionAgent;
import dimawo.middleware.overlay.impl.central.barrier.CentralBarrierAgent;
import dimawo.middleware.overlay.impl.central.barrier.CentralBarrierAgentInterface;
import dimawo.middleware.overlay.impl.central.barrier.LeaderCentralBarrierAgent;
import dimawo.middleware.overlay.impl.central.barrier.messages.CentralBarrierMessage;
import dimawo.middleware.overlay.impl.central.broadcast.CentralBroadcastAgent;
import dimawo.middleware.overlay.impl.central.broadcast.CentralControllerBroadcastAgent;
import dimawo.middleware.overlay.impl.central.events.ShutdownOverlay;
import dimawo.middleware.overlay.impl.central.map.CentralControllerMapAgent;
import dimawo.middleware.overlay.impl.central.map.CentralTableAgent;
import dimawo.middleware.overlay.impl.central.messages.MnPeerStateUpdateMessage;
import dimawo.middleware.overlay.impl.central.messages.OverlayErrorMessage;
import dimawo.middleware.overlay.impl.central.messages.SendToLeaderMessage;
import dimawo.middleware.overlay.mntree.MnPeerState;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdateCallBackInterface;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Action;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Cause;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Target;
import dimawo.middleware.sharedMap.messages.SharedMapAgentMessage;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;




public class CentralOverlay extends LoggingAgent implements OverlayAgentInterface, LeaderElectionInterface, MOSCallBack {
	
	private Communicator com;
	private DistributedAgent da;
	
	private boolean isLeader;
	private DAId ctrlId;
	private Timer connCheck;
	
	private String joinHost;
	private int joinPort;
	
	private Semaphore waitForAck;
	
	private SharedMapAgentInterface mapAgent;
	private BroadcastingAgentInterface broadAgent;
	private CentralBarrierAgentInterface barrierAgent;
	
	private TreeSet<DAId> das;
	private MnPeerState currentMnPeerState;
	private HashSet<MnTreeLocalUpdateCallBackInterface> cbs;
	
	
	public CentralOverlay(DistributedAgent da,
			SocketFactory sockFact) throws IOException {
		super(null, "CentralOverlay");
		
		this.setPrintStream(da.getFilePrefix());

		com = new Communicator(this, da, sockFact);
		com.setOverlayInterface(this);

		this.da = da;
		da.setOverlayInterface(this);
		
		das = new TreeSet<DAId>();
		cbs = new HashSet<MnTreeLocalUpdateCallBackInterface>();
	}
	
	@Override
	public void initOverlay() throws OverlayException {
		try {
			com.start();
		} catch (AgentException e1) {
			throw new OverlayException("Peer already connected");
		}

		isLeader = true;
		ctrlId = da.getDaId();
		currentMnPeerState = MnPeerState.createRootState(ctrlId, 0, Integer.MAX_VALUE);
		
		mapAgent = new CentralControllerMapAgent(this, da);
		broadAgent = new CentralControllerBroadcastAgent(this, da, com);
		barrierAgent = new LeaderCentralBarrierAgent(this, da, com);
		
		try {
			super.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		startAgents();
	}
	
	@Override
	public void setJoinParameters(JoinParameters params) {
		this.joinHost = params.getHostName();
		this.joinPort = params.getPort();
	}

	@Override
	public void joinOverlay() throws OverlayException {
		try {
			com.start();
		} catch (AgentException e1) {
			throw new OverlayException("Peer already connected");
		}

		isLeader = false;
		waitForAck = new Semaphore(0);
		
		mapAgent = new CentralTableAgent(this, da);
		broadAgent = new CentralBroadcastAgent(this, da);
		barrierAgent = new CentralBarrierAgent(this, this);
		
		try {
			register();
		} catch (OverlayException e) {
			com.stop();
			throw e;
		}
		
		try {
			super.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		
		try {
			if(! waitForAck.tryAcquire(20000, TimeUnit.MILLISECONDS)) {
				com.stop();
				try {
					stop();
				} catch (AgentException e) {
					e.printStackTrace();
				}
				throw new OverlayException("Unable to connect through peer "+joinHost);
			}
		} catch (InterruptedException e) {
			throw new OverlayException("Unable to join", e);
		}
		
		((CentralBroadcastAgent) broadAgent).setLeaderId(ctrlId);
		startAgents();
	}

	private void register() throws OverlayException {
		if(joinHost == null)
			throw new OverlayException("Join parameters are not set");

		for(int i = 0; i < 3; ++i) {

			SocketInterface sock = null;
			try {

				SocketFactory sockFact = com.getSocketFactory();
				sock = sockFact.newSocket();
				sock.connect(sock.getSocketAddress(joinHost, joinPort), 20000);

				if( ! sock.isConnected()) {
					throw new IOException("Could not connect to manager : Time out.");
				}

				agentPrintMessage("Sending join request #"+i);
				FailureDetectionOutputStream out =
					new FailureDetectionOutputStream(sock, 10000);
				out.writeObject(new OverlayJoinImpl(da.getDaId()));
				break; // Message sent -> registration finished

			} catch (IOException e) {

				agentPrintMessage("Could not identify DA ("+e+").");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}

			} finally {
				if(sock != null) {
					try {
						sock.close();
					} catch(IOException e) {}
				}
			}

		}
	}
	
	@Override
	public CommunicatorInterface getCommunicator() {
		return com;
	}

	@Override
	public void sendMessageToLeader(Message msg) {
		msg.setRecipient(ctrlId);
		if(isLeader) {
			msg.setSender(ctrlId);
			msg.setRecipient(ctrlId);
			com.submitIncomingMessage(msg);
		} else {
			com.sendDatagramMessage(new SendToLeaderMessage(ctrlId, msg));
		}
	}

	@Override
	public SharedMapAgentInterface getMapInterface() {
		return mapAgent;
	}

	@Override
	public BroadcastingAgentInterface getBroadcastInterface() {
		return broadAgent;
	}

	@Override
	public LeaderElectionInterface getLeaderElectionInterface() {
		// As there is no leader election process, no particular agent
		// is needed.
		return this;
	}
	
	private void startAgents() throws OverlayException {
		try {
			mapAgent.start();
		} catch (Exception e) {
			throw new OverlayException(e);
		}
		try {
			broadAgent.start();
		} catch (Exception e) {
			throw new OverlayException(e);
		}
		try {
			barrierAgent.start();
		} catch (Exception e) {
			throw new OverlayException(e);
		}
		
		connCheck = new Timer(true);
		connCheck.schedule(new TimerTask() {
			public void run() {
				checkConnectivity();
			}
		}, 10000, 10000);
	}
	
	private void stopAgents() {
		if(connCheck != null)
			connCheck.cancel();

		try {
			if(mapAgent != null)
				mapAgent.stop();
		} catch (Exception e) {
		}
		try {
			if(broadAgent != null)
				broadAgent.stop();
		} catch (Exception e) {
		}
		try {
			if(barrierAgent != null)
				barrierAgent.stop();
		} catch (Exception e) {
		}
		try {
			if(mapAgent != null)
				mapAgent.join();
		} catch (InterruptedException e) {
		}
		try {
			if(broadAgent != null)
				broadAgent.join();
		} catch (InterruptedException e) {
		}
		try {
			if(barrierAgent != null)
				barrierAgent.join();
		} catch (InterruptedException e) {
		}
		
		com.stop();
		try {
			com.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void submitOverlayMessage(OverlayMessage msg) {
		try {
			submitMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleOverlayJoin(OverlayJoin o) {
		if(ctrlId == null)
			// Not yet registered
			return;

		if(isLeader) {
			DAId id = o.getId();
			das.add(id);
			
			// Update mn peer state
			currentMnPeerState.addPeerToThisMn(id);
			MnPeerState otherState = currentMnPeerState.clone();
			MnTreeLocalUpdate update = new MnTreeLocalUpdate(id, Cause.join, Target.thisMn, Action.add, otherState);
			for(DAId daId : das) {
				com.sendDatagramMessage(new MnPeerStateUpdateMessage(daId, update));
			}
			
			// Signal update
			for(MnTreeLocalUpdateCallBackInterface cbInt : cbs) {
				cbInt.signalMnTreeLocalUpdate(new MnTreeLocalUpdate(id, Cause.join, Target.thisMn, Action.add, currentMnPeerState.clone()));
			}

			((LeaderCentralBarrierAgent) barrierAgent).submitNewDA(id);
			((CentralControllerBroadcastAgent) broadAgent).submitNewDA(id);

			com.sendDatagramMessage(new CentralOverlayJoinAckMessage(id, ctrlId));
		} else {
			com.sendDatagramMessage(new PeerJoinRequest(ctrlId, o));
		}
	}
	
	@Override
	protected void logAgentExit() {
		stopAgents();
		da.signalOverlayDisconnected();
		agentPrintMessage("Overlay closed.");
	}

	@Override
	protected void handleMessage(Object msg) throws Throwable {
		if(msg instanceof SharedMapAgentMessage) {
			mapAgent.submitDMapAgentMessage((SharedMapAgentMessage) msg);
		} else if(msg instanceof BroadcastingAgentMessage) {
			broadAgent.submitBroadcastingAgentMessage((BroadcastingAgentMessage) msg);
		} else if(msg instanceof CentralBarrierMessage) {
			barrierAgent.submitCentralBarrierMessage((CentralBarrierMessage) msg);
		} else if(msg instanceof OverlayJoin) {
			handleOverlayJoin((OverlayJoin) msg);
		} else if(msg instanceof CentralOverlayJoinAckMessage) {
			handleCentralOverlayJoinAckMessage((CentralOverlayJoinAckMessage) msg);
		} else if(msg instanceof BrokenDA) {
			handleBrokenDA((BrokenDA) msg);
		} else if(msg instanceof CheckConnectivity) {
			handleCheckConnectivity((CheckConnectivity) msg);
		} else if(msg instanceof OverlayErrorMessage) {
			handleOverlayErrorMessage((OverlayErrorMessage) msg);
		} else if(msg instanceof MnTreeLocalUpdateCallBackInterface) {
			handleMnTreeLocalUpdateCallBackInterface((MnTreeLocalUpdateCallBackInterface) msg);
		} else if(msg instanceof MnPeerStateUpdateMessage) {
			handleMnPeerStateUpdateMessage((MnPeerStateUpdateMessage) msg);
		} else if(msg instanceof SendToLeaderMessage) {
			handleSendToLeaderMessage((SendToLeaderMessage) msg);
		} else if(msg instanceof ShutdownOverlay) {
			handleShutdownOverlay((ShutdownOverlay) msg);
		} else if(msg instanceof ShutdownOverlayMessage) {
			handleShutdownOverlayMessage((ShutdownOverlayMessage) msg);
		} else if(msg instanceof DummyMessage) {
			// SKIP
		} else {
			throw new OverlayException("Unknown overlay message: "+msg.getClass().getName());
		}
	}

	private void handleShutdownOverlay(ShutdownOverlay msg) {
		for(DAId id : das) {
			com.sendDatagramMessage(new ShutdownOverlayMessage(id));
		}
		agentPrintMessage("Shutdown in progress...");
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}
	
	private void handleShutdownOverlayMessage(ShutdownOverlayMessage msg) {
		agentPrintMessage("Overlay shutdown...");
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}

	private void handleSendToLeaderMessage(SendToLeaderMessage msg) {
		if(! isLeader) {
			throw new Error("Message should have been sent to leader");
		}
		
		Message m = msg.getMessage();
		m.setSender(msg.getSender());
		m.setRecipient(ctrlId);
		com.submitIncomingMessage(m);
	}

	private void handleMnPeerStateUpdateMessage(
			MnPeerStateUpdateMessage msg) {
		if(isLeader) {
			throw new Error("Should not receive this message");
		}
		
		currentMnPeerState = msg.getUpdate().getNewState();
		if(currentMnPeerState.isMainPeer(da.getDaId()))
			throw new Error("Cannot be main peer");
		for(MnTreeLocalUpdateCallBackInterface cbInt : cbs) {
			cbInt.signalMnTreeLocalUpdate(msg.getUpdate());
		}
	}

	private void handleMnTreeLocalUpdateCallBackInterface(
			MnTreeLocalUpdateCallBackInterface msg) {
		cbs.add(msg);
		if(currentMnPeerState != null)
			msg.signalMnTreeLocalUpdate(new MnTreeLocalUpdate(null,
					Cause.init,
				null, null, currentMnPeerState));
	}

	private void handleOverlayErrorMessage(OverlayErrorMessage msg) throws InterruptedException, AgentException {
		agentPrintMessage("Received error message from DA "+msg.getSender());
		Throwable err = msg.getError();
		if(err != null) {
			err.printStackTrace();
		}
		
		das.remove(msg.getSender());
		if(isLeader) {
			// forward to other das
			for(DAId id : das) {
				com.sendDatagramMessage(new OverlayErrorMessage(id, null));
			}
		}
		stop();
	}

	private void handleCheckConnectivity(CheckConnectivity msg) {
		if(isLeader) {
			for(Iterator<DAId> it = das.iterator(); it.hasNext();) {
				DAId id = it.next();
				
				DummyMessage dm = new DummyMessage(id);
				dm.setCallBack(this);
				com.sendDatagramMessage(new DummyMessage(id));
			}
		} else {
			com.sendDatagramMessage(new DummyMessage(ctrlId));
		}
	}

	private void handleBrokenDA(BrokenDA bda) throws InterruptedException, AgentException {
		DAId id = bda.getDAId();
		if(! isLeader && id.equals(ctrlId)) {
			agentPrintMessage("Broken leader, shutdown.");
			stop();
			return;
		}
	}

	private void handleCentralOverlayJoinAckMessage(
			CentralOverlayJoinAckMessage msg) {
		if(isLeader)
			throw new Error("Leader is not joining an overlay");
		
		agentPrintMessage("Overlay joined.");
		
		ctrlId = msg.getControllerId();
		waitForAck.release();
	}
	
	@Override
	public final void start() {
		throw new Error("Overlay must be started through init or join");
	}

	@Override
	public DistributedAgent getDA() {
		return da;
	}

	@Override
	public void leaveOverlay() throws OverlayException {
		agentPrintMessage("Leaving overlay...");
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

	public void checkConnectivity() {
		try {
			submitMessage(new CheckConnectivity());
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
	public void signalSent(Message m, boolean success) {
		if(! success) {
			signalBroken(new BrokenDA(m.getRecipient()));
		}
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
	}

	@Override
	public BarrierSyncInterface getBarrierSyncInterface() {
		return barrierAgent;
	}
	
	@Override
	protected void handleChildException(UncaughtThrowable t) {
		com.sendDatagramMessage(new OverlayErrorMessage(ctrlId, t));
		
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
		try {
			submitMessage(cbInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public DAId getLeaderId() {
		return ctrlId;
	}

	@Override
	public void shutdownOverlay() throws OverlayException {
		try {
			submitMessage(new ShutdownOverlay());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public FaultDetectionAgent getPingServiceInterface() {
		throw new Error("unimplemented");
	}

}
