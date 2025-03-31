package protocols.dht;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.apps.AutomatedApp;
import protocols.dht.replies.LookupReply;
import protocols.dht.requests.Lookup;
import protocols.dht.utils.KademliaRoutingTable;
import protocols.point2point.notifications.ChannelCreated;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class KademliaDHT extends GenericProtocol {

	private static final Logger logger = LogManager.getLogger(KademliaDHT.class);

	public final static short PROTOCOL_ID = 500;
	public final static String PROTOCOL_NAME = "kademliaDHT";

	private String myPeerIDHex;
	private byte[] myPeerID;
	private KademliaRoutingTable routingTable;

	private boolean channelReady;

	public KademliaDHT() throws HandlerRegistrationException {
		super(PROTOCOL_NAME, PROTOCOL_ID);
		channelReady = false;

		registerRequestHandler(Lookup.REQUEST_ID, this::uponLookup);
		subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);
	}

	@Override
	public void init(Properties props) {

		this.myPeerIDHex = props.getProperty(AutomatedApp.PROPERTY_NODE_ID);
		this.myPeerID = new BigInteger(myPeerIDHex, 16).toByteArray();
		this.routingTable = new KademliaRoutingTable(myPeerID);

		logger.info("Kademlia DHT Initialized for node " + myPeerIDHex);
	}

	private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
		int cId = notification.getChannelId();

		registerSharedChannel(cId);

		channelReady = true;
	}

	private void uponLookup(Lookup request, short protoID) {
		if (!channelReady) return;

		logger.info("Received LookupRequest: " + request.toString());

		Pair<byte[], Host> peer = routingTable.findPeer(request.getPeerID());
		if (peer != null) updateRoutingTable(peer.getLeft(), peer.getRight());

		LookupReply lr = new LookupReply(request.getPeerID(), request.getMid());

		List<Pair<byte[], Host>> closestPeers = findClosestPeers(request.getPeerID());

		for (Pair<byte[], Host> p : closestPeers) {
			lr.addElementToPeers(p.getLeft(), p.getRight());
		}

		sendReply(lr, protoID);
	}

	public List<Pair<byte[], Host>> findClosestPeers(byte[] peerID) {
		return routingTable.findClosestPeers(peerID);
	}


	public void updateRoutingTable(byte[] peerID, Host peerHost) {
		routingTable.update(peerID, peerHost);
	}

	public byte[] getPeerID() {
		return myPeerID;
	}
}