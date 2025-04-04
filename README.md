# KademliaDHT 

## 📌 Overview

This project implements a decentralized, peer-to-peer communication protocol based on the **Kademlia Distributed Hash Table (DHT)**. It enables efficient and fault-tolerant message routing in dynamic and unreliable network environments, using a point-to-point communication system and helper node mechanism.

The implementation is built using **Java** within the **Babel framework**.

## ✨ Features

- ✅ **Kademlia DHT Protocol** for efficient peer discovery and routing
- 🔁 **Proximity-aware Routing** using XOR-based distance metric
- 📡 **Point-to-Point Messaging** with support for dynamic IPs and port changes
- 🧭 **Helper Nodes** to buffer messages for offline nodes
- 🧪 **Interactive & Batch Modes** for debugging and testing at scale

## 🏗️ Architecture

### 1. Kademlia DHT
- Nodes are arranged using a binary tree and routed based on XOR distance.
- Each node maintains **k-buckets**, dynamically populated based on observed traffic.
- Implements efficient **node lookup** and **routing table updates**.

### 2. Point-to-Point Communication
- Messages are routed using the DHT.
- If a node is offline, a **helper node** is selected to temporarily store and later forward the message.
- Duplicate prevention through **message IDs** and **acknowledgments**.

### 3. Application Layer
- **Interactive mode** for debugging
- **Batch mode** for scalability tests
- Logs performance metrics like message delivery rates and latency

## 🚀 Getting Started

### Prerequisites

- Java 8+
- Maven
- Babel framework (configured via `babel_config.properties`)

### Installation

```bash
git clone https://github.com/soniabecz/kademliaDHT.git
cd kademliaDHT
mvn clean install
```

### Running the Project

1. Configure networking in `babel_config.properties`
2. Launch a node using one of the main classes
3. Optionally, run in batch mode for simulations

## 📂 Project Structure

```
.
├── src/                   # Java source code
│   ├── dht/              # Kademlia protocol logic
│   ├── comm/             # Point-to-point messaging logic
│   └── app/              # Application layer
├── babel_config.properties
├── log4j2.xml
└── pom.xml
```

## 🧪 Experimental Evaluation

The evaluation framework is in place, but empirical tests on performance metrics such as **latency**, **message reliability**, and **scalability** were not completed. Future work will focus on benchmarking these aspects.

## ✅ Future Improvements

- Full experimental evaluation
- Integrate adaptive routing
- Handle large-scale churn scenarios
- Add duplicate message detection and suppression
