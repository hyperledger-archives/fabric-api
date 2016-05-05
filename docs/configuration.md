# Configuration
Configuration goes under the ```hyperledger``` section of a conf file. Within that you can configure various features of the node listed below.

## Configuration examples

### Integration testing between own PBFT nodes
Three configs for three nodes pointing to each other
 * [server/pbft/src/dist/conf/pbft1.conf](../server/pbft/src/dist/conf/pbft1.conf)
 * [server/pbft/src/dist/conf/pbft2.conf](../server/pbft/src/dist/conf/pbft2.conf)
 * [server/pbft/src/dist/conf/pbft3.conf](../server/pbft/src/dist/conf/pbft3.conf)
 
### Bitcoin compatible mining node
 * For production network: [server/main/conf/production_miner.conf](../server/main/conf/production_miner.conf)
 * For test network: [server/main/conf/testnet_miner.conf](../server/main/conf/testnet_miner.conf)

### Integration testing between own bitcoin compatible nodes 
 * [server/main/src/dist/conf/bitcoin1.conf](../server/main/src/dist/conf/bitcoin1.conf)
 * [server/main/src/dist/conf/bitcoin2.conf](../server/main/src/dist/conf/bitcoin2.conf)
 
### Running server and client in two processes connected with ActiveMQ
 * [server/main/src/dist/conf/production_with_jms.conf](../server/main/src/dist/conf/production_with_jms.conf)
 * [server/main/src/dist/conf/activemq.xml](../server/main/src/dist/conf/activemq.xml)

## Explaining the config options
### Blockchain
Which bitcoin blockchain to use. The _chain_ can have one of _production_, _testnet_, _regtest_ and _signedregtest_ values. The latter one is for the block signing feature, incompatible with the bitcoin network. See it below.

```
  blockchain {
    chain: "production"
  }
```

### Network
This is to configure the a (similar to) bitcoin network. 

You can either configure _network_ or _pbft_ (listed below) as they are fundamentally incompatible with each other.
```
  // network configuration to participate in a bitcoin p2p network 
  network {
    // limit the number of peers to connet to
    outgoingConnections: 1

    // the listening address and port of this node
    bindAddress: "0.0.0.0"
    bindPort: 18440
    
    // nodes to connect to
    discovery: [{
      // fixed set only, no dynamic discovery available yet
      type: fixed
      peers: [
        "192.168.0.1:8333"
        "192.168.0.2:8333"
      ]
    }]
  }
```

### Practical Byzantine Fault Tolerance (PBFT)
This is an alternative configuration to for a network where consensus is based on practical byzantine fault tolerance instead of the proof of work of a bitcoin network.

You can either configure _network_ or _pbft_ as they are fundamentally incompatible with each other.
```
  pbft {
    bindAddress: "127.0.0.1:8551"
    privateKey: "L2uwgqBffvx4KXjbdiVQdFfx4ALrrCbdr2ru7ucjPtbWjPnFCU8e"
    nodes: [
      {address: "127.0.0.1:8551", publicKey: "031a8027f5c2ab3f4e3e76c8c38f754484241bb1002b44f8b83217c47bb7cc0a87"},
      {address: "127.0.0.1:8552", publicKey: "0316f8caac24fb7dc113f21c870166700bc33377370ed9b3c3b2566858e279522b"}
      {address: "127.0.0.1:8553", publicKey: "0298751746d1456c8235922a6656caae5c7e7a1ca581de55248b3c3d24ad8daf63"}
    ]
  }
```

### Storing blocks and transactions
In LevelDB in a persistent way
```
  store {
    default-leveldb {
      database: data
      cacheSize: 104857600
    }
```

Or in memory in a non-persistent way
```
  store {
    memory: true
  }
```


### Connecting the server and client sides
Connection is through ActiveMQ. Connection is made to it through the below url using the below credentials. Not having this configuration implies using in-memory connector, i.e. the embedded configuration.

```
  connector {
    jms {
       username: "bitsofproof-server"
       password: "somepassword"
       brokerUrl: "tcp://localhost:61616"
    }
  }
```

### Mining
```
  mining {
    // true or false
    enabled: true
    // address to receive coinbase and mining fees
    minerAddress: "1CNABTVtwxFQBTvazuGfxhT87sfssmFbdE"
    // extra delay between mining blocks, for testing purposes
    delayBetweenMiningBlocksSecs: 0
  }
```

### Pruning
This is a feature to regularly remove transactions from the storage to decrease its size. 

```
  pruning {
    enabled: false
    // not used
    pruneAfterEvery: 25000
    // prune transactions in blocks which height is lower than this, -1 means no limit
    pruneOnlyLowerThanHeight: -1
    // do not prune transactions in the top N blocks
    doNotPruneTopBlocks: 10
    // prune transaction only in blocks which height is greater than or equal to
    pruneFrom: 207480
  }

```

### Features 
```
  feature {
    // check lock time verify
    cltv: false
    // signature covers input value
    sciv: false
    // signature covers input color
    scic: false
    // confidential transactions
    ct: false
    // enable native assets
    native-assets: false
    // transaction hash does not cover transaction signatures to avoid transaction malleability
    siglessTxId: false
  }
```

### Client
```
  client {
    timeout: "10m"
  }
```

### Signing the blocks
[Block signature](blocksignature.md) is to prove who created the given block and who can create the next one. This adds an extra field to the header to contain a scripts similar to the input and output P2SH scripts of the transactions. 

This is not compatible with the bitcoin network.

```
  blockSignature {
    enabled: true
    requiredSignatureCount: 1
    // WIF format
    minerPrivateKey: L1hUW4GmVdJWPQLm3uRw4jC8TaRGDSn7HNrEciacb9quTFZ1Uzi7
    // HEX format
    publicKeys: [
      03ae9d05bb3911dff167c34a6368676ebf8c427a180e563f606a7eb743914c2e81
      03ba646c26794fa99b0df6c6d13f14ef5944938f2de256a64fc3768349b67a890e
    ]
  }
```



