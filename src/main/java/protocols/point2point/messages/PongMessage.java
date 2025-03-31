package protocols.point2point.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PongMessage extends ProtoMessage {

    public static final short MSG_ID = 406; // Arbitrary unique ID for the pong message

    private final byte[] senderID;

    public PongMessage(byte[] senderID) {
        super(MSG_ID);
        this.senderID = senderID;
    }

    public byte[] getSenderID() {
        return senderID;
    }

    public static final ISerializer<PongMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(PongMessage msg, ByteBuf out) {
            out.writeInt(msg.senderID.length);
            if (msg.senderID.length > 0) {
                out.writeBytes(msg.senderID);
            }
        }

        @Override
        public PongMessage deserialize(ByteBuf in) {
            int size = in.readInt();
            byte[] id = new byte[size];
            if (size > 0)
                in.readBytes(id);
            return new PongMessage(id);
        }
    };
}

