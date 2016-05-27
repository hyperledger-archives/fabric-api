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

import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.common.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Test
    public void getNonExistingTransaction() throws HLAPIException {
        HLAPITransaction res = client.getTransaction(TID.INVALID);
        assertTrue(res == null);
    }

    @Test
    public void sendTransaction() throws HLAPIException, InterruptedException {
        Transaction tx = new Transaction(new byte[100]);

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
        Transaction tx1 = new Transaction(new byte[100]);
        Transaction tx2 = new Transaction(new byte[90]);
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

        };
        TestListener listener = new TestListener();
        client.registerTransactionListener(listener);

        client.sendTransaction(tx1);
        client.sendTransaction(tx2);

        byte expectedTxCount = 2;
        byte counter = 3;
        while(counter != 0 && expectedTxCount != listener.getProcessedTxCount())
        {
          Thread.sleep(1000);
          counter--;
        }
        client.removeTransactionListener(listener);
        assertEquals(expectedTxCount, listener.getProcessedTxCount());
    }

}
