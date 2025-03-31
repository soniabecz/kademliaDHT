package protocols.dht.requests;

import java.math.BigInteger;
import java.util.UUID;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashProducer;

public class Lookup extends ProtoRequest {

	public final static short REQUEST_ID = 501;
	public static ISerializer<? extends ProtoMessage> serializer;

	private final byte[] peerID;
	private final UUID mid;
	
	public Lookup(byte[] peerID, UUID mid) {
		super(REQUEST_ID);
		this.peerID = peerID.clone();
        this.mid = mid;
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

	public UUID getMid() {
		return mid;
	}

	public String toString() {
		return "Lookup Request for: " + this.getPeerIDHex();
	}
	
}
