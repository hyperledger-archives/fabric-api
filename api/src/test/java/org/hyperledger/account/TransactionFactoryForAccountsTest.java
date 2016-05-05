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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.common.*;
import org.junit.Test;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TransactionFactoryForAccountsTest {
    private static final int AMOUNT = 40000;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private MasterPrivateKey senderMasterPrivateKey = MasterPrivateKey.createNew();
    private MasterPublicKey recipientMasterPublicKey = MasterPrivateKey.createNew().getMasterPublic();

    @Test
    public void chainTest() throws HyperLedgerException {
        Transaction tx =
                new TransactionFactoryForAccounts(createSender().createTransactionFactory())
                        .propose(PaymentOptions.common, new OutputProposal(AMOUNT, createRecipient(), false))
                        .getTransaction();

        assertEquals(calcExpectedTx().getID(), tx.getID());
    }

    private Transaction calcExpectedTx() throws HyperLedgerException {
        return createSender().createTransactionFactory()
                .propose(PaymentOptions.common,
                        TransactionOutput.create()
                                .payTo(createRecipient().getChain().getNextReceiverAddress())
                                .value(40000)
                                .build())
                .getTransaction();
    }

    private Account createSender() throws HyperLedgerException {
        MasterPrivateChain chain = new MasterPrivateChain(senderMasterPrivateKey);
        List<Coin> starterCoins = new ArrayList<>();
        starterCoins.add(new Coin(new Outpoint(TID.INVALID, 0),
                TransactionOutput.create().payTo(chain.getNextReceiverAddress()).value(100000).build()));
        return new BaseAccount(chain, starterCoins);
    }

    private ReadOnlyAccount createRecipient() throws HyperLedgerException {
        return new BaseReadOnlyAccount(new MasterPublicChain(recipientMasterPublicKey));
    }
}
