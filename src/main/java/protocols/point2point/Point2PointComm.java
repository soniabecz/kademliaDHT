package protocols.point2point;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import protocols.dht.KademliaDHT;
import protocols.dht.messages.FindNodeMessage;
import protocols.dht.replies.LookupReply;
import protocols.dht.requests.Lookup;
import protocols.point2point.messages.PingMessage;
import protocols.point2point.messages.PongMessage;
import protocols.point2point.messages.Message;
import protocols.point2point.notifications.ChannelCreated;
import protocols.point2point.notifications.Deliver;
import protocols.point2point.requests.Send;
import protocols.point2point.timers.Timer;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashProducer;

public class Point2PointComm extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(Point2PointComm.class);

    public final static String PROTOCOL_NAME = "Point2PointComm";
    public final static short PROTOCOL_ID = 400;

    private final Host myself;
    private final short DHT_PROTO_ID;
    private final int channelId;
    private final Object lock;
    private boolean pongReceived = false;
    private final KademliaDHT kademliaDHT;


    private final Map<UUID, Send> messageQueue;
    private final Map<Host, Boolean> peerStatus;

    public Point2PointComm(Host commHost, short DHT_Proto_ID, Properties props, KademliaDHT kademliaDHT) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.myself = commHost;
        this.DHT_PROTO_ID = DHT_Proto_ID;
        this.kademliaDHT = kademliaDHT;
        this.lock = new Object();
        this.messageQueue = new HashMap<>();
        this.peerStatus = new HashMap<>();

        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address"));
        channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port"));
        channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000");
        channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000");
        channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000");
        channelId = createChannel(TCPChannel.NAME, channelProps);

        registerMessageSerializer(channelId, Message.MSG_ID, Message.serializer);
        registerMessageSerializer(channelId, PingMessage.MSG_ID, PingMessage.serializer);
        registerMessageSerializer(channelId, PongMessage.MSG_ID, PongMessage.serializer);
        registerMessageSerializer(channelId, FindNodeMessage.MSG_ID, FindNodeMessage.serializer);

        registerMessageHandler(channelId, Message.MSG_ID, this::uponMessage, this::uponMsgFail);
        registerMessageHandler(channelId, PingMessage.MSG_ID, this::uponPingMessage, this::uponMsgFail);
        registerMessageHandler(channelId, PongMessage.MSG_ID, this::uponPongMessage, this::uponMsgFail);
        registerMessageHandler(channelId, FindNodeMessage.MSG_ID, this::uponFindNodeMessage, this::uponMsgFail);
        registerRequestHandler(Send.REQUEST_ID, this::uponSendRequest);
        registerReplyHandler(LookupReply.REPLY_ID, this::uponLookupReply);
        registerTimerHandler(Timer.TIMER_ID, this::uponTimer);

        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
    }

    @Override
    public void init(Properties props) {
        logger.info("Point-to-Point communication protocol initialized on " + myself);

        triggerNotification(new ChannelCreated(channelId));

        if (props.containsKey("contact")) {
            try {
                String contact = props.getProperty("contact");
                String[] hostElems = contact.split(":");
                Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
                openConnection(contactHost);
                isPeerOnline(contactHost);
            } catch (Exception e) {
                logger.error("Invalid contact in configuration: '{}'", props.getProperty("contact"));
                e.printStackTrace();
            }
        }

        long RETRY_INTERVAL = 30000;
        setupPeriodicTimer(new Timer(), RETRY_INTERVAL, RETRY_INTERVAL);
    }

    private void uponMessage(Message msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);

        UUID messageID = msg.getMid();
        byte[] payload = msg.getContent();
        logger.info("Processing received message ID: {} with payload: {}", messageID, new String(payload));

        triggerNotification(new Deliver(msg.getSender(), messageID, payload));
    }

    private void uponPingMessage(PingMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Received Ping from peer: {}", from);

        kademliaDHT.updateRoutingTable(msg.getSenderID(), from);

        PongMessage pong = new PongMessage(kademliaDHT.getPeerID());
        sendMessage(channelId, pong, from);
    }

    private void uponPongMessage(PongMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Received Pong from peer: {}", from);

        synchronized (lock) {
            pongReceived = true;
            lock.notify();
        }

        kademliaDHT.updateRoutingTable(msg.getSenderID(), from);
        peerStatus.put(from, true);
    }

    private void uponFindNodeMessage(FindNodeMessage msg, Host host, short i, int i1) {
        List<Pair<byte[], Host>> myself = new ArrayList<>();
        List<Host> addedNodes = new ArrayList<>();
        for (Pair<byte[], Host> peer : msg.getPeers()) {
            if (!Arrays.equals(peer.getLeft(), kademliaDHT.getPeerID())) {
                openConnection(peer.getRight());
                kademliaDHT.updateRoutingTable(peer.getLeft(), peer.getRight());
                addedNodes.add(peer.getRight());
            } else {
                myself.add(peer);
            }

        }
        if (!myself.isEmpty() && !addedNodes.isEmpty()) {
            for (Host node : addedNodes) {
                if (!peerStatus.containsKey(node)) {
                    sendMessage(channelId, new FindNodeMessage(myself), node);
                }
            }

        }
    }

    private void uponSendRequest(Send request, short protoID) {
        logger.info("Received Send Request: " + request.toString() + " to " + request.getDestinationPeerIDHex());

        Lookup lookup = new Lookup(request.getDestinationPeerID(), request.getMessageID());

        sendRequest(lookup, DHT_PROTO_ID);

        messageQueue.put(request.getMessageID(), request);
    }

    private void uponLookupReply(LookupReply reply, short protoID) {
        logger.info("Received Lookup Reply: " + reply.toString());

        Iterator<Pair<byte[], Host>> peers = reply.getPeersIterator();

        if (peers.hasNext()) {
            Pair<byte[], Host> closestPeer = peers.next();

            if (isPeerOnline(closestPeer.getRight())) {
                Send originalRequest = messageQueue.get(reply.getMid());
                Message message = new Message(originalRequest.getMessageID(), closestPeer.getRight(), PROTOCOL_ID, originalRequest.getMessagePayload());
                sendMessage(channelId, message, closestPeer.getRight());
                messageQueue.remove(originalRequest.getMessageID());
                sendMessage(channelId, new FindNodeMessage(reply.getPeers()), closestPeer.getRight());
            } else {
                peers.remove();
                handleOfflinePeer(peers, reply);
            }
        } else {
            logger.warn("No peers found for destination: " + reply.getPeerIDHex());
        }
    }

    private void handleOfflinePeer(Iterator<Pair<byte[], Host>> it, LookupReply reply) {
        if (it.hasNext()) {
            Pair<byte[], Host> helperPeer = it.next();
            logger.info("Selected helper peer: " + HashProducer.toNumberFormat(helperPeer.getLeft()).toString(16));

            if (isPeerOnline(helperPeer.getRight())) {
                Send originalRequest = messageQueue.get(reply.getMid());
                Message message = new Message(originalRequest.getMessageID(), helperPeer.getRight(), PROTOCOL_ID, originalRequest.getMessagePayload());
                sendMessage(channelId, message, helperPeer.getRight());
                messageQueue.remove(originalRequest.getMessageID());
            } else {
                it.remove();
                handleOfflinePeer(it, reply);
            }
        } else {
            logger.warn("No helper peer found for offline peer: " + reply.getPeerIDHex());
        }
    }

    private boolean isPeerOnline(Host peer) {
        if (peerStatus.containsKey(peer) && peerStatus.get(peer)) {
            return true;
        }
        return pingPeer(peer);
    }

    private boolean pingPeer(Host peer) {
        logger.info("Pinging peer: {}", peer);

        synchronized (lock) {
            try {
                pongReceived = false;

                PingMessage ping = new PingMessage(kademliaDHT.getPeerID());
                sendMessage(channelId, ping, peer);

                lock.wait(2000);

                return pongReceived;

            } catch (InterruptedException e) {
                logger.error("Ping to peer {} was interrupted", peer, e);
                return false;
            } catch (Exception e) {
                logger.error("Ping to peer {} failed: {}", peer, e.getMessage());
                return false;
            }
        }
    }

    private void uponTimer(Timer timer, long timerId) {
        logger.info("Retrying pending messages...");
        for (Send request : messageQueue.values()) {
            sendRequest(request, DHT_PROTO_ID);
        }
    }

    private void uponMsgFail(ProtoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }

    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.info("Connection to peer " + peer + " is up");
        peerStatus.put(peer, true);
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.info("Connection to peer " + peer + " is down: " + event.getCause());
        peerStatus.put(peer, false);
    }

    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        logger.warn("Failed to connect to peer " + event.getNode() + ": " + event.getCause());
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.info("Incoming connection from " + peer + " is up");
        openConnection(peer);
        peerStatus.put(peer, true);
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.info("Incoming connection from " + peer + " is down: " + event.getCause());
        peerStatus.put(peer, false);
    }
}

