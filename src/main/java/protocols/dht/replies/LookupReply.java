package protocols.dht.replies;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashProducer;

public class LookupReply extends ProtoReply {

	public final static short REPLY_ID = 502;
	
	private final byte[] peerID;
	private final List<Pair<byte[],Host>> peers;
	private final UUID mid;
	
	public LookupReply(byte[] peerID, UUID mid) {
		super(REPLY_ID);
		this.peerID = peerID.clone();
        this.mid = mid;
        this.peers = new ArrayList<>();
	}

	public List<Pair<byte[], Host>> getPeers() {
		return peers;
	}

	public byte[] getPeerID() {
		return this.peerID.clone();
	}
	
	public BigInteger getPeerIDNumerical() {
		return HashProducer.toNumberFormat(peerID);
	}
	
	public String getPeerIDHex() {
		return HashProducer.toNumberFormat(peerID).toString(16);
	}
	
	public Iterator<Pair<byte[], Host>> getPeersIterator() {
		return this.peers.iterator();
	}
	
	public void addElementToPeers(byte[] peerID, Host h) {
		this.peers.add( Pair.of(peerID,h) );
	}

	public UUID getMid() {
		return mid;
	}

	public String toString() {
		String reply =  "LookupReply for " + this.getPeerIDHex() + " containing set (" + this.peers.size() + " elements):\n";
		for(Pair<byte[], Host> p: this.peers) {
			reply += "\t" + HashProducer.toNumberFormat(p.getLeft()).toString(16) + "::" + p.getRight().toString() + "\n";
		}
		return reply;
	}
	
}
