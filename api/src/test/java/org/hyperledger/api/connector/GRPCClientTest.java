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

import io.grpc.StatusRuntimeException;
import org.hamcrest.CoreMatchers;
import org.hyperledger.api.*;
import org.hyperledger.common.Hash;
import org.hyperledger.transaction.TID;
import org.hyperledger.transaction.Transaction;
import org.hyperledger.transaction.TransactionBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

public class GRPCClientTest {
    private static final Logger log = LoggerFactory.getLogger(GRPCClientTest.class);

    GRPCClient client;

    @Before
    public void setUp() {
        client = new GRPCClient("localhost", 30303, 31315);
    }

    @Test
    public void testGetBlockHeight() throws HLAPIException {
        int height = client.getChainHeight();

        log.debug("testGetBlockHeight height=" + height);

        assertTrue(height > 0);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getNonExistingTransaction() throws HLAPIException {
        thrown.expect(StatusRuntimeException.class);
        thrown.expectMessage(CoreMatchers.containsString("ledger: resource not found"));
        client.getTransaction(TID.INVALID);
    }

    @Test
    public void sendTransaction() throws HLAPIException, InterruptedException {
        Transaction tx = new TransactionBuilder().payload(new byte[100]).build();

        int originalHeight = client.getChainHeight();
        client.sendTransaction(tx);

        Thread.sleep(1500);
        HLAPITransaction res = client.getTransaction(tx.getID());
        assertEquals(tx.getID(), res.getID());
        assertArrayEquals(tx.getPayload(), res.getPayload());
        int newHeight = client.getChainHeight();
        assertTrue(newHeight == originalHeight + 1);
    }

    @Test
    public void transactionListener() throws HLAPIException, InterruptedException {
        Transaction tx1 = new TransactionBuilder().payload(new byte[100]).build();
        Transaction tx2 = new TransactionBuilder().payload(new byte[90]).build();
        Transaction tx3 = new TransactionBuilder().payload(new byte[95]).build();
        class TestListener implements TransactionListener {
            private byte processedTxCount = 0;

            public byte getProcessedTxCount() {
                return processedTxCount;
            }

            @Override
            public void process(HLAPITransaction t) throws HLAPIException {
                processedTxCount++;
                System.out.println(t.getID().toString());
            }

        }
        TestListener listener = new TestListener();
        client.registerTransactionListener(listener);

        client.sendTransaction(tx1);
        client.sendTransaction(tx2);
        client.invoke(client.chaincodeName, "some-fake-function-name", tx3.toByteArray());

        byte expectedTxCount = 2;
        byte counter = 3;
        while (counter != 0 && expectedTxCount != listener.getProcessedTxCount()) {
            Thread.sleep(1000);
            counter--;
        }
        client.removeTransactionListener(listener);
        assertEquals(expectedTxCount, listener.getProcessedTxCount());
    }

    @Test
    public void trunkListener() throws HLAPIException, InterruptedException {
        Transaction tx1 = new TransactionBuilder().payload(new byte[100]).build();
        Transaction tx2 = new TransactionBuilder().payload(new byte[90]).build();
        class TestListener implements TrunkListener {
            private byte processedBlockCount = 0;
            private byte processedTxCount = 0;

            public byte getProcessedTxCount() {
                return processedTxCount;
            }

            public byte getProcessedBlockCount() {
                return processedBlockCount;
            }

            @Override
            public void trunkUpdate(List<HLAPIBlock> added) {
                processedBlockCount += added.size();
                for (HLAPIBlock b : added) {
                    processedTxCount += b.getTransactions().size();
                }
            }
        }
        TestListener listener = new TestListener();
        client.registerTrunkListener(listener);
        client.invoke(client.chaincodeName, "some-fake-function-name", tx1.toByteArray());
        client.sendTransaction(tx2);

        byte expectedBlockCount = 1;
        byte expectedTxCount = 2;
        byte counter = 3;
        while (counter != 0 && expectedTxCount != listener.getProcessedTxCount()) {
            Thread.sleep(1000);
            counter--;
        }
        client.removeTrunkListener(listener);
        assertEquals(expectedBlockCount, listener.getProcessedBlockCount());
        assertEquals(expectedTxCount, listener.getProcessedTxCount());
    }

    @Test
    public void rejectListener() throws HLAPIException, InterruptedException {
        Transaction tx1 = new TransactionBuilder().payload(new byte[100]).build();
        Transaction tx2 = new TransactionBuilder().payload(new byte[90]).build();
        class TestListener implements RejectListener {
            private byte processedRejectionCount = 0;

            public byte getProcessedTxCount() {
                return processedRejectionCount;
            }

            @Override
            public void rejected(String command, Hash hash, String reason, int rejectionCode) {
                processedRejectionCount++;
            }
        }
        TestListener listener = new TestListener();
        client.registerRejectListener(listener);

        client.invoke(client.chaincodeName, "some-fake-function-name", tx1.toByteArray());
        client.sendTransaction(tx2);

        byte expectedTxCount = 1;
        byte counter = 3;
        while (counter != 0 && expectedTxCount != listener.getProcessedTxCount()) {
            Thread.sleep(1000);
            counter--;
        }
        client.removeRejectListener(listener);
        assertEquals(expectedTxCount, listener.getProcessedTxCount());
    }

}
