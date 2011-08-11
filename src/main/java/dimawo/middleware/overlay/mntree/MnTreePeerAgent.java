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
package dimawo.middleware.overlay.mntree;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.agents.AgentException;
import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.FailureDetectionOutputStream;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.HeartBeat;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.distributedAgent.DistributedAgent;
import dimawo.middleware.overlay.BroadcastingAgentInterface;
import dimawo.middleware.overlay.BroadcastingAgentMessage;
import dimawo.middleware.overlay.LeaderElectionInterface;
import dimawo.middleware.overlay.OverlayException;
import dimawo.middleware.overlay.impl.decentral.DecentralOverlay;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Action;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Cause;
import dimawo.middleware.overlay.mntree.MnTreeLocalUpdate.Target;
import dimawo.middleware.overlay.mntree.events.BroadcastRequest;
import dimawo.middleware.overlay.mntree.events.CheckConnectivity;
import dimawo.middleware.overlay.mntree.events.SendToLeader;
import dimawo.middleware.overlay.mntree.events.ShutdownMnTree;
import dimawo.middleware.overlay.mntree.forwarding.ReliableForwarder;
import dimawo.middleware.overlay.mntree.forwarding.messages.ReliableForwarderMessage;
import dimawo.middleware.overlay.mntree.messages.CheckConnectivityMessage;
import dimawo.middleware.overlay.mntree.messages.ChooseNewMainMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeJoinAckMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeJoinForwardMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeJoinMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeMainPeerMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeMovePeerMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeNewChildMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeNewPeerMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeRemovePeersMessage;
import dimawo.middleware.overlay.mntree.messages.PropagateErrorMessage;
import dimawo.middleware.overlay.mntree.messages.ShutdownMnTreeMessage;
import dimawo.middleware.overlay.mntree.messages.UncheckedMnTreeMessage;
import dimawo.middleware.overlay.mntree.messages.UpdateChildRoutingTableMessage;
import dimawo.middleware.overlay.mntree.messages.UpdateParentRoutingTableMessage;
import dimawo.middleware.overlay.mntree.messages.UpdateRoutingTableMessage;
import dimawo.middleware.overlay.mntree.messages.UpdateThisRoutingTableMessage;
import dimawo.middleware.overlay.mntree.messages.WrongRouteMessage;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.SocketInterface;


/**
 * A MN-tree can be seen as a tree whose nodes are groups of computers.
 * Each group
 * of computer is called a <i>meta-node</i>. Each meta-node has exactly one
 * leader called <i>main peer</i>. The other computers of the meta-node are
 * simply called <i>peers</i>. A peer maintains the addresses of the other
 * peers of the meta-node including the main peer. The main peer maintains
 * the addresses of the other peers of the meta-node and, in addition, the
 * addresses of its neighboring main peers i.e. the main peers of father, left
 * son and right son meta-nodes (if they exist).
 * <p>
 * A detailed description is available
 * in the thesis of Gerard Dethier, <i>Design and Implementation of a
 * Distributed Lattice Boltzmann-based Fluid Flow Simulation Tool</i>, 2011.
 * 
 * @author Gerard Dethier
 */
public class MnTreePeerAgent extends LoggingAgent
implements
MOSCallBack,
BroadcastingAgentInterface,
LeaderElectionInterface {
	private DecentralOverlay over;
	private DistributedAgent da;
	private CommunicatorInterface com;
	
	private Timer connCheckTimer;
	
	private MnPeerState state;
	private LinkedList<MnTreeMessage> waitingState;
	
	private Semaphore waitJoinAck;
	
	private HashSet<MnTreeLocalUpdateCallBackInterface> toNotifyOnLocalUp;
	
	private ReliableForwarder relForw;
	

	public MnTreePeerAgent(DecentralOverlay parent, CommunicatorInterface com,
			DistributedAgent da) {
		super(parent, "MNTreePeerAgent");
		this.over = parent;
		this.da = da;
		this.com = com;
		
		waitingState = new LinkedList<MnTreeMessage>();
		
		toNotifyOnLocalUp = new HashSet<MnTreeLocalUpdateCallBackInterface>();
		
		relForw = new ReliableForwarder(this);
	}
	
	public void initOverlay(int maxNumOfChildren, int reliabilityThreshold) throws AgentException {
		state = MnPeerState.createRootState(da.getDaId(), maxNumOfChildren, reliabilityThreshold);
		agentPrintMessage("Init MN-tree");
		agentPrintMessage(state.toString());
		start();
	}
	
	public void joinOverlay(long millis, String hostName, int port) throws AgentException, OverlayException, InterruptedException {
		agentPrintMessage("Join address: "+hostName+":"+port);
		waitJoinAck = new Semaphore(0);
		start();
		
		register(hostName, port);
		
		if(! waitJoinAck.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
			try { stop(); } catch(Exception e) {}
			throw new OverlayException("Could not join MN-tree after "+millis+"ms");
		}
	}

	private void register(String hostName, int port) throws OverlayException {
		for(int i = 0; i < 3; ++i) {

			SocketInterface sock = null;
			try {

				SocketFactory sockFact = com.getSocketFactory();
				sock = sockFact.newSocket();
				sock.connect(sock.getSocketAddress(hostName, port), 20000);

				if( ! sock.isConnected()) {
					throw new IOException("Could not connect to join address: Time out.");
				}

				agentPrintMessage("Sending join request #"+i);
				FailureDetectionOutputStream out =
					new FailureDetectionOutputStream(sock, 10000);
				out.writeObject(new MnTreeJoinMessage(da.getDaId()));
				out.close();
				return; // Message sent -> registration finished

			} catch (IOException e) {

				agentPrintMessage("Could not identify DA ("+e+").");
				try {
					Thread.sleep(2000);
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
		
		agentPrintMessage("Could not join MN-tree.");
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}
	
	@Override
	protected void logAgentExit() {
		agentPrintMessage("exit");
		over.signalMnTreeDisconnected();
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		connCheckTimer = new Timer(true);
		connCheckTimer.schedule(new TimerTask() {
			public void run() {
				try {
					submitMessage(new CheckConnectivity());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 5000, 5000);
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(state != null && state.isDisconnected()) {
			agentPrintMessage("Peer disconnected, ignoring message "+o.getClass().getCanonicalName());
			return;
		}

		try {
			if(o instanceof UncheckedMnTreeMessage) {
				handleUncheckedMnTreeMessage(o);
			} else if(o instanceof MnTreeMessage) {
				if(state == null) {
					waitingState.addLast((MnTreeMessage) o);
					return;
				}
				
				MnTreeMessage mnm = (MnTreeMessage) o;
				MnId dest = mnm.getRecipientMn();
				if(dest == null)
					throw new Error("Destination MN id not set for message "+mnm.getClass().getName());

				if(! dest.equals(state.getThisMnId())) {
					agentPrintMessage("Wrongly routed message: "+
							o.getClass().getSimpleName());
					if(! (mnm instanceof WrongRouteMessage)) {
						com.sendDatagramMessage(new WrongRouteMessage(mnm.getSender(), state.getThisMnId(),
								mnm.getSenderMn(), mnm));
					}
					return;
				}
				handleCheckedMnTreeMessage(o);
			} else {
				handleEvent(o);
			}
		} catch (Throwable e) {
			agentPrintMessage("Following error occured during message handling:");
			agentPrintMessage(e);
			agentPrintMessage("Propagating error...");
			propagateError(e);
		}
	}

	private void propagateError(Throwable e) {
		TreeSet<DAId> allNeighbors = new TreeSet<DAId>();
		state.getAllNeighbors(allNeighbors);
		for(DAId id : allNeighbors) {
			com.sendDatagramMessage(new PropagateErrorMessage(id, e));
		}

		agentPrintMessage("Disconnecting tree.");
		onTreeDisconnected();
	}

	private void handleEvent(Object o) throws Exception {
		if(o instanceof BrokenDA) {
			handleBrokenDA((BrokenDA) o);
		} else if(o instanceof BroadcastRequest) {
			handleBroadcastRequest((BroadcastRequest) o);
		} else if(o instanceof MnTreeLocalUpdateCallBackInterface) {
			handleMnTreeLocalUpdateCallBackInterface((MnTreeLocalUpdateCallBackInterface) o);
		} else if(o instanceof SendToLeader) {
			handleSendToLeader((SendToLeader) o);
		} else if(o instanceof CheckConnectivity) {
			handleCheckConnectivity((CheckConnectivity) o);
		} else if(o instanceof ShutdownMnTree) {
			handleShutdownMnTree((ShutdownMnTree) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}
	
	private void handleShutdownMnTree(ShutdownMnTree o) {
		relForw.broadcastMessage(new ShutdownMnTreeMessage());
		agentPrintMessage("Received shutdown request");
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}

	private void handleCheckConnectivity(CheckConnectivity o) {
		if(state == null) {
			agentPrintMessage("Connectivity check skipped: waiting for join");
			return;
		}

		agentPrintMessage("Connectivity check.");
		if(state.isMainPeer(da.getDaId())) {
			// Check this MN peers
			TreeSet<DAId> thisPeers = new TreeSet<DAId>();
			state.getThisMnPeersIds(thisPeers);
			thisPeers.remove(da.getDaId());
			for(DAId id : thisPeers) {
				agentPrintMessage("-> "+id);
				com.sendDatagramMessage(new HeartBeat(id, this));
			}
			
			// Check parent main peers
			DAId parentId = state.getParentMnMainPeer();
			if(parentId != null) {
				agentPrintMessage("-> "+parentId);
				com.sendDatagramMessage(new HeartBeat(parentId, this));
			}
			
			// Check children peers
			for(int childIndex = 0; childIndex < state.getMaxNumOfChildren(); ++childIndex) {
				DAId childId = state.getChildMnMainPeer(childIndex);
				if(childId != null) {
					agentPrintMessage("-> "+childId);
					com.sendDatagramMessage(new HeartBeat(childId, this));
				}
			}
		} else {
			DAId mainPeer = state.getThisMnMainPeer();
			agentPrintMessage("-> "+mainPeer);
			com.sendDatagramMessage(new HeartBeat(mainPeer, this));
		}
	}

	private void handleSendToLeader(SendToLeader msg) {
		relForw.sendMessageToLeader(msg.getMessage());
	}

	private void handleMnTreeLocalUpdateCallBackInterface(
			MnTreeLocalUpdateCallBackInterface o) {
		if(this.toNotifyOnLocalUp.add(o) && state != null) {
			o.signalMnTreeLocalUpdate(new MnTreeLocalUpdate(null,
					Cause.init, null, null,
					state.clone()));
		}
	}

	private void handleBrokenDA(BrokenDA o) {
		DAId daId = o.getDAId();
		agentPrintMessage("Detected peer failure: "+daId);
		
		DAId[] removed = new DAId[]{daId};
		if(state.isMainPeer(da.getDaId())) {
			if(state.thisMnContains(daId)) {
				agentPrintMessage("This MN's normal peer failure.");
				
				// Update routing tables
				state.removePeersFromThisMn(daId);
				signalRemovedPeersToThisMnPeers(removed, true);
				signalRemovedPeersToParentMnPeers(removed, true);
				signalRemovedPeersToChildrenMnPeers(-1, removed, true);
				
				relForw.signalBrokenBrother(daId);
				
				// Signal topology change
				notifyLocalUpdate(daId,
						MnTreeLocalUpdate.Cause.leave,
						MnTreeLocalUpdate.Target.thisMn,
						MnTreeLocalUpdate.Action.remove, state.clone());
			} else if(daId.equals(state.getParentMnMainPeer())) {
				agentPrintMessage("Parent MN's main peer failure.");

				state.removePeersFromParentMn(removed);
				if(state.parentMnIsEmpty()) {
					onTreeDisconnected();
				} else {
					agentPrintMessage("New parent MN's main peer.");
					MnId parentId = state.getParentMnId();
					DAId parentMainPeer = state.getParentMnMainPeer();
					signalNewMainPeerChoice(daId, parentId, parentMainPeer);
					relForw.signalParentMnNewMainPeer();
				}
			} else {
				for(int i = 0; i < state.getMaxNumOfChildren(); ++i) {
					if(state.hasChild(i) &&
							daId.equals(state.getChildMnMainPeer(i))) {
						agentPrintMessage("Child "+i+" MN's main peer.");
						
						state.removePeersFromChildMn(i, removed);
						if(state.childMnIsEmpty(i)) {
							onTreeDisconnected();
						} else {
							agentPrintMessage("New child "+i+" MN's main peer.");
							MnId childId = state.getChildMnId(i);
							DAId childMainPeer = state.getChildMnMainPeer(i);
							signalNewMainPeerChoice(daId, childId, childMainPeer);
							relForw.signalChildMnNewMainPeer(i);
						}
					}
				}
			}
		} else if(! state.isMainPeer(da.getDaId()) &&
				state.getThisMnMainPeer().equals(daId)) {
			agentPrintMessage("This MN's main peer failure.");

			state.removePeersFromThisMn(removed);
			if(state.isMainPeer(da.getDaId())) {
				agentPrintMessage("This peer is the new main peer.");
				convertToMainPeer(); // relForw is notified
			} else {
				agentPrintMessage("A new main peer has been set.");
				MnId thisId = state.getThisMnId();
				DAId thisMainPeer = state.getThisMnMainPeer();
				signalNewMainPeerChoice(daId, thisId, thisMainPeer);
				relForw.signalThisMnNewMainPeer();
			}

			// Signal topology change
			notifyLocalUpdate(daId,
					MnTreeLocalUpdate.Cause.leave,
					MnTreeLocalUpdate.Target.thisMn,
					MnTreeLocalUpdate.Action.remove, state.clone());

		} else if(! state.thisMnContains(daId)) {
			agentPrintMessage("Detected failure in neighboring MN.");
			
			boolean[] ret = state.removePeersFromParentMn(removed);
			if(ret[0]) {
				if(state.parentMnIsEmpty()) {
					onTreeDisconnected();
				} else {
					agentPrintMessage("Mn-tree is disconnected");
				}
			}

			for(int i = 0; i < state.getMaxNumOfChildren(); ++i) {
				if(state.hasChild(i)) {
					ret = state.removePeersFromChildMn(i, removed);
					if(ret[0] && state.childMnIsEmpty(i)) {
						onTreeDisconnected();
					}
				}
			}
		}
	}

	/**
	 * @param replaced
	 * @param destMnId
	 * @param newMainPeer
	 */
	private void signalNewMainPeerChoice(DAId replaced, MnId destMnId,
			DAId newMainPeer) {
		MnId thisId = state.getThisMnId();
		ChooseNewMainMessage cnm = new ChooseNewMainMessage(newMainPeer,
				thisId, destMnId, replaced);
		com.sendDatagramMessage(cnm);
	}

	/**
	 * 
	 */
	private void onTreeDisconnected() {
		agentPrintMessage("Mn-tree is disconnected");
		state.setDisconnectedFlag(true);
		over.signalMnTreeDisconnected();
		try {
			stop();
		} catch (InterruptedException e) {
		} catch (AgentException e) {
		}
	}

	private void convertToMainPeer() {
		if(! state.isMainPeer(da.getDaId()))
			throw new Error("Must be main peer");

		// synchronize routing tables for this MN
		MnId thisId = state.getThisMnId();
		DAId[] thisTable = new DAId[state.getThisMnSize()];
		DAId mainPeer = da.getDaId();
		state.getThisMnPeersIds(thisTable);
		
		for(DAId brotherId : thisTable) {
			if(! brotherId.equals(mainPeer))
				com.sendDatagramMessage(new UpdateThisRoutingTableMessage(brotherId,
					thisId, thisId, mainPeer, thisTable));
		}
		
		MnId parentId = state.getParentMnId();
		if(parentId != null) {
			DAId parentMain = state.getParentMnMainPeer();
			int thisChildIndex = state.getThisChildIndex();
			com.sendDatagramMessage(new UpdateChildRoutingTableMessage(parentMain,
					thisId, parentId, thisChildIndex, mainPeer, thisTable));
		}
		
		int maxChildren = state.getMaxNumOfChildren();
		for(int i = 0; i < maxChildren; ++i) {
			MnId childId = state.getChildMnId(i);
			if(childId != null) {
				DAId childMain = state.getChildMnMainPeer(i);
				com.sendDatagramMessage(new UpdateParentRoutingTableMessage(childMain,
						thisId, childId, mainPeer, thisTable));
			}
		}


		// convert reliable forwarder
		relForw.convertToMainPeer();

		// TODO : notify barrier sync (?)
	}

	private void notifyLocalUpdate(DAId sub, Cause cause, Target targ, Action act, MnPeerState newState) {
		MnTreeLocalUpdate up = new MnTreeLocalUpdate(sub, cause, targ, act, newState);
		for(MnTreeLocalUpdateCallBackInterface cb : this.toNotifyOnLocalUp) {
			cb.signalMnTreeLocalUpdate(up);
		}
	}

	private void handleCheckedMnTreeMessage(Object o) throws Exception {
		if(o instanceof MnTreeMainPeerMessage) {
			if(! state.isMainPeer(da.getDaId())) {
				throw new Error("must be main peer");
			}
			handleMnTreeMainPeerMessage(o);
		} else if(o instanceof MnTreeMovePeerMessage) {
			handleMnTreeMovePeerMessage((MnTreeMovePeerMessage) o);
		} else if(o instanceof MnTreeRemovePeersMessage) {
			handleMnTreeRemovePeersMessage((MnTreeRemovePeersMessage) o);
		} else if(o instanceof MnTreeNewPeerMessage) {
			handleMnTreeNewPeerMessage((MnTreeNewPeerMessage) o);
		} else if(o instanceof MnTreeNewChildMessage) {
			handleMnTreeNewChildMessage((MnTreeNewChildMessage) o);
		} else if(o instanceof ReliableForwarderMessage) {
			relForw.handleReliableForwarderMessage(o);
		} else if(o instanceof WrongRouteMessage) {
			handleWrongRouteMessage((WrongRouteMessage) o);
		} else if(o instanceof UpdateRoutingTableMessage) {
			handleUpdateRoutingTableMessage((UpdateRoutingTableMessage) o);
		} else if(o instanceof ChooseNewMainMessage) {
			handleChooseNewMainMessage((ChooseNewMainMessage) o);
		} else if(o instanceof CheckConnectivityMessage) {
			// SKIP
		} else {
			throw new UnknownAgentMessage(o);
		}
	}
	
	private void handleChooseNewMainMessage(ChooseNewMainMessage o) {
		if(state.isMainPeer(da.getDaId())) {
			agentPrintMessage("Peer has already been converted into main.");
			return;
		}
		
		agentPrintMessage("This peer should be the new main peer.");
		agentPrintMessage("Current main peer: "+state.getThisMnMainPeer());
		agentPrintMessage("Replaced main peer: "+o.getReplacedPeer());
		state.removePeersFromThisMn(o.getReplacedPeer());
		agentPrintMessage("New main peer: "+state.getThisMnMainPeer());
		if(! state.isMainPeer(da.getDaId())) {
			throw new Error("No consensus reached on new main peer !!!");
		}
		
		convertToMainPeer(); // relForw is notified

		// Signal topology change
		notifyLocalUpdate(o.getReplacedPeer(),
				MnTreeLocalUpdate.Cause.leave,
				MnTreeLocalUpdate.Target.thisMn,
				MnTreeLocalUpdate.Action.remove, state.clone());
	}

	private void handleUpdateRoutingTableMessage(UpdateRoutingTableMessage o) {
		DAId newMainPeer = o.getMainPeer();
		DAId[] peers = o.getRoutingTable();
		
		if(o instanceof UpdateThisRoutingTableMessage) {
			agentPrintMessage("Received this routing table update from main peer.");
			DAId currentMainPeer = state.getThisMnMainPeer();
			state.updateThisRoutingTable(newMainPeer, peers);
			if(! currentMainPeer.equals(newMainPeer)) {
				agentPrintMessage("This main peer failure not already detected.");
				relForw.signalThisMnNewMainPeer();

				// Signal topology change
				notifyLocalUpdate(currentMainPeer,
						MnTreeLocalUpdate.Cause.leave,
						MnTreeLocalUpdate.Target.thisMn,
						MnTreeLocalUpdate.Action.remove, state.clone());
			}
		} else if(o instanceof UpdateParentRoutingTableMessage) {
			agentPrintMessage("Received parent routing table update.");
			DAId currentMainPeer = state.getParentMnMainPeer();
			state.updateParentRoutingTable(newMainPeer, peers);
			if(! currentMainPeer.equals(newMainPeer)) {
				agentPrintMessage("Parent main peer failure has not been already detected.");
				relForw.signalParentMnNewMainPeer();

				// Signal topology change
				notifyLocalUpdate(currentMainPeer,
						MnTreeLocalUpdate.Cause.leave,
						MnTreeLocalUpdate.Target.parentMn,
						MnTreeLocalUpdate.Action.remove, state.clone());
			}
		} else if(o instanceof UpdateChildRoutingTableMessage) {
			UpdateChildRoutingTableMessage ucrt = (UpdateChildRoutingTableMessage) o;
			int childIndex = ucrt.getChildIndex();
			
			agentPrintMessage("Received child routing table update.");
			DAId currentMainPeer = state.getChildMnMainPeer(childIndex);
			state.updateChildRoutingTable(childIndex, newMainPeer, peers);
			if(! currentMainPeer.equals(newMainPeer)) {
				agentPrintMessage("Child "+childIndex+
						" main peer failure has not been already detected.");
				relForw.signalParentMnNewMainPeer();

				// Signal topology change
				notifyLocalUpdate(currentMainPeer,
						MnTreeLocalUpdate.Cause.leave,
						MnTreeLocalUpdate.Target.childMn,
						MnTreeLocalUpdate.Action.remove, state.clone());
			}
		} else {
			throw new Error("Unknown routing table update message");
		}
	}

	private void handleWrongRouteMessage(WrongRouteMessage o) {
		MnTreeMessage msg = o.getMessage();
		
		if(msg instanceof ReliableForwarderMessage) {
			relForw.handleWrongRoute(o);
		} else {
			agentPrintMessage("Ignored wrong route error for message: "+msg.getClass().getName());
		}
	}

	private void handleMnTreeMainPeerMessage(Object o) throws UnknownAgentMessage {
		if(o instanceof MnTreeJoinForwardMessage) {
			handleMnTreeJoinForwardMessage((MnTreeJoinForwardMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}
	
	private void handleMnTreeNewChildMessage(MnTreeNewChildMessage o) {
		int childId = o.getChildIndex();
		agentPrintMessage("Setting new child "+childId);
		DAId main = o.getChildMainPeer();
		DAId[] peers = o.getChildPeers();
		state.setChildMn(childId, main, peers);
		
		this.notifyLocalUpdate(null, Cause.move , Target.childMn, Action.add, state.clone());
	}

	private void handleMnTreeNewPeerMessage(MnTreeNewPeerMessage o) {
		DAId daId = o.getNewPeer();
		Cause c;
		if(o.isJoin()) {
			c = Cause.join;
		} else {
			c = Cause.move;
		}
		if(o.sourceIsParent()) {
			state.addPeerToParentMn(daId);
			this.notifyLocalUpdate(daId, c, Target.parentMn, Action.add, state.clone());
		} else if(o.sourceIsChild()) {
			int childId = o.getSourceChildId();
			state.addPeerToChildMn(childId, daId);
			this.notifyLocalUpdate(daId, c, Target.childMn, Action.add, state.clone());
		} else if(o.sourceIsThis()) {
			state.addPeerToThisMn(daId);
			this.notifyLocalUpdate(daId, c, Target.thisMn, Action.add, state.clone());
		} else {
			throw new Error("Unknown source");
		}
		
		if(state.isMainPeer(da.getDaId())) {
			multicastMessageToThisMn(o);
		}
	}

	private void handleUncheckedMnTreeMessage(Object o) throws Throwable {
		if(o instanceof MnTreeJoinMessage) {
			handleMnTreeJoinMessage((MnTreeJoinMessage) o);
		} else if(o instanceof MnTreeJoinAckMessage) {
			handleMnTreeJoinAckMessage((MnTreeJoinAckMessage) o);
		} else if(o instanceof ReliableForwarderMessage) {
			relForw.handleReliableForwarderMessage(o);
		} else if(o instanceof ShutdownMnTreeMessage) {
			handleShutdownMnTreeMessage();
		} else if(o instanceof PropagateErrorMessage) {
			handlePropagateErrorMessage((PropagateErrorMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handlePropagateErrorMessage(PropagateErrorMessage o) {
		agentPrintMessage("Error to propagate (from "+o.getSender()+")");
		agentPrintMessage(o.getError());
		propagateError(o.getError());
	}

	private void handleShutdownMnTreeMessage() {
		agentPrintMessage("Received shutdown message");
		if(connCheckTimer != null) {
			connCheckTimer.cancel();
			connCheckTimer = null;
		}
		
		try {
			stop();
		} catch (Exception e) {
		}
	}

	private void handleMnTreeRemovePeersMessage(MnTreeRemovePeersMessage o) {
		DAId[] removedPeers = o.getRemovedPeers();
		Cause c;
		if(o.isLeave()) {
			c = Cause.leave;
		} else {
			c = Cause.move;
		}
		if(o.sourceIsParent()) {
			state.removePeersFromParentMn(removedPeers);
			agentPrintMessage("Removed peers from parent MN, new size="+state.getParentMnSize());
			this.notifyLocalUpdate(null, c, Target.parentMn, Action.remove, state.clone());
		} else if(o.sourceIsChild()) {
			int childId = o.getSourceChildId();
			state.removePeersFromChildMn(childId, removedPeers);
			agentPrintMessage("Removed peers from child MN "+childId+", new size="+state.getChildMnSize(childId));
			this.notifyLocalUpdate(null, c, Target.childMn, Action.remove, state.clone());
		} else if(o.sourceIsThis()) {
			state.removePeersFromThisMn(removedPeers);
			agentPrintMessage("Removed peers from this MN, new size="+state.getThisMnSize());
			this.notifyLocalUpdate(null, c, Target.thisMn, Action.remove, state.clone());
		} else {
			throw new Error("Unknown source");
		}
		
		if(state.isMainPeer(da.getDaId())) {
			multicastMessageToThisMn(o);
		}
	}

	private void handleMnTreeMovePeerMessage(MnTreeMovePeerMessage o) {
		if(state == null) {
			throw new Error("Cannot move non-inserted peer");
		}
		
		if(state.isMainPeer(da.getDaId())) {
			throw new Error("Cannot move main peer");
		}

		state = o.getNewState();
		
		agentPrintMessage("Peer moved to another MN:");
		agentPrintMessage(state.toString());
		
		// TODO : actions ?
		this.notifyLocalUpdate(da.getDaId(), Cause.move, Target.thisMn, null, state.clone());
	}

	private void signalNewPeerToThisMnPeers(DAId daId, boolean isJoin) {
		multicastMessageToThisMn(daId, new MnTreeNewPeerMessage(
				daId, MnTreeNewPeerMessage.Source.thisMn, isJoin));
	}
	
	private void signalNewPeerToParentMnPeers(DAId daId, boolean isJoin) {
		DAId parentDaId = state.getParentMnMainPeer();
		if(parentDaId == null)
			return;

		com.sendDatagramMessage(new MnTreeNewPeerMessage(parentDaId, state.getThisMnId(),
						state.getParentMnId(), daId, state.getThisChildIndex(), isJoin));
	}
	
	private void signalNewPeerToChildrenMnPeers(DAId daId, boolean isJoin) {
		int maxNumOfChildren = state.getMaxNumOfChildren();
		for(int childId = 0; childId < maxNumOfChildren; ++childId) {
			if(state.hasChild(childId)) {
				signalNewPeerToChildMnPeers(childId, daId, isJoin);
			}
		}
	}
	
	private void signalNewPeerToChildMnPeers(int childId, DAId daId, boolean isJoin) {
		DAId childDaId = state.getChildMnMainPeer(childId);
		MnId thisMnId = state.getThisMnId();
		MnId childMnId = state.getChildMnId(childId);
		com.sendDatagramMessage(new MnTreeNewPeerMessage(childDaId, thisMnId,
				childMnId, daId, MnTreeNewPeerMessage.Source.parentMn, isJoin));
	}

	private void handleMnTreeJoinAckMessage(MnTreeJoinAckMessage o) throws Throwable {
		if(state != null) {
			// peer has already joined
			throw new Error("peer already joined");
		}
		
		state = o.getInitialState();
		agentPrintMessage("Joined successfully:");
		agentPrintMessage(state.toString());

		if(! state.thisMnContains(da.getDaId()))
			throw new Error("This peer is not part of the MN");
		
		for(MnTreeMessage msg : waitingState) {
			handleMessage(msg);
		}
		waitingState.clear();
		
		waitJoinAck.release(); // release join method

		this.notifyLocalUpdate(null, Cause.init, null, null, state.clone());
	}

	private void handleMnTreeJoinForwardMessage(MnTreeJoinForwardMessage o) {
		tryInsert(o.getOriginalJoinMessage());
	}
	
	private void handleMnTreeJoinMessage(MnTreeJoinMessage o) {
		if(! state.isMainPeer(da.getDaId())) {
			// forward to main peer
			DAId main = state.getThisMnMainPeer();
			MnId thisMnId = state.getThisMnId();
			com.sendDatagramMessage(new MnTreeJoinForwardMessage(main,
					thisMnId, thisMnId, o));
			return;
		}

		tryInsert(o);
	}

	private void tryInsert(MnTreeJoinMessage o) {
		if(! state.thisMnIsReliable() ||
				! state.hasMaxNumOfChildren() ||
				state.getMaxNumOfChildren() == 0) {
			agentPrintMessage("thisIsReliable="+state.thisMnIsReliable());
			agentPrintMessage("hasMaxNumOfChildren="+state.hasMaxNumOfChildren());
			acceptJoin(o);
		} else {
			// forward to a randomly selected child
			int childId = state.getNextInsertChildIndex();
			DAId main = state.getChildMnMainPeer(childId);
			com.sendDatagramMessage(new MnTreeJoinForwardMessage(main,
					state.getThisMnId(), state.getChildMnId(childId), o));
		}
	}

	private void acceptJoin(MnTreeJoinMessage o) {
		DAId joiningDa = o.getSender();

		int reliabilityThreshold = state.getReliabilityThreshold();
		if(state.getMaxNumOfChildren() > 0 &&
				state.getThisMnSize() == 2*reliabilityThreshold - 1) {
			// Create a new meta-node
			DAId[] newMnPeers = new DAId[reliabilityThreshold];
			state.extractYoungestPeersFromThisMn(newMnPeers, 0, reliabilityThreshold - 1);

			newMnPeers[newMnPeers.length - 1] = joiningDa;

			// Set new child meta-node
			int newChildId = state.getNextChildId();
			state.setChildMn(newChildId, newMnPeers[0], newMnPeers);
			signalNewChildToThisMnPeers(newChildId, newMnPeers[0], newMnPeers);
			
			agentPrintMessage("Splitting MN and setting child "+newChildId);
			agentPrintMessage(state.toString());
			
			// newMnPeers[0] = move peer and make main peer of new MN
			MnPeerState baseState = state.getNewChildState(newChildId,
					newMnPeers[0], newMnPeers);
			
			MnPeerState mainPeerState = baseState.clone();
			
			MnId thisMnId = state.getThisMnId();
			if(newMnPeers.length > 1)
				com.sendDatagramMessage(new MnTreeMovePeerMessage(newMnPeers[0],
						thisMnId, thisMnId, mainPeerState));


			// newMnPeers[1..newMnPeers.length - 2] = move peers
			for(int i = 1; i < newMnPeers.length - 1; ++i) {
				com.sendDatagramMessage(new MnTreeMovePeerMessage(newMnPeers[i],
					thisMnId, thisMnId, baseState));
			}

			// newMnPeers[newMnPeers.length - 1] = insert in new MN
			if(newMnPeers.length > 1)
				com.sendDatagramMessage(new MnTreeJoinAckMessage(joiningDa, baseState));
			else
				com.sendDatagramMessage(new MnTreeJoinAckMessage(joiningDa, mainPeerState));
			
			// Signal changes to "old" neighboring meta-nodes
			signalRemovedPeersToThisMnPeers(newMnPeers, false);
			signalRemovedPeersToParentMnPeers(newMnPeers, false);
			signalRemovedPeersToChildrenMnPeers(newChildId, newMnPeers, false);
			
			this.notifyLocalUpdate(null, Cause.move, Target.childMn, Action.remove, state.clone());
		} else {
			agentPrintMessage("Adding peer to this MN");
			state.addPeerToThisMn(joiningDa);
			agentPrintMessage(state.toString());
			
			// Insert in this meta-node
			MnPeerState state = getStateForInsertion();
	
			// prepare insertion data
			MnTreeJoinAckMessage joinAck = new MnTreeJoinAckMessage(joiningDa, state);
			com.sendDatagramMessage(joinAck);
			
			signalNewPeerToThisMnPeers(joiningDa, true);
			signalNewPeerToParentMnPeers(joiningDa, true);
			signalNewPeerToChildrenMnPeers(joiningDa, true);
			
			this.notifyLocalUpdate(joiningDa, Cause.join, Target.thisMn, Action.add, this.state.clone());
		}
	}

	private void signalNewChildToThisMnPeers(int newChildId, DAId main,
			DAId[] childPeers) {
		multicastMessageToThisMn(new MnTreeNewChildMessage(newChildId,
				main, childPeers));
	}

	private void signalRemovedPeersToChildrenMnPeers(int newChildId,
			DAId[] newMnPeers, boolean isLeave) {
		for(int childId = 0; childId < state.getMaxNumOfChildren(); ++childId) {
			if(childId != newChildId && state.hasChild(childId)) {
				signalRemovedPeersToChildMnPeer(childId, newMnPeers, isLeave);
			}
		}
	}

	private void signalRemovedPeersToChildMnPeer(int childId, DAId[] newMnPeers, boolean isLeave) {
		DAId childDaId = state.getChildMnMainPeer(childId);
		if(childDaId == null)
			return;

		MnId thisMnId = state.getThisMnId();
		MnId childMnId = state.getChildMnId(childId);
		com.sendDatagramMessage(new MnTreeRemovePeersMessage(childDaId, thisMnId,
						childMnId, MnTreeRemovePeersMessage.Source.parentMn, newMnPeers, isLeave));
	}

	private void signalRemovedPeersToParentMnPeers(DAId[] newMnPeers, boolean isLeave) {
		DAId parentDaId = state.getParentMnMainPeer();
		if(parentDaId == null)
			return;

		MnId thisMnId = state.getThisMnId();
		MnId parentMnId = state.getParentMnId();
		com.sendDatagramMessage(new MnTreeRemovePeersMessage(parentDaId, thisMnId,
						parentMnId, state.getThisChildIndex(), newMnPeers, isLeave));
	}

	private void signalRemovedPeersToThisMnPeers(DAId[] newMnPeers, boolean isLeave) {
		multicastMessageToThisMn(new MnTreeRemovePeersMessage(MnTreeRemovePeersMessage.Source.thisMn, newMnPeers, isLeave));
	}

	private MnPeerState getStateForInsertion() {
		MnPeerState state = this.state.clone();
		return state;
	}
	
	public void submitMnTreeMessage(MnTreeMessage msg) {
		try {
			submitMessage(msg);
		} catch(InterruptedException e) {
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
			try {
				submitMessage(new BrokenDA(m.getRecipient()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void broadcastMessage(Message msg) {
		try {
			submitMessage(new BroadcastRequest(msg));
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void submitBroadcastingAgentMessage(BroadcastingAgentMessage msg) {
		try {
			submitMessage(msg);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastRequest(BroadcastRequest o) {
		relForw.broadcastMessage(o.getMessageToBroadcast());
	}

	private void multicastMessageToThisMn(MnTreeMessage msg) {
		TreeSet<DAId> addr = new TreeSet<DAId>();
		state.getThisMnPeersIds(addr);
		addr.remove(da.getDaId());
		
		if(addr.size() == 0)
			return;
		
		DAId[] addrArr = new DAId[addr.size()];
		addr.toArray(addrArr);
		
		msg.setSenderMn(state.getThisMnId());
		msg.setRecipientMn(state.getThisMnId());
		
		com.multicastMessage(addrArr, msg);
	}
	
	private void multicastMessageToThisMn(DAId toExclude, MnTreeMessage msg) {
		TreeSet<DAId> addr = new TreeSet<DAId>();
		state.getThisMnPeersIds(addr);
		addr.remove(da.getDaId());
		addr.remove(toExclude);
		
		if(addr.size() == 0)
			return;
		
		DAId[] addrArr = new DAId[addr.size()];
		addr.toArray(addrArr);
		
		msg.setSenderMn(state.getThisMnId());
		msg.setRecipientMn(state.getThisMnId());
		
		com.multicastMessage(addrArr, msg);
	}
	
	public void register(MnTreeLocalUpdateCallBackInterface cbInt) {
		try {
			submitMessage(cbInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendMessageToLeader(Message msg) {
		try {
			submitMessage(new SendToLeader(msg));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean isMainPeer() {
		return state.isMainPeer(da.getDaId());
	}

	public MnPeerState getPeerState() {
		return state;
	}

	public CommunicatorInterface getCommunicator() {
		return com;
	}

	public DistributedAgent getDistributedAgent() {
		return da;
	}

	public void signalShutdown() {
		try {
			submitMessage(new ShutdownMnTree());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
