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
package dimawo.middleware.sharedMap.dht.chord;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.agents.UnknownAgentMessage;
import dimawo.agents.events.AsynchronousCall;
import dimawo.middleware.commonEvents.BrokenDA;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.communication.outputStream.MOSCallBack;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.OverlayException;
import dimawo.middleware.overlay.SharedMapEntry;
import dimawo.middleware.overlay.Update;
import dimawo.middleware.overlay.impl.decentral.DecentralOverlay;
import dimawo.middleware.sharedMap.AbstractSharedMapAgent;
import dimawo.middleware.sharedMap.dht.chord.callstates.CallState;
import dimawo.middleware.sharedMap.dht.chord.callstates.GetCallState;
import dimawo.middleware.sharedMap.dht.chord.callstates.JoinCallState;
import dimawo.middleware.sharedMap.dht.chord.callstates.PutCallState;
import dimawo.middleware.sharedMap.dht.chord.callstates.RemoveCallState;
import dimawo.middleware.sharedMap.dht.chord.callstates.UpdateCallState;
import dimawo.middleware.sharedMap.dht.chord.events.RequestTO;
import dimawo.middleware.sharedMap.dht.chord.events.RoutingTO;
import dimawo.middleware.sharedMap.dht.chord.events.TriggerCallRouting;
import dimawo.middleware.sharedMap.dht.chord.events.TriggerStabilization;
import dimawo.middleware.sharedMap.dht.chord.messages.CallStateMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ChordAgentMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ChordJoinMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ChordJoinResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetDataEntryResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetNextHopMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetPredecessorMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.GetPredecessorResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.PutDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.PutDataEntryResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RemoveDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RemoveDataEntryResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RemoveReplicaMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.ReplicateDataMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.RoutingResultMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.SetDataMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.SetSuccessorsMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.StabilizationAlgorithmMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.UpdateDataEntryMessage;
import dimawo.middleware.sharedMap.dht.chord.messages.UpdateDataEntryResultMessage;
import dimawo.middleware.sharedMap.events.GetValue;
import dimawo.middleware.sharedMap.events.PutValue;
import dimawo.middleware.sharedMap.events.RemoveValue;
import dimawo.middleware.sharedMap.events.UpdateValue;




public class ChordAgent extends AbstractSharedMapAgent implements MOSCallBack {
	private CommunicatorInterface com;
	private Semaphore waitJoinAck;
	private int maxSuccessors = 5;
	
	private ChordTableEntry thisEntry;
	private ChordRoutingTable table;
	private ChordDataTable data;

	private LinkedList<AsynchronousCall> pendingActions;
	private CallState currentCallState;
	
	private LinkedList<ChordAgentMessage> pendingRequests;

	private ChordRoutingAlgorithm routing;
	private StabilizationAlgorithm stab;
	private Timer stabTimer;
	private long stabPeriod;
	private boolean successorsChanged;
	
	private TreeSet<DAId> brokenPeers;


	public ChordAgent(DecentralOverlay over, int nSuccessors, boolean init,
			DAId contact) {
		super(over, "ChordAgent");
		com = over.getCommunicator();
		
		DAId thisDaId = over.getDA().getDaId();
		thisEntry = new ChordTableEntry(thisDaId, new ChordId(thisDaId));
		table = new ChordRoutingTable(thisEntry, nSuccessors);
		if(init)
			table.thisIsInit();
		data = new ChordDataTable();
		
		pendingActions = new LinkedList<AsynchronousCall>();
		pendingRequests = new LinkedList<ChordAgentMessage>();
		
		routing = new ChordRoutingAlgorithm(this);
		stab = new StabilizationAlgorithm(this);
		stabPeriod = 1000; // ms
		successorsChanged = false;

		if(! init) {
			waitJoinAck = new Semaphore(0);
			currentCallState = new JoinCallState(contact, this);
		}
		
		brokenPeers = new TreeSet<DAId>();
	}
	
	@Override
	protected void logAgentExit() {
		for(Iterator<AsynchronousCall> it = pendingActions.iterator(); it.hasNext();) {
			AsynchronousCall call = it.next();
			call.signalError(new Exception("DMap agent closed"));
		}
		agentPrintMessage("exit");
	}

	@Override
	protected void init() throws Throwable {
		agentPrintMessage("init");
		if(currentCallState != null) {
			executeCurrentState(); // join
		} else {
			scheduleStabilization();
		}
	}

	@Override
	protected void handleSpecificMessage(Object o) throws Exception {
		if(o instanceof GetNextHopMessage) {
			handleGetNextHopMessage((GetNextHopMessage) o);
		} else if(o instanceof RoutingResultMessage) {
			routing.handleRoutingResultMessage((RoutingResultMessage) o);
		} else if(o instanceof GetDataEntryMessage) {
			handleGetDataEntryMessage((GetDataEntryMessage) o);
		} else if(o instanceof PutDataEntryMessage) {
			handlePutDataEntryMessage((PutDataEntryMessage) o);
		} else if(o instanceof CallStateMessage) {
			handleCallStateMessage((CallStateMessage) o);
		} else if(o instanceof ChordJoinMessage) {
			handleChordJoinMessage((ChordJoinMessage) o);
		} else if(o instanceof UpdateDataEntryMessage) {
			handleUpdateDataEntryMessage((UpdateDataEntryMessage) o);
		} else if(o instanceof RemoveDataEntryMessage) {
			handleRemoveDataEntryMessage((RemoveDataEntryMessage) o);
		} else if(o instanceof GetPredecessorMessage) {
			handleGetPredecessorMessage((GetPredecessorMessage) o);
		} else if(o instanceof StabilizationAlgorithmMessage) {
			stab.handleStabilizationAlgorithmMessage((StabilizationAlgorithmMessage) o);
		} else if(o instanceof SetPredecessorMessage) {
			handleSetPredecessorMessage((SetPredecessorMessage) o);
		} else if(o instanceof TriggerStabilization) {
			handleTriggerStabilization((TriggerStabilization) o);
		} else if(o instanceof SetDataMessage) {
			handleSetDataMessage((SetDataMessage) o);
		} else if(o instanceof BrokenDA) {
			handleBrokenDA((BrokenDA) o);
		} else if(o instanceof SetSuccessorsMessage) {
			handleSetSuccessorsMessage((SetSuccessorsMessage) o);
		} else if(o instanceof ReplicateDataMessage) {
			handleReplicateDataMessage((ReplicateDataMessage) o);
		} else if(o instanceof RemoveReplicaMessage) {
			handleRemoveReplicaMessage((RemoveReplicaMessage) o);
		} else if(o instanceof TriggerCallRouting) {
			handleTriggerCallRouting((TriggerCallRouting) o);
		} else if(o instanceof RoutingTO) {
			routing.signalTO((RoutingTO) o);
		} else if(o instanceof RequestTO) {
			if(currentCallState != null) {
				currentCallState.signalTO((RequestTO) o);
			}
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleTriggerCallRouting(TriggerCallRouting o) {
		if(currentCallState != null) {
			currentCallState.triggerRouting();
		}
	}

	private void handleRemoveReplicaMessage(RemoveReplicaMessage o) {
		String key = o.getKey();
		data.removeEntry(new ChordId(key), key);
	}

	private void handleReplicateDataMessage(ReplicateDataMessage o) {
		agentPrintMessage("Got data replica from "+o.getSender());
		SharedMapEntry[] content = o.getContent();
		for(SharedMapEntry e : content) {
			agentPrintMessage("-- "+e.getKey());
			data.putReplica(new ChordId(e.getKey()), e);
		}
	}

	private void handleSetSuccessorsMessage(SetSuccessorsMessage o) {
		DAId daFrom = o.getSender();
		ChordTableEntry succ = table.getSuccEntry();
		if(succ == null || ! daFrom.equals(succ.getDaId())) {
			agentPrintMessage("Ignored successors list from obsolete successor");
			return;
		}
		
		LinkedList<ChordTableEntry> recvSucc = o.getSuccessors();
		for(Iterator<ChordTableEntry> it = recvSucc.iterator(); it.hasNext();) {
			ChordTableEntry e = it.next();
			if(brokenPeers.contains(e.getDaId())) {
				it.remove();
			}
		}
		
		if(recvSucc.isEmpty()) {
			agentPrintMessage("All received successors are broken.");
			return;
		}
		
		agentPrintMessage("Received "+recvSucc.size()+" successors.");
		
		Collection<ChordTableEntry> newSucc =
			table.updateSuccessors(recvSucc);
		
		if(newSucc.size() > 0) {
			agentPrintMessage(newSucc.size()+" new successors set");
			agentPrintMessage(newSucc.toString());
		
			// Replicate data on new successors
			SharedMapEntry[] content = new SharedMapEntry[data.getContent().size()];
			data.getContent().toArray(content);
			replicateEntry(newSucc, content);
			
//			propagateSuccessorsList();
			successorsChanged = true;
		}
	}

	private void handleBrokenDA(BrokenDA o) {
		DAId id = o.getDAId();
		agentPrintMessage("DA "+id+" is broken.");
		if(! brokenPeers.add(id)) {
			agentPrintMessage("-- already part of broken set");
//			return; // disabled: routing can still use broken id
		}

		routing.signalBroken(id);
		
		boolean removed = table.remove(id);
		if(table.isBroken())
			throw new Error("Ring disconnected");

		if(removed && table.getPrecEntry() != null) {
			// Removed id from successors list
			successorsChanged = true;
		}
		
		routing.restartRouting();
		if(currentCallState != null)
			currentCallState.signalBrokenDA(o.getDAId());
	}

	private void propagateSuccessorsList() {
		if(! table.hasPredecessor())
			return;
		
		// Send successors list
		LinkedList<ChordTableEntry> succCopy = new LinkedList<ChordTableEntry>();
		succCopy.addAll(table.getSuccessors());
		if(! succCopy.getFirst().equals(thisEntry))
			succCopy.addFirst(thisEntry);
		while(succCopy.size() > maxSuccessors)
			succCopy.removeLast();
		ChordTableEntry prec = table.getPrecEntry();
		sendMessage(new SetSuccessorsMessage(prec.getDaId(),
				thisEntry.getChordId(), prec.getChordId(), succCopy));
	}

	private void handleSetDataMessage(SetDataMessage o) {
		agentPrintMessage("Data table update");
		data.putEntries(o.getContent());
		
		if(table.hasPredecessor()) {
			data.cleanTable(table.getPrecEntry().getChordId(),
					thisEntry.getChordId());
		}
		
		data.printKeys(this);
	}

	private void handleTriggerStabilization(TriggerStabilization o) {
		stab.trigger();
	}

	private void handleSetPredecessorMessage(SetPredecessorMessage o) throws Exception {
		ChordId newPrecId = o.getChordFrom();
		ChordTableEntry curPrec = table.getPrecEntry();
		boolean updatePrec = curPrec == null ||
		newPrecId.isInInterval(curPrec.getChordId(), thisEntry.getChordId());
		if(updatePrec) {
			agentPrintMessage("Updating predecessor");
			table.setPredecessor(new ChordTableEntry(o.getSender(), newPrecId));
			table.print(this);
			
			// Send data to new predecessor
			sendMessage(new SetDataMessage(o.getSender(), thisEntry.getChordId(),
					o.getChordFrom(),
					data.getContent()));
			
			data.migrateReplicas(newPrecId, thisEntry.getChordId());
			data.cleanTable(newPrecId, thisEntry.getChordId());
			
			onPredecessorUpdate();
		}
		
		if(updatePrec || successorsChanged) {
			propagateSuccessorsList();
		}
	}

	private void handleGetPredecessorMessage(GetPredecessorMessage o) {
//		agentPrintMessage("Get prec request from "+o.getSender());
		sendMessage(new GetPredecessorResultMessage(o.getSender(),
				thisEntry.getChordId(), o.getChordFrom(), table.getPrecEntry()));
	}

	private void handleRemoveDataEntryMessage(RemoveDataEntryMessage o)
	throws BrokenRoutingTableException {
		agentPrintMessage("Remove request from "+o.getSender());
		if(! table.hasPredecessor()) {
			agentPrintMessage("-- request queued: broken routing table");
			pendingRequests.addLast(o);
			return;
		}

		ChordId id = o.getKeyId();
		String key = o.getKey();
		if(! table.isDestination(id)) {
			sendMessage(new RemoveDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key, true));
		} else {
			data.removeEntry(id, key);
			
			removeReplicas(key);
			
			sendMessage(new RemoveDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key, false));
		}
	}

	private void handleUpdateDataEntryMessage(UpdateDataEntryMessage o)
	throws BrokenRoutingTableException {
		agentPrintMessage("Update request from "+o.getSender());
		if(! table.hasPredecessor()) {
			agentPrintMessage("-- request queued: broken routing table");
			pendingRequests.addLast(o);
			return;
		}

		ChordId id = o.getKeyId();
		String key = o.getKey();
		Update up = o.getUpdate();
		SharedMapEntry en = o.getEntry();
		if(! table.isDestination(id)) {
			agentPrintMessage("-- routing error");
			sendMessage(new UpdateDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key, null, true));
		} else {
			agentPrintMessage("-- update "+key);
			SharedMapEntry updated = data.updateEntry(id, key, up, en);
			if(updated != null) {
				agentPrintMessage("-- updated");
				replicateEntry(table.getSuccessors(),
						new SharedMapEntry[]{updated});
			} else
				agentPrintMessage("-- not updated");
			
			
			
			sendMessage(new UpdateDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key,
					(updated != null) ? updated.clone() : null,
					false));
		}
	}

	private void replicateEntry(Collection<ChordTableEntry> succs, SharedMapEntry[] data) {
		for(ChordTableEntry e : succs) {
			if(! e.equals(thisEntry))
				sendMessage(new ReplicateDataMessage(e.getDaId(),
					thisEntry.getChordId(), e.getChordId(), data));
		}
	}
	
	private void removeReplicas(String key) {
		LinkedList<ChordTableEntry> succs = table.getSuccessors();
		for(ChordTableEntry e : succs) {
			if(! e.equals(thisEntry))
				sendMessage(new RemoveReplicaMessage(e.getDaId(),
					thisEntry.getChordId(), e.getChordId(), key));
		}
	}

	private void handleChordJoinMessage(ChordJoinMessage o) throws BrokenRoutingTableException {
		agentPrintMessage("Join request from "+o.getSender());

		ChordId joinId = o.getChordFrom();
		DAId joinDaId = o.getSender();
		if(succIsDestination(joinId)) {
			sendMessage(new ChordJoinResultMessage(joinDaId,
					thisEntry.getChordId(),
					joinId,
					table.getSuccEntry(),
					true));
		} else {
			ChordTableEntry nextHop = table.getNextHop(o.getChordFrom());
			sendMessage(new ChordJoinResultMessage(joinDaId,
					thisEntry.getChordId(),
					joinId,
					nextHop,
					false));
		}
	}

	private void handlePutDataEntryMessage(PutDataEntryMessage o) throws BrokenRoutingTableException {
		agentPrintMessage("Put request from "+o.getSender());
		if(! table.hasPredecessor()) {
			agentPrintMessage("-- request queued: broken routing table");
			pendingRequests.addLast(o);
			return;
		}

		ChordId id = o.getKeyChordId();
		SharedMapEntry en = o.getEntry();
		if(! table.isDestination(id)) {
			sendMessage(new PutDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), en, true));
		} else {
			agentPrintMessage("-- Putting entry "+o.getEntry().getKey());
			data.putEntry(id, en);
			replicateEntry(table.getSuccessors(),
					new SharedMapEntry[]{en});
			sendMessage(new PutDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), en, false));
		}
	}

	private void handleCallStateMessage(CallStateMessage o) throws Exception {
		if(currentCallState != null) {
			currentCallState.handleMessage(o);
			checkIfStateFinished();
		} else {
			agentPrintMessage("No call state for message "+o.getClass().getName());
		}
	}

	private void checkIfStateFinished() {
		if(currentCallState.isFinished()) {
			if(! pendingActions.isEmpty()) {
				executeCall(pendingActions.removeLast());
			} else {
				currentCallState = null;
			}
		}
	}

	private void handleGetDataEntryMessage(GetDataEntryMessage o)
	throws BrokenRoutingTableException {
		agentPrintMessage("Get request from "+o.getSender());
		ChordId id = o.getDataChordId();
		if(! table.hasPredecessor()) {
			agentPrintMessage("-- request queued: broken routing table");
			pendingRequests.addLast(o);
			return;
		}

		String key = o.getDataKey();
		if(! table.isDestination(id)) {
			agentPrintMessage("-- wrong route");
			sendMessage(new GetDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key, null, true));
		} else {
			agentPrintMessage("-- getting "+key);
			SharedMapEntry dataEn = data.getEntry(id, key);
			if(dataEn != null)
				agentPrintMessage("-- found");
			else
				agentPrintMessage("-- not found");
			sendMessage(new GetDataEntryResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key,
					(dataEn != null) ? dataEn.getValue() : null, false));
		}
	}

	private void handleGetNextHopMessage(GetNextHopMessage o) {
//		agentPrintMessage("Next hop request from "+o.getSender());
		ChordId key = o.getSearchedForKey();
		if(succIsDestination(key)) {
			com.sendDatagramMessage(new RoutingResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key,
					true, table.getSuccEntry()));
		} else {
			ChordTableEntry en = table.getNextHop(key);
			com.sendDatagramMessage(new RoutingResultMessage(o.getSender(),
					thisEntry.getChordId(), o.getChordFrom(), key,
					false, en));
		}
	}

	private void handleAsynchronousCall(AsynchronousCall call) {
		if(pendingActions.isEmpty() && currentCallState == null) {
			executeCall(call);
		} else {
			pendingActions.addFirst(call);
		}
	}

	private void executeCall(AsynchronousCall call) {
		setCurrentState(call);
		executeCurrentState();
	}

	/**
	 * @throws Error
	 */
	private void executeCurrentState() throws Error {
		currentCallState.init();
		while(currentCallState != null && currentCallState.isFinished()) {
			if(pendingActions.isEmpty()) {
				currentCallState = null;
			} else {
				setCurrentState(pendingActions.removeLast());
				currentCallState.init();
			} 
		}
		// Post: currentCallState == null || ! currentCallState.isFinished()
	}

	/**
	 * @param call
	 * @throws Error
	 */
	private void setCurrentState(AsynchronousCall call) throws Error {
		if(call instanceof GetValue) {
			currentCallState = new GetCallState((GetValue) call, this);
		} else if(call instanceof PutValue) {
			currentCallState = new PutCallState((PutValue) call, this);
		} else if(call instanceof UpdateValue) {
			currentCallState = new UpdateCallState((UpdateValue) call, this);
		} else if(call instanceof RemoveValue) {
			currentCallState = new RemoveCallState((RemoveValue) call, this);
		} else {
			throw new Error("Unhandled call");
		}
	}

	public SharedMapEntry getLocalEntry(ChordId id, String key) {
		return data.getEntry(id, key);
	}

	public boolean succIsDestination(ChordId key) {
		return table.succIsDestination(key);
	}

	public ChordId getAgentChordId() {
		return thisEntry.getChordId();
	}

	public void sendMessage(ChordAgentMessage msg) {
		msg.setCallBack(this);
		if(msg.getRecipient().equals(thisEntry.getDaId())) {
			// Loopback
			msg.setSender(thisEntry.getDaId());
			try {
				this.submitMessage(msg);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			msg.setMessageSent(true);
		} else {
			com.sendDatagramMessage(msg);
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

	public void scheduleStabilization() {
		stabTimer = new Timer(true);
		stabTimer.schedule(new TimerTask() {
			public void run() {
				try {
					submitMessage(new TriggerStabilization());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, stabPeriod, stabPeriod);
	}

	@Override
	public void signalSent(Message m, boolean success) {
		if(! success) {
			signalBroken(new BrokenDA(m.getRecipient()));
		}
	}

	public ChordDataTable getDataTable() {
		return data;
	}
	
	public ChordRoutingAlgorithm getRoutingAlgorithm() {
		return routing;
	}

	public void printMessage(String string) {
		agentPrintMessage(string);
	}

	@Override
	protected void handlePutValue(PutValue o) throws Exception {
		handleAsynchronousCall(o);
	}

	@Override
	protected void handleUpdateValue(UpdateValue o) throws Exception {
		handleAsynchronousCall(o);
	}

	@Override
	protected void handleRemoveValue(RemoveValue o) throws Exception {
		handleAsynchronousCall(o);
	}

	@Override
	protected void handleGetValue(GetValue o) throws Exception {
		handleAsynchronousCall(o);
	}

	public ChordRoutingTable getRoutingTable() {
		return table;
	}

	public void signalDestination(ChordTableEntry en) {
		if(currentCallState != null) {
			currentCallState.setDestination(en);
			checkIfStateFinished();
		} else {
			agentPrintMessage("Ignored destination, no call state.");
		}
	}

	public ChordTableEntry getThisChordTableEntry() {
		return thisEntry;
	}

	public void initRoutingTable(ChordTableEntry succ) throws Exception {
		table.setSuccessor(succ);
	}

	private void onPredecessorUpdate() throws Exception {
		handlePendingRequests();
	}

	private void handlePendingRequests() throws Exception {
		LinkedList<ChordAgentMessage> tmp = new LinkedList<ChordAgentMessage>();
		tmp.addAll(pendingRequests);
		pendingRequests.clear();

		for(ChordAgentMessage m : tmp) {
			handleSpecificMessage(m);
		}
	}

	public void initDataTable(LinkedList<SharedMapEntry> initData) {
		data.putEntries(initData);
	}

	public void waitForJoin(long millis) throws InterruptedException, OverlayException {
		if(! waitJoinAck.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
			try { stop(); } catch(Exception e) {}
			throw new OverlayException("Could not join Chord after "+millis+"ms");
		}
	}

	public void signalJoined() {
		waitJoinAck.release();
	}

	public void updateSuccessor(ChordTableEntry succPrec) {
		if(! brokenPeers.contains(succPrec.getDaId()) &&
				! succPrec.equals(table.getSuccEntry())) {
			table.setSuccessor(succPrec);
		}
	}

	public void scheduleNewCallRouting() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				try {
					submitMessage(new TriggerCallRouting());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void submitRoutingTO(int id) {
		try {
			submitMessage(new RoutingTO(id));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void submitCallRequestTO(String callType, DAId dest, String key) {
		try {
			submitMessage(new RequestTO(callType, dest, key));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
