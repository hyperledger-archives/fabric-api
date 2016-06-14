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

package org.hyperledger.api.connector;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.block.BID;
import org.hyperledger.transaction.Transaction;
import protos.Chaincode;
import protos.EventsGrpc;
import protos.EventsOuterClass;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.HashSet;
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
                        ByteString invocationSpecBytes = openchainEvent.getBlock().getTransactions(0).getPayload();
                        Chaincode.ChaincodeInvocationSpec invocationSpec = Chaincode.ChaincodeInvocationSpec.parseFrom(invocationSpecBytes);
                        String transactionString = invocationSpec.getChaincodeSpec().getCtorMsg().getArgs(0);
                        byte[] transactionBytes = DatatypeConverter.parseBase64Binary(transactionString);
                        HLAPITransaction tx = new HLAPITransaction(new Transaction(transactionBytes), BID.INVALID);
                        listener.process(tx);
                    } catch (HLAPIException | IOException e) {
                        e.printStackTrace();
                    }
                });
                System.out.println("new event: " + openchainEvent.toString());
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
