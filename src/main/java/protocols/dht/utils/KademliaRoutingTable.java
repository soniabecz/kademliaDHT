package protocols.dht.utils;

import org.apache.commons.lang3.tuple.Pair;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashProducer;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.math.BigInteger;

import java.math.BigInteger;
import java.util.*;

public class KademliaRoutingTable {

    private static final int K = 20;
    private final List<LinkedList<Pair<byte[], Host>>> kBuckets;
    private final byte[] myPeerID;

    public KademliaRoutingTable(byte[] myPeerID) {
        this.myPeerID = myPeerID;
        int bucketCount = 256;
        kBuckets = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            kBuckets.add(new LinkedList<>());
        }
    }

    public List<Pair<byte[], Host>> findClosestPeers(byte[] targetID) {
        int bucketIndex = getBucketIndex(targetID);
        return new ArrayList<>(kBuckets.get(bucketIndex)); // Return a copy to avoid modification
    }

    public void update(byte[] peerID, Host peerHost) {
        int bucketIndex = getBucketIndex(peerID);
        LinkedList<Pair<byte[], Host>> bucket = kBuckets.get(bucketIndex);

        Pair<byte[], Host> existingPeer = findPeer(peerID);
        if (existingPeer != null) {
            bucket.remove(existingPeer);
            bucket.addFirst(Pair.of(peerID, peerHost));
        } else if (bucket.size() < K) {
            bucket.addFirst(Pair.of(peerID, peerHost));
        } else {
            bucket.removeLast(); // Remove LRU entry
            bucket.addFirst(Pair.of(peerID, peerHost));
        }
    }

    public Pair<byte[], Host> findPeer(byte[] peerID) {
        int bucketIndex = getBucketIndex(peerID);
        LinkedList<Pair<byte[], Host>> bucket = kBuckets.get(bucketIndex);

        for (Pair<byte[], Host> pair : bucket) {
            if (Arrays.equals(pair.getLeft(), peerID)) {
                return pair;
            }
        }
        return null;
    }

    private int getBucketIndex(byte[] peerID) {
        BigInteger distance = HashProducer.toNumberFormat(peerID).xor(HashProducer.toNumberFormat(myPeerID));
        return distance.bitLength() - 1;
    }
}

