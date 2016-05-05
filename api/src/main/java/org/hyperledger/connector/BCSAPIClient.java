/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.connector;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.api.*;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class BCSAPIClient implements BCSAPI {
    private static final Logger log = LoggerFactory.getLogger(BCSAPIClient.class);

    private final ConnectorFactory connectionFactory;
    private final HyperLedgerSettings clientSettings;
    private Connector connection;
    private final String clientVersion;

    private final Map<String, MessageDispatcher> messageDispatcher = new HashMap<>();

    public BCSAPIClient(ConnectorFactory connectionFactory) {
        this(connectionFactory, HyperLedgerSettings.getInstance());
    }

    public BCSAPIClient(ConnectorFactory connectionFactory, HyperLedgerSettings clientSettings) {
        this.connectionFactory = connectionFactory;
        this.clientSettings = clientSettings;
        BufferedReader input = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("version.txt")));
        String version = "unknown";
        try {
            version = input.readLine();
            input.close();
        } catch (IOException e) {
            discard(e);
        }
        clientVersion = version;
    }

    private interface TypedListener<T> {
        void onMessage(T array);
    }

    private interface ByteArrayConverter<T> {
        T convert(byte[] in);
    }

    private class MessageDispatcher<T> {
        private final Map<Object, TypedListener<T>> wrapperMap = new HashMap<>();

        private final ConnectorConsumer consumer;

        public MessageDispatcher(ConnectorConsumer consumer, ByteArrayConverter<T> converter) {
            this.consumer = consumer;
            try {
                consumer.setMessageListener(new ConnectorListener() {
                    @Override
                    public void onMessage(ConnectorMessage message) {
                        try {
                            T msg = converter.convert(message.getPayload());
                            List<TypedListener<T>> listenerList = new ArrayList<>();
                            synchronized (wrapperMap) {
                                listenerList.addAll(wrapperMap.values());
                            }
                            for (TypedListener<T> listener : listenerList) {
                                listener.onMessage(msg);
                            }
                        } catch (ConnectorException e) {
                            log.error("Unable to extract payload", e);
                        }
                    }
                });
            } catch (ConnectorException e) {
                log.error("Can not attach message listener ", e);
            }
        }

        public void addListener(Object inner, TypedListener<T> listener) {
            synchronized (wrapperMap) {
                wrapperMap.put(inner, listener);
            }
        }

        public void removeListener(Object inner) {
            synchronized (wrapperMap) {
                wrapperMap.remove(inner);
            }
        }

        public boolean isListened() {
            synchronized (wrapperMap) {
                return !wrapperMap.isEmpty();
            }
        }

        public ConnectorConsumer getConsumer() {
            return consumer;
        }
    }

    private static void discard(Exception e) {
        log.debug("Discarded " + e.getClass().getName() + ": " + e.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> void addTopicListener(String topic, Object inner, ByteArrayConverter<T> converter, TypedListener<T> listener) throws ConnectorException {
        synchronized (messageDispatcher) {
            MessageDispatcher<T> dispatcher = messageDispatcher.get(topic);
            if (dispatcher == null) {
                ConnectorSession session = connection.createSession();
                ConnectorConsumer consumer = session.createConsumer(session.createTopic(topic));
                messageDispatcher.put(topic, dispatcher = new MessageDispatcher<>(consumer, converter));
            }
            dispatcher.addListener(inner, listener);
        }
    }

    private void removeTopicListener(String topic, Object inner) {
        synchronized (messageDispatcher) {
            MessageDispatcher dispatcher = messageDispatcher.get(topic);
            if (dispatcher != null) {
                dispatcher.removeListener(inner);
                if (!dispatcher.isListened()) {
                    messageDispatcher.remove(topic);
                    try {
                        dispatcher.getConsumer().close();
                    } catch (ConnectorException e) {
                        discard(e);
                    }
                }
            }
        }
    }

    public void init() {
        try {
            log.debug("Initialize BCSAPI connector");
            connection = connectionFactory.getConnector();
            connection.start();
        } catch (Exception e) {
            log.error("Can not create connector", e);
        }
    }

    public void destroy() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (ConnectorException e) {
            discard(e);
        }
    }

    private BCSAPIMessage.Ping handshake(long nonce) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("ping {}", nonce);

            ConnectorMessage m = session.createMessage();
            BCSAPIMessage.Ping.Builder builder = BCSAPIMessage.Ping.newBuilder();
            builder.setNonce(nonce);
            builder.setClientVersion(getClientVersion());
            m.setPayload(builder.build().toByteArray());
            ConnectorProducer pingProducer = session.createProducer(session.createQueue("ping"));
            byte[] response = synchronousRequest(session, pingProducer, m);
            if (response != null) {
                BCSAPIMessage.Ping echo = BCSAPIMessage.Ping.parseFrom(response);
                if (echo.getNonce() != nonce) {
                    throw new BCSAPIException("Incorrect echo nonce from ping");
                }
                return echo;
            }
            throw new BCSAPIException("no reply");
        } catch (ConnectorException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }
    }


    @Override
    public String getClientVersion() throws BCSAPIException {
        return clientVersion;
    }

    @Override
    public String getServerVersion() throws BCSAPIException {
        return handshake(0).getServerVersion();
    }

    @Override
    public long ping(long nonce) throws BCSAPIException {
        return handshake(nonce).getNonce();
    }

    @Override
    public void addAlertListener(final AlertListener alertListener) throws BCSAPIException {
        try {
            addTopicListener("alert", alertListener, bytes -> {
                BCSAPIMessage.Alert alert = null;
                try {
                    alert = BCSAPIMessage.Alert.parseFrom(bytes);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Alert message format error", e);
                }
                return alert;
            }, alert -> {
                if (alert != null)
                    alertListener.alert(alert.getAlert(), alert.getSeverity());
            });
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void removeAlertListener(AlertListener listener) {
        removeTopicListener("alert", listener);
    }

    @Override
    public void scanTransactionsForAddresses(Set<Address> addresses, TransactionListener listener) throws BCSAPIException {
        List<Script> al = new ArrayList<>(addresses.size());
        for (Address a : addresses) {
            try {
                al.add(a.getAddressScript());
            } catch (HyperLedgerException e) {
                discard(e);
            }
        }
        scanRequest(al, listener, "matchRequest");
    }

    @Override
    public void scanTransactions(MasterPublicKey master, int lookAhead, final TransactionListener listener) throws BCSAPIException {
        scanRequest(master, lookAhead, listener, "accountRequest");
    }

    private void scanRequest(Collection<Script> match, final TransactionListener listener, String requestQueue)
            throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            ConnectorMessage m = session.createMessage();

            ConnectorProducer exactMatchProducer = session.createProducer(session.createQueue(requestQueue));
            BCSAPIMessage.ExactMatchRequest.Builder builder = BCSAPIMessage.ExactMatchRequest.newBuilder();
            for (Script d : match) {
                builder.addMatch(ByteString.copyFrom(d.toByteArray()));
            }
            m.setPayload(builder.build().toByteArray());
            final ConnectorTemporaryQueue answerQueue = session.createTemporaryQueue();
            final ConnectorConsumer consumer = session.createConsumer(answerQueue);
            m.setReplyTo(answerQueue);
            final Semaphore ready = new Semaphore(0);
            consumer.setMessageListener(message -> {
                try {
                    byte[] body = message.getPayload();
                    if (body != null) {
                        APITransaction t = APITransaction.fromProtobuf(BCSAPIMessage.TX.parseFrom(body));
                        listener.process(t);
                    } else {
                        consumer.close();
                        answerQueue.delete();
                        ready.release();
                    }
                } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
                    log.error("Malformed message received for scan matching transactions", e);
                }
            });

            exactMatchProducer.send(m);
            ready.acquireUninterruptibly();
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    private void scanRequest(MasterPublicKey master, int lookAhead, final TransactionListener listener, String request)
            throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            ConnectorMessage m = session.createMessage();

            ConnectorProducer scanAccountProducer = session.createProducer(session.createQueue(request));
            BCSAPIMessage.AccountRequest.Builder builder = BCSAPIMessage.AccountRequest.newBuilder();
            builder.setPublicKey(master.serialize(true));
            builder.setLookAhead(lookAhead);
            m.setPayload(builder.build().toByteArray());

            final ConnectorTemporaryQueue answerQueue = session.createTemporaryQueue();
            final ConnectorConsumer consumer = session.createConsumer(answerQueue);
            m.setReplyTo(answerQueue);
            final Semaphore ready = new Semaphore(0);
            consumer.setMessageListener(message -> {
                try {
                    byte[] body = message.getPayload();
                    if (body != null) {
                        APITransaction t = APITransaction.fromProtobuf(BCSAPIMessage.TX.parseFrom(body));
                        listener.process(t);
                    } else {
                        consumer.close();
                        answerQueue.delete();
                        ready.release();
                    }
                } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
                    log.error("Malformed message received for account scan transactions", e);
                }
            });

            scanAccountProducer.send(m);
            ready.acquireUninterruptibly();
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void catchUp(List<BID> inventory, int limit, boolean headers, final TrunkListener listener) throws BCSAPIException {
        log.trace("catchUp");
        ConnectorMessage m;
        try (ConnectorSession session = connection.createSession()) {
            ConnectorProducer transactionRequestProducer = session.createProducer(session.createQueue("catchUpRequest"));

            m = session.createMessage();
            BCSAPIMessage.CatchUpRequest.Builder builder = BCSAPIMessage.CatchUpRequest.newBuilder();
            builder.setLimit(limit);
            builder.setHeaders(true);
            for (BID hash : inventory) {
                builder.addInventory(ByteString.copyFrom(hash.unsafeGetArray()));
            }
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, transactionRequestProducer, m);
            if (response != null) {
                BCSAPIMessage.TrunkUpdate blockMessage = BCSAPIMessage.TrunkUpdate.parseFrom(response);
                List<APIBlock> blockList = new ArrayList<>();
                for (BCSAPIMessage.BLK b : blockMessage.getAddedList()) {
                    blockList.add(APIBlock.fromProtobuf(b));
                }
                listener.trunkUpdate(blockList);
            }
        } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void spendingTransactions(List<TID> tids, final TransactionListener listener)
            throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            ConnectorMessage m = session.createMessage();

            ConnectorProducer scanAccountProducer = session.createProducer(session.createQueue("spendingTransactions"));
            BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder();
            for (TID tid : tids) {
                builder.addHash(ByteString.copyFrom(tid.unsafeGetArray()));
            }
            m.setPayload(builder.build().toByteArray());

            final ConnectorTemporaryQueue answerQueue = session.createTemporaryQueue();
            final ConnectorConsumer consumer = session.createConsumer(answerQueue);
            m.setReplyTo(answerQueue);
            final Semaphore ready = new Semaphore(0);
            consumer.setMessageListener(message -> {
                try {
                    byte[] body = message.getPayload();
                    if (body != null) {
                        APITransaction t = APITransaction.fromProtobuf(BCSAPIMessage.TX.parseFrom(body));
                        listener.process(t);
                    } else {
                        consumer.close();
                        answerQueue.delete();
                        ready.release();
                    }
                } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
                    log.error("Malformed message received for spending transactions request", e);
                }
            });

            scanAccountProducer.send(m);
            ready.acquireUninterruptibly();
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void registerTransactionListener(final TransactionListener listener) throws BCSAPIException {
        try {
            addTopicListener("transaction", listener, bytes -> {
                        APITransaction transaction = null;
                        try {
                            transaction = APITransaction.fromProtobuf(BCSAPIMessage.TX.parseFrom(bytes));
                        } catch (Exception e) {
                            log.error("Transaction message error", e);
                        }
                        return transaction;
                    },
                    transaction -> {
                        if (transaction != null)
                            try {
                                listener.process(transaction);
                            } catch (HyperLedgerException e) {
                                log.error("Error in transaction processing", e);
                            }
                    }
            );
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        removeTopicListener("transaction", listener);
    }

    @Override
    public void registerTrunkListener(final TrunkListener listener) throws BCSAPIException {
        try {
            addTopicListener("trunk", listener, body -> {
                        List<APIBlock> blockList = null;
                        try {
                            BCSAPIMessage.TrunkUpdate blockMessage = BCSAPIMessage.TrunkUpdate.parseFrom(body);
                            blockList = new ArrayList<>();
                            for (BCSAPIMessage.BLK b : blockMessage.getAddedList()) {
                                blockList.add(APIBlock.fromProtobuf(b));
                            }
                        } catch (Exception e) {
                            log.error("Block message error", e);
                        }
                        return blockList;
                    },
                    listener::trunkUpdate);
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void removeTrunkListener(TrunkListener listener) {
        removeTopicListener("trunk", listener);
    }

    private byte[] synchronousRequest(ConnectorSession session, ConnectorProducer producer, ConnectorMessage m) throws BCSAPIException {
        ConnectorTemporaryQueue answerQueue = null;
        try {
            answerQueue = session.createTemporaryQueue();
            m.setReplyTo(answerQueue);

            try (ConnectorConsumer consumer = session.createConsumer(answerQueue)) {
                producer.send(m);
                ConnectorMessage reply = consumer.receive(clientSettings.getTimeout());
                if (reply == null) {
                    throw new BCSAPIException("timeout");
                }
                return reply.getPayload();
            }
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        } finally {
            try {
                if (answerQueue != null) {
                    answerQueue.delete();
                }
            } catch (ConnectorException e) {
                discard(e);
            }
        }
    }

    @Override
    public APITransaction getTransaction(TID hash) throws BCSAPIException {
        log.trace("get transaction {}", hash);
        ConnectorMessage m;
        try (ConnectorSession session = connection.createSession()) {
            ConnectorProducer transactionRequestProducer = session.createProducer(session.createQueue("transactionRequest"));

            m = session.createMessage();
            BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder();
            builder.addHash(ByteString.copyFrom(hash.unsafeGetArray()));
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, transactionRequestProducer, m);
            if (response != null) {
                APITransaction t;
                t = APITransaction.fromProtobuf(BCSAPIMessage.TX.parseFrom(response));
                return t;
            }
        } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }

        return null;
    }

    @Override
    public List<APITransaction> getInputTransactions(TID txId) throws BCSAPIException {
        log.trace("get input transactions {}", txId);
        ConnectorMessage m;
        try (ConnectorSession session = connection.createSession()) {
            ConnectorProducer transactionRequestProducer = session.createProducer(session.createQueue("inputTransactionsRequest"));

            m = session.createMessage();
            BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder();
            builder.addHash(ByteString.copyFrom(txId.unsafeGetArray()));
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, transactionRequestProducer, m);
            if (response != null) {
                List<BCSAPIMessage.OPTIONAL_TX> txsList = BCSAPIMessage.TXS.parseFrom(response).getTxsList();
                List<APITransaction> txs = new ArrayList<>(txsList.size());
                for (BCSAPIMessage.OPTIONAL_TX tx : txsList) {
                    if (tx.getIsNull()) {
                        txs.add(null);
                    } else {
                        txs.add(APITransaction.fromProtobuf(tx.getTransaction()));
                    }
                }
                return txs;
            }
        } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }

        return null;
    }

    @Override
    public int getChainHeight() throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("get chain height");

            ConnectorProducer heightRequestProducer = session.createProducer(session.createQueue("chainHeightRequest"));

            ConnectorMessage m = session.createMessage();
            byte[] response = synchronousRequest(session, heightRequestProducer, m);
            if (response != null) {
                return BCSAPIMessage.HEIGHT.parseFrom(response).getHeight();
            }
        } catch (ConnectorException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }
        return -1;
    }

    @Override
    public APIBlockIdList getBlockIds(BID blockId, int count) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("get {} block ids from {}", count, blockId);

            ConnectorProducer blockIdsRequestProducer = session.createProducer(session.createQueue("blockIdsRequest"));

            ConnectorMessage m = session.createMessage();
            BCSAPIMessage.BLKIDSREQ.Builder builder = BCSAPIMessage.BLKIDSREQ.newBuilder();
            if (blockId != null) {
                builder.setBlockHash(ByteString.copyFrom(blockId.unsafeGetArray()));
            }
            if (count <= 0) count = 20;
            builder.setCount(count);
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, blockIdsRequestProducer, m);
            if (response != null) {
                BCSAPIMessage.BLKIDS message = BCSAPIMessage.BLKIDS.parseFrom(response);
                List<ByteString> blockIdsList = message.getBlockIdsList();
                List<BID> blockIds = blockIdsList.stream().map(bs -> new BID(bs.toByteArray())).collect(Collectors.toList());
                return new APIBlockIdList(blockIds, message.getHeight(), message.hasPreviousBlockId() ? new BID(message.getPreviousBlockId().toByteArray()) : null);
            }
        } catch (ConnectorException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }

        return null;
    }

    @Override
    public APIBlock getBlock(BID hash) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("get block {}", hash);

            ConnectorProducer blockRequestProducer = session.createProducer(session.createQueue("blockRequest"));

            ConnectorMessage m = session.createMessage();
            BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder();
            builder.addHash(ByteString.copyFrom(hash.unsafeGetArray()));
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, blockRequestProducer, m);
            if (response != null) {
                return APIBlock.fromProtobuf(BCSAPIMessage.BLK.parseFrom(response));
            }
        } catch (ConnectorException | HyperLedgerException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }

        return null;
    }

    @Override
    public APIHeader getBlockHeader(BID hash) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("get block header {}", hash);

            ConnectorProducer blockHeaderRequestProducer = session.createProducer(session.createQueue("headerRequest"));

            ConnectorMessage m = session.createMessage();
            BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder();
            builder.addHash(ByteString.copyFrom(hash.unsafeGetArray()));
            m.setPayload(builder.build().toByteArray());
            byte[] response = synchronousRequest(session, blockHeaderRequestProducer, m);
            if (response != null) {
                return APIHeader.fromProtobuf(BCSAPIMessage.BLK.parseFrom(response));
            }
        } catch (ConnectorException | InvalidProtocolBufferException e) {
            throw new BCSAPIException(e);
        }

        return null;
    }

    @Override
    public void sendTransaction(Transaction transaction) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("send transaction {}", transaction.getID());

            ConnectorProducer transactionProducer = session.createProducer(session.createTopic("newTransaction"));

            ConnectorMessage m = session.createMessage();
            m.setPayload(transaction.toBCSAPIMessage().toByteArray());
            byte[] reply = synchronousRequest(session, transactionProducer, m);
            if (reply != null) {
                try {
                    BCSAPIMessage.ExceptionMessage em = BCSAPIMessage.ExceptionMessage.parseFrom(reply);
                    throw new BCSAPIException(em.getMessage(0));
                } catch (InvalidProtocolBufferException e) {
                    throw new BCSAPIException("Invalid response", e);
                }
            }
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public APIHeader mine(Address address) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("mine to {}", address);

            ConnectorProducer producer = session.createProducer(session.createTopic("mine"));

            ConnectorMessage m = session.createMessage();
            BCSAPIMessage.Script.Builder builder =
                    BCSAPIMessage.Script.newBuilder().setScript(ByteString.copyFrom(address.getAddressScript().toByteArray()));
            m.setPayload(builder.build().toByteArray());
            byte[] reply = synchronousRequest(session, producer, m);
            if (reply != null) {
                return APIHeader.fromProtobuf(BCSAPIMessage.BLK.parseFrom(reply));
            }
        } catch (ConnectorException | InvalidProtocolBufferException | HyperLedgerException e) {
            throw new BCSAPIException(e);
        }
        return null;
    }


    @Override
    public void sendBlock(Block block) throws BCSAPIException {
        try (ConnectorSession session = connection.createSession()) {
            log.trace("send block {}", block.getID());
            ConnectorProducer blockProducer = session.createProducer(session.createTopic("newBlock"));

            ConnectorMessage m = session.createMessage();
            m.setPayload(block.toBCSAPIMessage().toByteArray());
            byte[] reply = synchronousRequest(session, blockProducer, m);
            if (reply != null) {
                try {
                    BCSAPIMessage.ExceptionMessage em = BCSAPIMessage.ExceptionMessage.parseFrom(reply);
                    throw new BCSAPIException(em.getMessage(0));
                } catch (InvalidProtocolBufferException e) {
                    throw new BCSAPIException("Invalid response", e);
                }
            }
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void registerRejectListener(final RejectListener rejectListener) throws BCSAPIException {
        try {
            addTopicListener("reject", rejectListener, body -> {
                        BCSAPIMessage.Reject reject = null;
                        try {
                            reject = BCSAPIMessage.Reject.parseFrom(body);
                        } catch (Exception e) {
                            log.error("Reject message error", e);
                        }
                        return reject;
                    },
                    reject -> {
                        if (reject != null) {
                            rejectListener.rejected(reject.getCommand(), Hash.createFromSafeArray(reject.getHash().toByteArray()), reject.getReason(),
                                    reject.getRejectCode());
                        }
                    });
        } catch (ConnectorException e) {
            throw new BCSAPIException(e);
        }
    }

    @Override
    public void removeRejectListener(RejectListener rejectListener) {
        removeTopicListener("reject", rejectListener);
    }
}
