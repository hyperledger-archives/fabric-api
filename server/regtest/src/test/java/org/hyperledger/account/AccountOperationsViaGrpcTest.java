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

import com.typesafe.config.ConfigFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.api.BCSAPI;
import org.hyperledger.common.*;
import org.hyperledger.test.GRPCRegtestRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.Security;

import static org.hyperledger.account.ActionWaiter.expected;
import static org.hyperledger.account.ActionWaiter.expectedOneOnEach;
import static org.junit.Assert.assertEquals;

public class AccountOperationsViaGrpcTest {
    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @ClassRule
    public static GRPCRegtestRule regtestRule = new GRPCRegtestRule(ConfigFactory.parseResources("test-config-grpc.json"));

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void sendToAddressTest() throws Exception {
        // setting up sender's account and receiver's read only accounts
        PrivateKey priv = PrivateKey.createNew();
        KeyListChain senderKeyChain = new KeyListChain(priv);
        BaseAccount senderAccount = new BaseAccount(senderKeyChain);

        Address receiverAddress = PrivateKey.createNew().getAddress();
        ReadOnlyAccount receiverAccount = new BaseReadOnlyAccount(new AddressListChain(receiverAddress));

        // getting the interface towards obs
        BCSAPI api = regtestRule.getBCSAPI();

        // starting chain height
        int startingChainHeight = api.getChainHeight();

        // accounts start listening to the changes in the accounts
        api.registerTransactionListener(senderAccount);
        api.registerTransactionListener(receiverAccount);

        // transaction to fund the sender's account with coinbase
        Transaction fundingTx = Transaction.create()
                .inputs(TransactionInput.create().source(TID.INVALID, -1).build())
                .outputs(TransactionOutput.create().payTo(priv.getAddress()).value(100000).build())
                .build();

        // execute the funding transaction
        ActionWaiter.execute(() -> api.sendTransaction(fundingTx), expected(senderAccount, 1));

        // expect that a new block was created with the funding tx
        int newChainHeight = api.getChainHeight();
        assertEquals("No block was created", startingChainHeight + 1, newChainHeight);
        // expect that sender's account now have the intended funding
        assertEquals("Incorrect balance", 100000, senderAccount.getCoins().getTotalSatoshis());

        // transaction to pay some of the funding forward
        TransactionFactory factory = new BaseTransactionFactory(senderAccount);
        Transaction nextTx = factory.propose(receiverAddress, 30000).sign(senderKeyChain);

        // execute the transaction
        ActionWaiter.execute(() -> api.sendTransaction(nextTx), expectedOneOnEach(senderAccount, receiverAccount));

        // expect the receiver got the transfer
        assertEquals("Incorrect balance", 30000, receiverAccount.getCoins().getTotalSatoshis());
        // expect the sender's balance was decreased
        // it is 5000 less because the transaction costed fee of 5000
        assertEquals("Incorrect balance", 65000, senderAccount.getCoins().getTotalSatoshis());
    }
}
