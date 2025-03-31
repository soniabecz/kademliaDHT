package protocols.point2point.requests;

import java.math.BigInteger;
import java.util.UUID;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.ISerializer;
import utils.HashProducer;

public class Send extends ProtoRequest {

	public final static short REQUEST_ID = 401;
	public static ISerializer<? extends ProtoMessage> serializer;

	private final byte[] senderID;
	private final byte[] destinationID;
	private final UUID messageID;
	private final byte[] messagePayload;
	
	public Send(byte[] senderID, byte[] destID, UUID mid, byte[] mPayload) {
		super(REQUEST_ID);
		this.senderID = senderID.clone();
		this.destinationID = destID.clone();
		this.messageID = mid;
		this.messagePayload = mPayload.clone();
	}
	
	public byte[] getSenderPeerID() {
		return this.senderID.clone();
	}
	
	public byte[] getDestinationPeerID() {
		return this.destinationID.clone();
	}
	
	public BigInteger getSenderPeerIDNumerical() {
		return HashProducer.toNumberFormat(senderID);
	}
	
	public String getSenderPeerIDHex() {
		return HashProducer.toNumberFormat(senderID).toString(16);
	}
	
	public BigInteger getDestinationPeerIDNumerical() {
		return HashProducer.toNumberFormat(destinationID);
	}
	
	public String getDestinationPeerIDHex() {
		return HashProducer.toNumberFormat(destinationID).toString(16);
	}
	
	public byte[] getMessagePayload() {
		return this.messagePayload.clone();
	}
	
	public UUID getMessageID() {
		return this.messageID;
	}
	
	public String toString() {
		return "SendRequest from " + this.getSenderPeerIDHex() + " to " + this.getDestinationPeerIDHex() + " with message ID " + this.messageID + " payload of " + this.messagePayload.length +  " bytes";
	}
	
	public String toStringLong() {
		String representation = this.toString();
		representation += "\nPayload:\n" + new String(this.messagePayload, 0, this.messagePayload.length);
		return representation;
	}
}
