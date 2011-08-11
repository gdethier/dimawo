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
package dimawo.middleware.overlay.mntree.forwarding;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import dimawo.agents.UnknownAgentMessage;
import dimawo.middleware.communication.CommunicatorInterface;
import dimawo.middleware.communication.Message;
import dimawo.middleware.distributedAgent.DAId;
import dimawo.middleware.overlay.mntree.MnId;
import dimawo.middleware.overlay.mntree.MnPeerState;
import dimawo.middleware.overlay.mntree.MnTreePeerAgent;
import dimawo.middleware.overlay.mntree.forwarding.messages.FirstStageAckMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.FirstStageMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.RemoveFromThirdStageCacheMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.SecondStageAckMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.SecondStageMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.ThirdStageAckMessage;
import dimawo.middleware.overlay.mntree.forwarding.messages.ThirdStageMessage;
import dimawo.middleware.overlay.mntree.messages.MnTreeMessage;
import dimawo.middleware.overlay.mntree.messages.WrongRouteMessage;




public class ReliableForwarder {
	public enum ForwardType {toLeader, broadcast};
	
	private MnTreePeerAgent mnPeer;
	private CommunicatorInterface com;
	
	// Caches
	private int firstStageSeqNum;
	private FirstStageCache fsCache;
	private SecondStageCache ssCache;
	private ThirdStageCache tsCache;
	
	// Ack waiting
	private FirstStageWaitingAcks fsWaitingAck;
	private SecondStageWaitingAcks ssWaitingAck;
	private ThirdStageWaitingAcks tsWaitingAck;
	
	private TreeMap<MessageId, Message> waitSent;
	
	public ReliableForwarder(MnTreePeerAgent mnPeer) {
		this.mnPeer = mnPeer;
		com = mnPeer.getCommunicator();
		
		fsCache = new FirstStageCache();
		ssCache = new SecondStageCache();
		tsCache = new ThirdStageCache();
		
		fsWaitingAck = new FirstStageWaitingAcks();
		ssWaitingAck = new SecondStageWaitingAcks();
		tsWaitingAck = new ThirdStageWaitingAcks();
		
		waitSent = new TreeMap<MessageId, Message>();
	}
	
	private MessageId getNextMessageId() {
		DAId thisDaId = mnPeer.getDistributedAgent().getDaId();
		MessageId msgId = new MessageId(thisDaId, firstStageSeqNum);
		++firstStageSeqNum;
		return msgId;
	}
	
	public void broadcastMessage(Message m) {
		printMessage("Broadcasting message: "+m.getClass().getName());
		MnPeerState peerState = mnPeer.getPeerState();
		ForwardType type = ForwardType.broadcast;
		if(mnPeer.isMainPeer()) {
			SecondStageWaitingAcks.Destinations dest = new SecondStageWaitingAcks.Destinations(peerState.getMaxNumOfChildren());
//			SecondStageMessageId msgId = getNextSecondStageMessageId();
			MessageId msgId = getNextMessageId();
			SourceMn src = new SourceMn();
			src.setThis();
			SecondStageMessageInfo msgInf = new SecondStageMessageInfo(msgId, type, src, m);

			sendSecondStageMessageToParent(dest, msgInf);
			sendSecondStageMessageToChildren(dest, -1, msgInf);
			
			ThirdStageWaitingAcks.Destinations tDest = new ThirdStageWaitingAcks.Destinations();
			sendThirdStageMessages(tDest, msgInf);
			
			ssCache.cache(msgInf);
			ssWaitingAck.waitForAcks(dest, msgId);
			tsWaitingAck.waitForAcks(tDest, msgId);
			waitSent.put(msgId, m);
			
			printMessage("2nd cache size: "+ssCache.size());
		} else {
			// Send 1st stage message to main peer
			MessageId msgId = getNextMessageId();
			printMessage("Sending a first stage message: "+msgId);
			
			DAId mainPeerId = peerState.getThisMnMainPeer();
			MnId thisId = peerState.getThisMnId();
			FirstStageMessage fsm = new FirstStageMessage(mainPeerId,
					thisId, thisId, msgId, type, m);
			
			fsCache.cache(fsm);
			waitSent.put(msgId, m);
			com.sendDatagramMessage(fsm);
			
			printMessage("1st cache size: "+fsCache.size());
		}
	}

	private void sendSecondStageMessageToParent(
			SecondStageWaitingAcks.Destinations dest,
			SecondStageMessageInfo msgInf) {
		MnPeerState peerState = mnPeer.getPeerState();
		DAId parentDaId = peerState.getParentMnMainPeer();
		if(parentDaId != null) {
			// Send 2nd stage message to parent
			SourceMn src = new SourceMn();
			src.setChild(peerState.getThisChildIndex());
			printMessage("Sending a second stage message to parent ("+parentDaId+"): "+msgInf.getMessageId());

			if(dest != null)
				dest.setWaitAckFromParent();
			
			MnId thisId = peerState.getThisMnId();
			MnId parentId = peerState.getParentMnId();
			SecondStageMessage ssm = new SecondStageMessage(parentDaId,
					thisId, parentId, msgInf, src);

			com.sendDatagramMessage(ssm);
		}
	}
	
	private void sendSecondStageMessageToChildren(
			SecondStageWaitingAcks.Destinations dest,
			int excludedChild,
			SecondStageMessageInfo msgInf) {
		MnPeerState peerState = mnPeer.getPeerState();
		for(int c = 0; c < peerState.getMaxNumOfChildren(); ++c) {
			if(c != excludedChild)
				sendSecondStageMessageToChild(peerState, c, dest, msgInf);
		}
	}
	
	private void sendSecondStageMessageToChild(
			MnPeerState peerState,
			int childIndex,
			SecondStageWaitingAcks.Destinations dest,
			SecondStageMessageInfo msgInf) {
		DAId childDaId = peerState.getChildMnMainPeer(childIndex);
		SourceMn src = new SourceMn();
		src.setParent();
		if(childDaId != null) {
			// Send 2nd stage message to parent
			printMessage("Sending a second stage message to child "+childIndex+" ("+childDaId+"): "+msgInf.getMessageId());

			if(dest != null)
				dest.setWaitAckFromChild(childIndex);
			
			MnId thisId = peerState.getThisMnId();
			MnId childId = peerState.getChildMnId(childIndex);
			SecondStageMessage ssm = new SecondStageMessage(childDaId,
					thisId, childId, msgInf, src);

			com.sendDatagramMessage(ssm);
		}
	}
	
	public void sendMessageToLeader(Message m) {
		printMessage("Sending message to leader: "+m.getClass().getName());
		MnPeerState peerState = mnPeer.getPeerState();
		if(mnPeer.isMainPeer()) {
			DAId parentDaId = peerState.getParentMnMainPeer();
			if(parentDaId != null) {
				// Send 2nd stage message to parent
//				SecondStageMessageId msgId = getNextSecondStageMessageId();
				MessageId msgId = getNextMessageId();
	
				SecondStageWaitingAcks.Destinations dest = new SecondStageWaitingAcks.Destinations(peerState.getMaxNumOfChildren());
				SourceMn src = new SourceMn();
				src.setThis();
				SecondStageMessageInfo msgInf = new SecondStageMessageInfo(msgId, ForwardType.toLeader, src, m);
				
				sendSecondStageMessageToParent(dest, msgInf);
				ssCache.cache(msgInf);
				ssWaitingAck.waitForAcks(dest, msgId);
				waitSent.put(msgId, m);
				
				printMessage("2nd cache size: "+ssCache.size());
			} else {
				printMessage("Message reached destination.");
				
				// Message reached destination
				m.setSender(mnPeer.getDistributedAgent().getDaId());
				m.setRecipient(mnPeer.getDistributedAgent().getDaId());
				m.setMessageSent(true);
				if(m instanceof MnTreeMessage) {
					MnTreeMessage mtm = (MnTreeMessage) m;
					mtm.setSenderMn(peerState.getThisMnId());
					mtm.setRecipientMn(peerState.getThisMnId());
				}
				mnPeer.getCommunicator().submitIncomingMessage(m);
			}
		} else {
			// Send 1st stage message to main peer
			MessageId msgId = getNextMessageId();
			printMessage("Sending a first stage message: "+msgId);
			
			DAId mainPeerId = peerState.getThisMnMainPeer();
			MnId thisId = peerState.getThisMnId();
			FirstStageMessage fsm = new FirstStageMessage(mainPeerId, thisId,
				thisId, msgId, ForwardType.toLeader, m);
			
			fsCache.cache(fsm);
			waitSent.put(msgId, m);
			com.sendDatagramMessage(fsm);
			
			printMessage("1st cache size: "+fsCache.size());
		}
	}
	
	private void handleFirstStageMessage(FirstStageMessage fsm) {
		MnPeerState peerState = mnPeer.getPeerState();
		if(! mnPeer.isMainPeer()) {
			throw new Error("First stage message must be forwarded to main peer");
		}

		if(ssCache.contains(fsm.getMessageId())) {
			// Can happen if a 1st stage message is re-sent
			// to new main peer but was already received as 3rd stage message
			printMessage("First stage message already received.");
			return;
		}

		MessageId msgId = fsm.getMessageId();
		ForwardType type = fsm.getForwardType();
		Message m = fsm.getMessage();
		
		printMessage("Received 1st stage message ("+type+") from "+fsm.getSender());

		DAId senderId = fsm.getSender();
		FirstStageAckMessage ack = new FirstStageAckMessage(senderId, msgId);

		SecondStageWaitingAcks.Destinations dest = new SecondStageWaitingAcks.Destinations(peerState.getMaxNumOfChildren());
		SourceMn src = new SourceMn();
		src.setThis();
		SecondStageMessageInfo msgInf = new SecondStageMessageInfo(msgId,
				type, src, m);
		
		if(type.equals(ForwardType.toLeader)) {
			if(peerState.hasParent()) {
				sendSecondStageMessageToParent(dest, msgInf);
			} else {
				// message reached destination
				m.setSender(mnPeer.getDistributedAgent().getDaId());
				m.setRecipient(mnPeer.getDistributedAgent().getDaId());
				if(m instanceof MnTreeMessage) {
					MnTreeMessage mtm = (MnTreeMessage) m;
					mtm.setSenderMn(peerState.getThisMnId());
					mtm.setRecipientMn(peerState.getThisMnId());
				}
				mnPeer.getCommunicator().submitIncomingMessage(m);
				
				// Send first stage ack
				com.sendDatagramMessage(ack);
				return;
			}
		} else if(type.equals(ForwardType.broadcast)) {
			sendSecondStageMessageToParent(dest, msgInf);
			sendSecondStageMessageToChildren(dest, -1, msgInf);
		} else {
			throw new Error("Unhandled forward type");
		}
		
		ThirdStageWaitingAcks.Destinations tDest = new ThirdStageWaitingAcks.Destinations();
		sendThirdStageMessages(tDest, msgInf);
		
		ssCache.cache(msgInf);
		ssWaitingAck.waitForAcks(dest, msgId);
		fsWaitingAck.waitAck(msgId, ack);
		tsWaitingAck.waitForAcks(tDest, msgId);
		
		printMessage("2nd cache size: "+ssCache.size());
		
		if(type.equals(ForwardType.broadcast)) {
			// Message reached destination
			m.setSender(mnPeer.getDistributedAgent().getDaId());
			m.setRecipient(mnPeer.getDistributedAgent().getDaId());
			if(m instanceof MnTreeMessage) {
				MnTreeMessage mtm = (MnTreeMessage) m;
				mtm.setSenderMn(peerState.getThisMnId());
				mtm.setRecipientMn(peerState.getThisMnId());
			}
			mnPeer.getCommunicator().submitIncomingMessage(m);
		} // else type == toLeader : already handled
	}

	private void sendThirdStageMessages(
			ThirdStageWaitingAcks.Destinations dest,
			SecondStageMessageInfo msgInf) {
		MnPeerState mnPeerState = mnPeer.getPeerState();
		TreeSet<DAId> ids = new TreeSet<DAId>();
		mnPeerState.getThisMnPeersIds(ids);
		DAId thisDaId = mnPeer.getDistributedAgent().getDaId();
		ids.remove(thisDaId);
		
		MnId thisId = mnPeerState.getThisMnId();
		for(DAId id : ids) {
			dest.waitFrom(id);
			com.sendDatagramMessage(new ThirdStageMessage(id,
					thisId, thisId, msgInf));
		}
	}

	private void handleSecondStageMessage(SecondStageMessage ssm) throws CacheException {
		if(! mnPeer.isMainPeer()) {
			throw new Error("Second stage message must be forwarded to main peer");
		}
		
		if(ssCache.contains(ssm.getInfo().getMessageId())) {
			// Can happen if a 2nd stage message is re-sent
			// to new main peer but was already received as 3rd stage message
			printMessage("Second stage message already received.");
			return;
		}

		SecondStageMessageInfo msgInf = ssm.getInfo();
		forward2ndStageMessage(msgInf, ssm.getSource());
	}

	/**
	 * @param ssm
	 * @param peerState
	 * @throws Error
	 */
	private void forward2ndStageMessage(SecondStageMessageInfo msgInf,
			SourceMn src) {
		MnPeerState peerState = mnPeer.getPeerState();
		
		ForwardType type = msgInf.getForwardType();
		MessageId msgId = msgInf.getMessageId();
		
		if(src == null)
			throw new Error("No source given for 2nd stage message");
		
		printMessage("Forwarding 2nd stage message "+msgId+" coming from "+src+" ("+type+")");
		SecondStageWaitingAcks.Destinations dest = new SecondStageWaitingAcks.Destinations(peerState.getMaxNumOfChildren());
		ThirdStageWaitingAcks.Destinations tDest = new ThirdStageWaitingAcks.Destinations();
		SecondStageMessageInfo newMsgInf = new SecondStageMessageInfo(
				msgId, type, src, msgInf.getMessage());
		if(type.equals(ForwardType.toLeader)) {
			if(peerState.getParentMnId() != null) {
				// Continue forwarding
				sendSecondStageMessageToParent(dest, newMsgInf);
				sendThirdStageMessages(tDest, newMsgInf);
				
				ssCache.cache(newMsgInf);
				ssWaitingAck.waitForAcks(dest, msgId);
				tsWaitingAck.waitForAcks(tDest, msgId);
				
				printMessage("2nd cache size: "+ssCache.size());
			} else {
				printMessage("Message reached destination");
				Message m = msgInf.getMessage();
				m.setSender(getMainDaId(src));
				m.setRecipient(com.getHostingDaId());
				if(m instanceof MnTreeMessage) {
					MnTreeMessage mtm = (MnTreeMessage) m;
					mtm.setSenderMn(peerState.getThisMnId());
					mtm.setRecipientMn(peerState.getThisMnId());
				}
				mnPeer.getCommunicator().submitIncomingMessage(m);
				
				ssCache.cache(newMsgInf);
				ackSecondStageMessage(msgId);
				
				printMessage("2nd cache size: "+ssCache.size());
			}
		} else if(type.equals(ForwardType.broadcast)) {
			// Continue forwarding
			Message m = msgInf.getMessage();
			
			if(! src.isParent())
				sendSecondStageMessageToParent(dest, newMsgInf);
			if(src.isChild()) {
				sendSecondStageMessageToChildren(dest, src.getChildIndex(), newMsgInf);
			} else {
				sendSecondStageMessageToChildren(dest, -1, newMsgInf);
			}
			sendThirdStageMessages(tDest, newMsgInf);
			
			ssCache.cache(newMsgInf);
			if(! dest.waitsForAcks() && ! tDest.waitsForAcks()) {
				printMessage("Cannot be forwarded further.");
				ackSecondStageMessage(msgId);
			} else {
				ssWaitingAck.waitForAcks(dest, msgId);
				tsWaitingAck.waitForAcks(tDest, msgId);
			}
			
			printMessage("2nd cache size: "+ssCache.size());

			// Message reached destination
			m.setSender(getMainDaId(src));
			m.setRecipient(com.getHostingDaId());
			if(m instanceof MnTreeMessage) {
				MnTreeMessage mtm = (MnTreeMessage) m;
				mtm.setSenderMn(peerState.getThisMnId());
				mtm.setRecipientMn(peerState.getThisMnId());
			}
			mnPeer.getCommunicator().submitIncomingMessage(m);
		}
	}
	
	private DAId getMainDaId(SourceMn src) {
		MnPeerState state = mnPeer.getPeerState();
		if(src.isParent())
			return state.getParentMnMainPeer();
		else if(src.isThis())
			return state.getThisMnMainPeer();
		else
			return state.getChildMnMainPeer(src.getChildIndex());
	}

	private void handleSecondStageAckMessage(SecondStageAckMessage ack) throws CacheException {
		if(! mnPeer.isMainPeer()) {
			throw new Error("Not the main peer of this MN");
		}
		
		MessageId msgId = ack.getAckedMessageId();
		printMessage("Received an ack for second stage message "+msgId);
		
		SourceMn ackSrc = ack.getSource();
		ForwardType forwType = ack.getAckedMessageType();

		onSecondStageAck(msgId, ackSrc, forwType);
	}

	private void onSecondStageAck(MessageId msgId, SourceMn ackSrc,
			ForwardType forwType) throws CacheException, Error {
		boolean acked = false;
		if(ackSrc.isChild()) {
			acked = ssWaitingAck.ackFromChild(ackSrc.getChildIndex(), msgId);
		} else if(ackSrc.isParent()) {
			acked = ssWaitingAck.ackFromParent(msgId);
		} else {
			throw new Error("Unhandled source");
		}
		
		if(! acked) {
			printMessage("Still waiting acks");
			return;
		}
		
		// If ack for a toLeader message, all caches can be cleared
		// and no more acks (even 3rd) are awaited.
		// If ack for a broadcast message, ack can be "forwarded" only
		// if all 3rd acks have been received.
		boolean isToLeader = forwType.equals(ForwardType.toLeader);
		boolean wait3rdAck = tsWaitingAck.isWaiting(msgId);
		
		if(isToLeader || (! isToLeader && ! wait3rdAck)) {
			signalSent(msgId);
			ackFirstStageMessage(msgId);
			ackSecondStageMessage(msgId);
			if(isToLeader) {
				clearThirdStageCaches(msgId);
				tsWaitingAck.remove(msgId);
			}
		}
	}

	private void ackFirstStageMessage(MessageId msgId) {
		FirstStageAckMessage fsAck = fsWaitingAck.ack(msgId);
		if(fsAck != null) {
			printMessage("Ack 1st stage message "+fsAck.getAckedMessageId());
			com.sendDatagramMessage(fsAck);
		} else {
			printMessage("No 1st stage message to ack: "+msgId);
		}
	}

	private void clearThirdStageCaches(MessageId msgId) {
		printMessage("Clearing 3rd stage caches: "+msgId);
		
		DAId[] peers = new DAId[mnPeer.getPeerState().getThisMnSize()];
		mnPeer.getPeerState().getThisMnPeersIds(peers);
		
		MnId thisId = mnPeer.getPeerState().getThisMnId();
		DAId thisDaId = mnPeer.getDistributedAgent().getDaId();
		for(DAId id : peers) {
			if(id.equals(thisDaId))
				continue;
			com.sendDatagramMessage(new RemoveFromThirdStageCacheMessage(
					id, thisId, thisId,
					msgId));
		}
	}

	private void printMessage(String string) {
		mnPeer.agentPrintMessage("[ReliableForwarder] "+string);
	}

	public void handleReliableForwarderMessage(Object o) throws Exception {
		if(o instanceof FirstStageMessage) {
			handleFirstStageMessage((FirstStageMessage) o);
		} else if(o instanceof SecondStageMessage) {
			handleSecondStageMessage((SecondStageMessage) o);
		} else if(o instanceof FirstStageAckMessage) {
			handleFirstStageAckMessage((FirstStageAckMessage) o);
		} else if(o instanceof SecondStageAckMessage) {
			handleSecondStageAckMessage((SecondStageAckMessage) o);
		} else if(o instanceof ThirdStageMessage) {
			handleThirdStageMessage((ThirdStageMessage) o);
		} else if(o instanceof ThirdStageAckMessage) {
			handleThirdStageAckMessage((ThirdStageAckMessage) o);
		} else if(o instanceof RemoveFromThirdStageCacheMessage) {
			handleRemoveFromThirdStageCacheMessage((RemoveFromThirdStageCacheMessage) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleRemoveFromThirdStageCacheMessage(
			RemoveFromThirdStageCacheMessage o) {
		MessageId msgId = o.getMessageId();
		tsCache.remove(msgId);
		printMessage("3rd cache size: "+tsCache.size());
	}

	private void handleThirdStageAckMessage(ThirdStageAckMessage o) {
		MessageId msgId = o.getMessageId();
		ForwardType type = o.getType();
		DAId senderId = o.getSender();

		onThirdStageAck(msgId, type, senderId);
	}

	private void onThirdStageAck(MessageId msgId, ForwardType type,
			DAId senderId) {
		if(tsWaitingAck.ack(msgId, senderId)) {
			printMessage("All 3rd stage acks received for "+msgId);
			
			// If 2nd stage ack was already received, 3rd caches
			// need to be cleared. If the message was broadcasted,
			// it also needs to be removed from cache.
			// Otherwise, 2nd stage ack can be sent but message still
			// needs to be kept in cache until 2nd stage ack is received.
			
			boolean toLeader = type.equals(ForwardType.toLeader);
			if(! toLeader) {
				signalSent(msgId);
				ackFirstStageMessage(msgId);
				ackSecondStageMessage(msgId);
			}
			
			if(! ssWaitingAck.isWaiting(msgId)) {
				printMessage("All 2nd stage acks already received.");
				clearThirdStageCaches(msgId);
			} else {
				printMessage("Still waiting 2nd stage acks.");
			}
		}
	}

	private void ackSecondStageMessage(MessageId msgId) {
		SecondStageMessageInfo msgInf = ssCache.remove(msgId);
		printMessage("2nd cache size: "+ssCache.size());

		if(msgInf != null) {
			SourceMn src = msgInf.getSource();
			if(! src.isThis()) {
				printMessage("Sending 2nd stage ack for message "+msgId);
	
				DAId destId;
				MnId destMnId;
				MnPeerState peerState = mnPeer.getPeerState();
				SourceMn sentAckSrc = new SourceMn();
				if(src.isParent()) {
					destId = peerState.getParentMnMainPeer();
					destMnId = peerState.getParentMnId();
					sentAckSrc.setChild(peerState.getThisChildIndex());
				} else if(src.isChild()) {
					int childIndex = src.getChildIndex();
					destId = peerState.getChildMnMainPeer(childIndex);
					destMnId = peerState.getChildMnId(childIndex);
					sentAckSrc.setParent();
				} else {
					throw new Error("Unhandled source");
				}
	
				MnId thisMnId = peerState.getThisMnId();
				SecondStageAckMessage newAck = new SecondStageAckMessage(destId,
					thisMnId, destMnId, msgId, msgInf.getForwardType(), sentAckSrc);
				com.sendDatagramMessage(newAck);
			} else {
				printMessage("No ack to send for 2nd stage message "+msgId);
			}
		} else {
			printMessage("2nd stage message already acked: "+msgId);
		}
	}

	private void handleThirdStageMessage(ThirdStageMessage o) {
		boolean notAlreadyReceived = tsCache.cache(o);
		printMessage("3rd cache size: "+tsCache.size());
		
		// Submit if message reached destination
		if(notAlreadyReceived) {
			ForwardType type = o.getMessageInfo().getForwardType();
			if(type.equals(ForwardType.toLeader)) {
				// SKIP
			} else if(type.equals(ForwardType.broadcast)) {
				Message m = o.getMessageInfo().getMessage();
				m.setSender(o.getSender());
				m.setRecipient(o.getRecipient());
				if(m instanceof MnTreeMessage) {
					MnTreeMessage mtm = (MnTreeMessage) m;
					mtm.setSenderMn(mnPeer.getPeerState().getThisMnId());
					mtm.setRecipientMn(mnPeer.getPeerState().getThisMnId());
				}
				mnPeer.getCommunicator().submitIncomingMessage(m);
			} else {
				throw new Error("Unknown forward type");
			}
		}
		
		// Send ack to main peer
		ThirdStageAckMessage ack = new ThirdStageAckMessage(
			o.getSender(), o.getRecipientMn(), o.getSenderMn(),
			o.getMessageInfo().getMessageId(), o.getMessageInfo().getForwardType());
		com.sendDatagramMessage(ack);
	}

	private void handleFirstStageAckMessage(FirstStageAckMessage o) {
		MessageId msgId = o.getAckedMessageId();
		printMessage("Received an ack for first stage message "+msgId);
		fsCache.remove(msgId);
		signalSent(msgId);
		
		printMessage("1st cache size: "+fsCache.size());
	}

	private void signalSent(MessageId msgId) {
		Message m = waitSent.remove(msgId);
		if(m != null) {
			printMessage("Signaling "+m.getClass().getName()+" has been sucessfully sent");
			m.setMessageSent(true);
		}
	}

	public void handleWrongRoute(WrongRouteMessage o) {
		MnTreeMessage m = o.getMessage();
		
		printMessage("Wrongly routed message: "+m.getClass().getName());
		if(m instanceof FirstStageMessage) {
			FirstStageMessage fsm = (FirstStageMessage) m;
			fsm.setRecipient(mnPeer.getPeerState().getThisMnMainPeer());
			com.sendDatagramMessage(fsm);
		} else {
			printMessage("TODO : unimplemented");
		}
	}

	public void convertToMainPeer() {
		// Handle cached 1st stage messages
		for(FirstStageMessage m : fsCache) {
			handleFirstStageMessage(m);
		}

		// Re-send 3rd stage cache messages
		for(ThirdStageMessage m : tsCache) {
			forward2ndStageMessage(m.getMessageInfo(), m.getMessageInfo().getSource());
		}
	}

	public void signalThisMnNewMainPeer() {
		MnPeerState state = mnPeer.getPeerState();
		DAId mainPeer = state.getThisMnMainPeer();
		
		// Re-send 1st stage messages
		for(FirstStageMessage m : fsCache) {
			m.setRecipient(mainPeer);
			com.sendDatagramMessage(m);
		}
	}

	public void signalBrokenBrother(DAId daId) {
		// Discard 1st stage acks for broken DA
		fsWaitingAck.removeReferences(daId);
		
		// Consider 3rd stage acks from DA as received
		LinkedList<MessageId> thirdAckMessages = new LinkedList<MessageId>();
		tsWaitingAck.listWaitingAcks(daId, thirdAckMessages);
		for(MessageId msgId : thirdAckMessages) {
			SecondStageMessageInfo inf = ssCache.get(msgId);
			if(inf != null)
				onThirdStageAck(msgId, inf.getForwardType(), daId);
		}
	}

	public void signalParentMnNewMainPeer() {
		// Extract waiting 2nd stage ack messages from parent
		LinkedList<MessageId> infos = new LinkedList<MessageId>();
		ssWaitingAck.listWaitingForParent(infos);
		
		// Re-send extracted 2nd stage messages to new main
		for(MessageId id : infos) {
			SecondStageMessageInfo info = ssCache.get(id);
			printMessage("Message "+info.getMessageId()+
					" is forwarded again to parent.");
			sendSecondStageMessageToParent(null, info);
		}
	}

	public void signalChildMnNewMainPeer(int childIndex) {
		// Extract waiting 2nd stage ack messages from ith child
		LinkedList<MessageId> infos = new LinkedList<MessageId>();
		ssWaitingAck.listWaitingForChild(childIndex, infos);

		// Re-send extracted 2nd stage messages to new main
		MnPeerState state = mnPeer.getPeerState();
		for(MessageId id : infos) {
			SecondStageMessageInfo info = ssCache.get(id);
			if(info != null) {
				printMessage("Message "+info.getMessageId()+
					" is forwarded again to child "+childIndex);
				sendSecondStageMessageToChild(state, childIndex, null, info);
			} else {
				printMessage(id+" in ssWaitingAck but not in cache ???");
			}
		}
	}
}
