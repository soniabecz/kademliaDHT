package protocols.point2point.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.UUID;

public class Message extends ProtoMessage {

    public final static short MSG_ID = 405;

    private final UUID mid;
    private final Host sender;

    private final short toDeliver;
    private final byte[] content;

    @Override
    public String toString() {
        return "Message{" +
                "mid=" + mid +
                '}';
    }

    public Message(UUID mid, Host sender, short toDeliver, byte[] content) {
        super(MSG_ID);
        this.mid = mid;
        this.sender = sender;
        this.toDeliver = toDeliver;
        this.content = content;
    }

    public Host getSender() {
        return sender;
    }

    public UUID getMid() {
        return mid;
    }

    public short getToDeliver() {
        return toDeliver;
    }

    public byte[] getContent() {
        return content;
    }

    public static ISerializer<Message> serializer = new ISerializer<>() {
        @Override
        public void serialize(Message message, ByteBuf out) throws IOException {
            out.writeLong(message.mid.getMostSignificantBits());
            out.writeLong(message.mid.getLeastSignificantBits());
            Host.serializer.serialize(message.sender, out);
            out.writeShort(message.toDeliver);
            out.writeInt(message.content.length);
            if (message.content.length > 0) {
                out.writeBytes(message.content);
            }
        }

        @Override
        public Message deserialize(ByteBuf in) throws IOException {
            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID mid = new UUID(firstLong, secondLong);
            Host sender = Host.serializer.deserialize(in);
            short toDeliver = in.readShort();
            int size = in.readInt();
            byte[] content = new byte[size];
            if (size > 0)
                in.readBytes(content);

            return new Message(mid, sender, toDeliver, content);
        }
    };
}


