package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.tuple.Pair;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FindNodeMessage extends ProtoMessage {
    public static final short MSG_ID = 503; // Arbitrary unique ID for the ping message

    private final List<Pair<byte[], Host>> peers;

    public FindNodeMessage(List<Pair<byte[], Host>> peers) {
        super(MSG_ID);
        this.peers = peers;
    }

    public List<Pair<byte[], Host>> getPeers() {
        return peers;
    }

    public static ISerializer<FindNodeMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(FindNodeMessage message, ByteBuf out) throws IOException {
            out.writeInt(message.peers.size());
            for (Pair<byte[], Host> peer : message.peers) {
                byte[] key = peer.getLeft();
                out.writeInt(key.length);
                if (key.length > 0) {
                    out.writeBytes(key);
                }
                Host.serializer.serialize(peer.getRight(), out);
            }
        }

        @Override
        public FindNodeMessage deserialize(ByteBuf in) throws IOException {
            int listSize = in.readInt();
            List<Pair<byte[], Host>> peers = new ArrayList<>(listSize);

            for (int i = 0; i < listSize; i++) {
                int keyLength = in.readInt();
                byte[] key = new byte[keyLength];
                if (keyLength > 0) {
                    in.readBytes(key);
                }
                Host host = Host.serializer.deserialize(in);
                peers.add(Pair.of(key, host));
            }

            return new FindNodeMessage(peers);
        }
    };
}
