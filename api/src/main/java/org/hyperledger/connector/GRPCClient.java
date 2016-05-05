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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.api.*;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protos.Chaincode;
import protos.Chaincode.ChaincodeID;
import protos.Chaincode.ChaincodeInput;
import protos.Chaincode.ChaincodeInvocationSpec;
import protos.Chaincode.ChaincodeSpec;
import protos.DevopsGrpc;
import protos.DevopsGrpc.DevopsBlockingStub;

import protos.Openchain;
import protos.OpenchainGrpc;
import protos.OpenchainGrpc.OpenchainBlockingStub;
import protos.Api.BlockCount;

import javax.xml.bind.DatatypeConverter;

public class GRPCClient implements BCSAPI {
    private static final Logger log = LoggerFactory.getLogger(GRPCClient.class);

    final String chaincodeName = "utxo";


    private DevopsBlockingStub dbs;
    private OpenchainBlockingStub obs;

    private final GRPCObserver observer;

    public GRPCClient(String host, int port, int observerPort) {
        log.debug("Trying to connect to GRPC host:port={}:{}, host:observerPort={}:{}, ", host, port, observerPort);
        ManagedChannel channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
        ManagedChannel observerChannel = NettyChannelBuilder.forAddress(host, observerPort).negotiationType(NegotiationType.PLAINTEXT).build();
        dbs = DevopsGrpc.newBlockingStub(channel);
        obs = OpenchainGrpc.newBlockingStub(channel);
        observer = new GRPCObserver(observerChannel);
        observer.connect();
    }

    public void invoke(String chaincodeName, byte[] transaction) {
        String encodedTransaction = Base64.getEncoder().encodeToString(transaction);

        ChaincodeID.Builder chaincodeId = ChaincodeID.newBuilder();
        chaincodeId.setName(chaincodeName);

        ChaincodeInput.Builder chaincodeInput = ChaincodeInput.newBuilder();
        chaincodeInput.setFunction("execute");
        chaincodeInput.addArgs(encodedTransaction);

        ChaincodeSpec.Builder chaincodeSpec = ChaincodeSpec.newBuilder();
        chaincodeSpec.setChaincodeID(chaincodeId);
        chaincodeSpec.setCtorMsg(chaincodeInput);

        ChaincodeInvocationSpec.Builder chaincodeInvocationSpec = ChaincodeInvocationSpec.newBuilder();
        chaincodeInvocationSpec.setChaincodeSpec(chaincodeSpec);

        dbs.invoke(chaincodeInvocationSpec.build());
    }

    private ByteString query(String functionName, Iterable<String> args) {
        Chaincode.ChaincodeID chainCodeId = Chaincode.ChaincodeID.newBuilder()
                .setName("utxo")
                .build();

        Chaincode.ChaincodeInput chainCodeInput = Chaincode.ChaincodeInput.newBuilder()
                .setFunction(functionName)
                .addAllArgs(args)
                .build();

        Chaincode.ChaincodeSpec chaincodeSpec = Chaincode.ChaincodeSpec.newBuilder()
                .setChaincodeID(chainCodeId)
                .setCtorMsg(chainCodeInput)
                .build();

        Chaincode.ChaincodeInvocationSpec chaincodeInvocationSpec = Chaincode.ChaincodeInvocationSpec.newBuilder()
                .setChaincodeSpec(chaincodeSpec)
                .build();

        Openchain.Response response = dbs.query(chaincodeInvocationSpec);

        return response.getMsg();
    }

    @Override
    public String getClientVersion() throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServerVersion() throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long ping(long nonce) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlertListener(AlertListener listener) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlertListener(AlertListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getChainHeight() throws BCSAPIException {
        BlockCount height = obs.getBlockCount(com.google.protobuf.Empty.getDefaultInstance());
        return (int) height.getCount();
    }

    @Override
    public APIBlockIdList getBlockIds(BID blockId, int count) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public APIHeader getBlockHeader(BID hash) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public APIBlock getBlock(BID hash) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public APITransaction getTransaction(TID hash) throws BCSAPIException {
        try {
            String hexedHash = ByteUtils.toHex(hash.toByteArray());
            ByteString result = query("getTran", Collections.singletonList(hexedHash));
            byte[] resultStr = result.toByteArray();
            if (resultStr.length == 0) return null;
            return new APITransaction(new WireFormatter().fromWire(resultStr), BID.INVALID);
        } catch (IOException e) {
            throw new BCSAPIException (e);
        }
    }

    @Override
    public List<APITransaction> getInputTransactions(TID txId) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendTransaction(Transaction transaction) throws BCSAPIException {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            WireFormat.Writer w = new WireFormat.Writer(os);
            new WireFormatter().toWire(transaction, w);
            invoke(chaincodeName, os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerRejectListener(RejectListener rejectListener) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRejectListener(RejectListener rejectListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public APIHeader mine(Address address) throws BCSAPIException {
        log.info("mine discarded for {}", address);
        return null;
    }

    @Override
    public void sendBlock(Block block) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerTransactionListener(TransactionListener listener) throws BCSAPIException {
        observer.subscribe(listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        observer.unsubscribe(listener);
    }

    @Override
    public void registerTrunkListener(TrunkListener listener) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTrunkListener(TrunkListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scanTransactionsForAddresses(Set<Address> addresses, TransactionListener listener)
            throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scanTransactions(MasterPublicKey master, int lookAhead, TransactionListener listener)
            throws BCSAPIException {
        // TODO we will need this
        throw new UnsupportedOperationException();
    }

    @Override
    public void catchUp(List<BID> inventory, int limit, boolean headers, TrunkListener listener)
            throws BCSAPIException {
        // TODO we will need this
        throw new UnsupportedOperationException();
    }

    @Override
    public void spendingTransactions(List<TID> tids, TransactionListener listener) throws BCSAPIException {
        throw new UnsupportedOperationException();
    }

}
