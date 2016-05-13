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

import org.hyperledger.account.BaseAccount;
import org.hyperledger.account.BaseTransactionFactory;
import org.hyperledger.account.KeyListChain;
import org.hyperledger.account.TransactionFactory;
import org.hyperledger.api.APITransaction;
import org.hyperledger.api.BCSAPIException;
import org.hyperledger.common.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class GRPCClientTest {
    private static final Logger log = LoggerFactory.getLogger(GRPCClientTest.class);

    GRPCClient client;

    @Before
    public void setUp() {
        client = new GRPCClient("localhost", 30303, 31315);
    }


    @Test
    public void testGetBlockHeight() throws BCSAPIException {
        int height = client.getChainHeight();

        log.debug("testGetBlockHeight height=" + height);

        assertTrue(height > 0);
    }

    @Test
    public void getNonExistingTransaction() throws BCSAPIException {
        APITransaction res = client.getTransaction(TID.INVALID);
        assertTrue(res == null);
    }

    @Test
    public void sendTransaction() throws HyperLedgerException, BCSAPIException, InterruptedException {
        PrivateKey priv = PrivateKey.createNew();

        Transaction tx = Transaction.create()
                .inputs(TransactionInput.create().source(TID.INVALID, -1).build())
                .outputs(TransactionOutput.create().payTo(priv.getAddress()).build())
                .build();

        int originalHeight = client.getChainHeight();
        client.sendTransaction(tx);

        Thread.sleep(1500);

        APITransaction res = client.getTransaction(tx.getID());
        assertEquals(tx, res);
        int newHeight = client.getChainHeight();
        assertTrue(newHeight == originalHeight + 1);
    }


}
