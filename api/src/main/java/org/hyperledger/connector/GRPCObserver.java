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
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.common.BID;
import org.hyperledger.common.Transaction;
import protos.Chaincode;
import protos.EventsGrpc;
import protos.EventsOuterClass;
import protos.Fabric.Block;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GRPCObserver {
    private EventsGrpc.EventsStub es;
    private Set<TransactionListener> listeners = new HashSet<>();

    public GRPCObserver(Channel eventsChannel) {
        es = EventsGrpc.newStub(eventsChannel);
    }

    public void connect() {
        StreamObserver<EventsOuterClass.Event> receiver = new StreamObserver<EventsOuterClass.Event>() {
            @Override
            public void onNext(EventsOuterClass.Event openchainEvent) {
                listeners.forEach((listener) -> {
                    try {
                        if (openchainEvent.getEventCase() == EventsOuterClass.Event.EventCase.BLOCK) {
                            Block block = openchainEvent.getBlock();
                            processAll(listener, block.getTransactionsList());
                        }
                    } catch (HLAPIException | IOException e) {
                        e.printStackTrace();
                    }
                });
                System.out.println("new event: " + openchainEvent.toString());
            }

            private void processAll(TransactionListener listener, List<protos.Fabric.Transaction> transactionsList) throws HLAPIException, InvalidProtocolBufferException {
                for(protos.Fabric.Transaction tx : transactionsList) {
                    ByteString invocationSpecBytes = tx.getPayload();
                    Chaincode.ChaincodeInvocationSpec invocationSpec = Chaincode.ChaincodeInvocationSpec.parseFrom(invocationSpecBytes);
                    String transactionString = invocationSpec.getChaincodeSpec().getCtorMsg().getArgs(0);
                    byte[] transactionBytes = DatatypeConverter.parseBase64Binary(transactionString);
                    HLAPITransaction hlapitx = new HLAPITransaction(new Transaction(transactionBytes), BID.INVALID);
                    listener.process(hlapitx);
                }
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

    public void subscribe(TransactionListener l) {
        listeners.add(l);
    }

    public void unsubscribe(TransactionListener l) {
        listeners.remove(l);
    }
}
