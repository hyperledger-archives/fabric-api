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
import org.hyperledger.api.APITransaction;
import org.hyperledger.api.BCSAPIMessage;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.common.BID;
import org.hyperledger.common.ByteUtils;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.WireFormatter;
import protos.Chaincode;
import protos.OpenchainEventsGrpc;
import protos.Events;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class GRPCObserver {
    private OpenchainEventsGrpc.OpenchainEventsStub es;
    private Set<TransactionListener> listeners = new HashSet<>();

    public GRPCObserver(Channel eventsChannel) {
        es = OpenchainEventsGrpc.newStub(eventsChannel);
    }

    public void connect() {
        StreamObserver<Events.OpenchainEvent> receiver = new StreamObserver<Events.OpenchainEvent>() {
            @Override
            public void onNext(Events.OpenchainEvent openchainEvent) {
                listeners.forEach((listener) -> {
                    try {
                        ByteString invocationSpecBytes = openchainEvent.getBlock().getTransactions(0).getPayload();
                        Chaincode.ChaincodeInvocationSpec invocationSpec = Chaincode.ChaincodeInvocationSpec.parseFrom(invocationSpecBytes);
                        String transactionString = invocationSpec.getChaincodeSpec().getCtorMsg().getArgs(0);
                        byte[] transactionBytes = DatatypeConverter.parseBase64Binary(transactionString);
                        APITransaction tx = new APITransaction(new WireFormatter().fromWire(transactionBytes), BID.INVALID);
                        listener.process(tx);
                    } catch (HyperLedgerException | IOException e) {
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

        StreamObserver<Events.OpenchainEvent> sender = es.chat(receiver);

        Events.Interest interest = Events.Interest.newBuilder()
                .setEventType("block")
                .setResponseType(Events.Interest.ResponseType.PROTOBUF)
                .build();

        Events.Register register = Events.Register.newBuilder()
                .addEvents(0, interest)
                .build();

        Events.OpenchainEvent registerEvent = Events.OpenchainEvent.newBuilder()
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
