package protocols.point2point.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PingMessage extends ProtoMessage {

    public static final short MSG_ID = 407; // Arbitrary unique ID for the ping message

    private final byte[] senderID;

    public PingMessage(byte[] senderID) {
        super(MSG_ID);
        this.senderID = senderID;
    }

    public byte[] getSenderID() {
        return senderID;
    }

    public static final ISerializer<PingMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(PingMessage msg, ByteBuf out) {
            out.writeInt(msg.senderID.length);
            if (msg.senderID.length > 0) {
                out.writeBytes(msg.senderID);
            }
        }

        @Override
        public PingMessage deserialize(ByteBuf in) {
            int size = in.readInt();
            byte[] id = new byte[size];
            if (size > 0)
                in.readBytes(id);
            return new PingMessage(id);
        }
    };
}

