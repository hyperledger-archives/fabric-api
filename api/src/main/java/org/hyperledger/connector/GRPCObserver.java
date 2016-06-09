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

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import org.hyperledger.api.HLAPIBlock;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.RejectListener;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.api.TrunkListener;
import org.hyperledger.common.BID;
import org.hyperledger.common.TID;
import org.hyperledger.common.Transaction;
import protos.Chaincode;
import protos.EventsGrpc;
import protos.EventsOuterClass;
import protos.EventsOuterClass.Rejection;
import protos.Fabric.Block;
import protos.Fabric.TransactionResult;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class GRPCObserver {
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
                    serveTransactionListeners(transactionsList);
                    serveTrunkListeners(block);
                }
                if (openchainEvent.getEventCase() == EventsOuterClass.Event.EventCase.REJECTION) {
                    serveRejectionListeners(openchainEvent.getRejection());
                }
                System.out.println("new event: " + openchainEvent.toString());
            }

            private void serveRejectionListeners(Rejection rejection) {
                rejectionListeners.forEach((rjListener) -> {
                    rjListener.rejected("invoke", TID.fromUuidString(rejection.getUuid()), rejection.getErrorMsg(), 1);
                });
            }

            private void serveTransactionListeners(List<HLAPITransaction> transactionsList) {
                for(HLAPITransaction tx : transactionsList) {
                    txListeners.forEach((txListener) -> {
                        try {
                            // It is possible here to use only TXs that had a zero return value
                            // TransactionResult result = results.get(tx.getID().toUuidString());
                            // if (0 == Integer.valueOf(result.getErrorCode())) {
                            txListener.process(tx);
                        } catch (HLAPIException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            private void serveTrunkListeners(HLAPIBlock block) {
                List<HLAPIBlock> blocks = new ArrayList<HLAPIBlock>(1);
                blocks.add(block);
                trunkListeners.forEach((blockListener) -> {
                    blockListener.trunkUpdate(blocks);
                });
            }

            private List<HLAPITransaction> convertToHLAPITxList(List<protos.Fabric.Transaction> transactionsList) {
                ArrayList<HLAPITransaction> result = new ArrayList<HLAPITransaction>(transactionsList.size());
                for(protos.Fabric.Transaction tx : transactionsList) {
                    ByteString invocationSpecBytes = tx.getPayload();
                    Chaincode.ChaincodeInvocationSpec invocationSpec;
                    try {
                        invocationSpec = Chaincode.ChaincodeInvocationSpec.parseFrom(invocationSpecBytes);
                        String transactionString = invocationSpec.getChaincodeSpec().getCtorMsg().getArgs(0);
                        byte[] transactionBytes = DatatypeConverter.parseBase64Binary(transactionString);
                        HLAPITransaction hlapitx = new HLAPITransaction(new Transaction(transactionBytes), BID.INVALID);
                        result.add(hlapitx);
                    } catch (InvalidProtocolBufferException e) {
                        result.add(new HLAPITransaction(new Transaction(new byte[0]), BID.INVALID));
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

        EventsOuterClass.Interest interestB = EventsOuterClass.Interest.newBuilder()
                .setEventType("block")
                .setResponseType(EventsOuterClass.Interest.ResponseType.PROTOBUF)
                .build();
        EventsOuterClass.Interest interestR = EventsOuterClass.Interest.newBuilder()
                .setEventType("rejection")
                .setResponseType(EventsOuterClass.Interest.ResponseType.PROTOBUF)
                .build();

        EventsOuterClass.Register register = EventsOuterClass.Register.newBuilder()
                .addEvents(0, interestB)
                .addEvents(1, interestR)
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
