package protocols.point2point.notifications;

import java.math.BigInteger;
import java.util.UUID;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashProducer;

public class Deliver extends ProtoNotification {
	
	public final static short NOTIFICATION_ID = 403;
	
	private final Host sender;
	private final UUID messageID;
	private final byte[] messagePayload;
	
	public Deliver(Host sender, UUID mid, byte[] mPayload) {
		super(NOTIFICATION_ID);
        this.sender = sender;
		this.messageID = mid;
		this.messagePayload = mPayload.clone();
	}

	public Host getSender() {
		return sender;
	}

	public byte[] getMessagePayload() {
		return this.messagePayload.clone();
	}
	
	public UUID getMessageID() {
		return this.messageID;
	}
	
	public String toString() {
		return "DeliverNotification from " + this.getSender() + " with message ID " + this.messageID + " payload of " + this.messagePayload.length +  " bytes";
	}
	
	public String toStringLong() {
		String representation = this.toString();
		representation += "\nPayload:\n" + new String(this.messagePayload, 0, this.messagePayload.length);
		return representation;
	}
}
