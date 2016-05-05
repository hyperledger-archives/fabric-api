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
package org.hyperledger.account;

import org.hyperledger.api.APIBlock;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class BaseReadOnlyAccountTest {

    private ConfirmationManager manager;
    private PrivateKey key;
    private KeyListChain chain;
    private BaseReadOnlyAccount account;
    private Address address;
    private APIBlock b1;
    private APIBlock b2;
    private APIBlock b3;
    private APIBlock b4;

    @Before
    public void setUp() throws Exception {
        manager = new ConfirmationManager();
        key = PrivateKey.createNew();
        address = key.getAddress();
        chain = new KeyListChain(key);
        account = new BaseReadOnlyAccount(chain);
        b1 = makeBlock(BID.INVALID, 1, asList(APITransaction.create().build()));
        b2 = makeBlock(b1.getID(), 2, asList(APITransaction.create().build()));
        b3 = makeBlock(b2.getID(), 3, asList(APITransaction.create().build()));
        b4 = makeBlock(b3.getID(), 4, asList(APITransaction.create().build()));
        // t1 not related to our account
        APITransaction t1 = APITransaction.create().blockID(b1.getID())
                .outputs(
                        TransactionOutput.create().payTo(PrivateKey.createNew().getAddress()).value(10000).build()
                ).build();
        // t2 has three outputs to us
        APITransaction t2 = APITransaction.create().blockID(b2.getID())
                .inputs(TransactionInput.create().source(t1.getID(), 0).build())
                .outputs(
                        TransactionOutput.create().payTo(address).value(1000).build(),
                        TransactionOutput.create().payTo(address).value(2000).build(),
                        TransactionOutput.create().payTo(address).value(4000).build()
                ).build();
        APITransaction t3 = APITransaction.create().blockID(b3.getID())
                .inputs(TransactionInput.create().source(t2.getID(), 0).build())
                .build();
        APITransaction t4 = APITransaction.create().blockID(b4.getID())
                .inputs(TransactionInput.create().source(t2.getID(), 1).build())
                .build();
        manager.trunkUpdate(asList(b1, b2, b3, b4));
        account.process(t1);
        account.process(t2);
        account.process(t3);
        account.process(t4);
    }

    private APIBlock makeBlock(BID prev, int height, List<APITransaction> txs) {
        return new APIBlock.Builder()
                .createTime(0)
                .difficultyTarget(0)
                .version(4)
                .height(height)
                .nonce(0)
                .transactions(txs)
                .previousHash(prev)
                .build();
    }

    @Test
    public void testSnapshot() throws Exception {
        // Only the 4000 output is unspent
        assertEquals(4000L, account.getConfirmedCoins().getTotalSatoshis());
        // Just before b4, the 2000 output is also unspent
        ReadOnlyAccount snap = account.snapshot(b4.getID(), manager);
        assertEquals(6000L, snap.getConfirmedCoins().getTotalSatoshis());
        // Just before b2 no transactions
        ReadOnlyAccount snap1 = account.snapshot(b2.getID(), manager);
        assertEquals(0L, snap1.getConfirmedCoins().getTotalSatoshis());
    }

    @Test
    public void testSpendUnconfirmed() throws Exception {
        APITransaction unconfirmed = APITransaction.create()
                .outputs(
                        TransactionOutput.create().payTo(address).value(11111).build()
                ).build();
        APITransaction confirmed = APITransaction.create().blockID(b4.getID())
                .outputs(
                        TransactionOutput.create().payTo(address).value(11111).build()
                ).build();
        Coin coin = unconfirmed.getCoin(0);

        account.process(unconfirmed);
        assertTrue(account.getCoins().contains(coin));
        APITransaction spend = APITransaction.create()
                .inputs(TransactionInput.create().source(coin.getOutpoint()).build())
                .build();
        account.process(spend);
        assertFalse(account.getCoins().contains(coin));
        account.process(confirmed);
        assertFalse(account.getCoins().contains(coin));
    }
}
