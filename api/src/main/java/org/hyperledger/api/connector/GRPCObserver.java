/**
 * Copyright 2016 Digital Asset Holdings, LLC
 *
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

package org.hyperledger.api.connector;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.hyperledger.api.*;
import org.hyperledger.block.BID;
import org.hyperledger.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protos.Chaincode;
import protos.EventsGrpc;
import protos.EventsOuterClass;
import protos.Fabric.TransactionResult;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GRPCObserver {
    private static final Logger log = LoggerFactory.getLogger(GRPCObserver.class);

    private EventsGrpc.EventsStub es;
    private Set<TransactionListener> txListeners = new HashSet<>();
    private Set<TrunkListener> trunkListeners = new HashSet<>();
    private Set<RejectListener> rejectionListeners = new HashSet<>();

    public GRPCObserver(Channel eventsChannel) {
        es = EventsGrpc.newStub(eventsChannel);
    }

    public void connect() {
        StreamObserver<EventsOuterClass.Event> receiver = new StreamObserver<EventsOuterClass.Event>() {
            @Override
            public void onNext(EventsOuterClass.Event openchainEvent) {
                if (openchainEvent.getEventCase() == EventsOuterClass.Event.EventCase.BLOCK) {
                    List<HLAPITransaction> transactionsList = convertToHLAPITxList(openchainEvent.getBlock().getTransactionsList());
                    HLAPIBlock block = new HLAPIBlock.Builder().transactions(transactionsList).build();
                    Map<String, TransactionResult> results = openchainEvent.getBlock().getNonHashData()
                            .getTransactionResultsList().stream()
                            .collect(Collectors.toMap(TransactionResult::getUuid, item -> item));

                    serveTransactionListeners(transactionsList, results);
                    serveRejectionListeners(transactionsList, results);
                    serveTrunkListeners(block);
                }
                System.out.println("new event: " + openchainEvent.toString());
            }

            private void serveTransactionListeners(List<HLAPITransaction> transactionsList, Map<String, TransactionResult> results) {
                for (HLAPITransaction tx : transactionsList) {
                    txListeners.forEach((txListener) -> {
                        try {
                            TransactionResult result = results.get(tx.getID().toUuidString());
                            if (result.getErrorCode() == 0) {
                                txListener.process(tx);
                            }
                        } catch (HLAPIException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            private void serveRejectionListeners(List<HLAPITransaction> transactionsList, Map<String, TransactionResult> results) {
                for (HLAPITransaction tx : transactionsList) {
                    rejectionListeners.forEach((rjListener) -> {
                        TransactionResult result = results.get(tx.getID().toUuidString());
                        if (result.getErrorCode() != 0) {
                            rjListener.rejected("invoke", tx.getID(), result.getError(), result.getErrorCode());
                        }
                    });
                }
            }

            private void serveTrunkListeners(HLAPIBlock block) {
                List<HLAPIBlock> blocks = new ArrayList<>(1);
                blocks.add(block);
                trunkListeners.forEach((blockListener) -> {
                    blockListener.trunkUpdate(blocks);
                });
            }

            private List<HLAPITransaction> convertToHLAPITxList(List<protos.Fabric.Transaction> transactionsList) {
                List<HLAPITransaction> result = new ArrayList<>(transactionsList.size());
                for (protos.Fabric.Transaction tx : transactionsList) {
                    ByteString invocationSpecBytes = tx.getPayload();
                    Chaincode.ChaincodeInvocationSpec invocationSpec;
                    try {
                        invocationSpec = Chaincode.ChaincodeInvocationSpec.parseFrom(invocationSpecBytes);
                        String transactionString = invocationSpec.getChaincodeSpec().getCtorMsg().getArgs(0);
                        byte[] transactionBytes = DatatypeConverter.parseBase64Binary(transactionString);
                        HLAPITransaction hlapitx = new HLAPITransaction(Transaction.fromByteArray(transactionBytes), BID.INVALID);
                        result.add(hlapitx);
                    } catch (IOException e) {
                        log.error("Error when processing transaction: {}", e.getMessage());
                    }

                }
                return result;
            }

            @Override
            public void onError(Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            @Override
            public void onCompleted() {
                System.out.println("onComplete");
            }
        };

        StreamObserver<EventsOuterClass.Event> sender = es.chat(receiver);

        EventsOuterClass.Interest interest = EventsOuterClass.Interest.newBuilder()
                .setEventType("block")
                .setResponseType(EventsOuterClass.Interest.ResponseType.PROTOBUF)
                .build();

        EventsOuterClass.Register register = EventsOuterClass.Register.newBuilder()
                .addEvents(0, interest)
                .build();

        EventsOuterClass.Event registerEvent = EventsOuterClass.Event.newBuilder()
                .setRegister(register)
                .build();

        sender.onNext(registerEvent);
    }

    public void subscribeToTransactions(TransactionListener l) {
        txListeners.add(l);
    }

    public void unsubscribeFromTransactions(TransactionListener l) {
        txListeners.remove(l);
    }

    public void subscribeToBlocks(TrunkListener l) {
        trunkListeners.add(l);
    }

    public void unsubscribeFromBlocks(TrunkListener l) {
        trunkListeners.remove(l);
    }

    public void subscribeToRejections(RejectListener l) {
        rejectionListeners.add(l);
    }

    public void unsubscribeFromRejections(RejectListener l) {
        rejectionListeners.remove(l);
    }
}
