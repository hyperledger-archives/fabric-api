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
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.security.Security;
import java.util.Arrays;

import static org.hyperledger.account.TestHelper.assertCoinsAndValues;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class AccountOperationsTest {

    @Parameterized.Parameters
    public static Iterable<Object[]> data() throws HyperLedgerException {
        return Arrays.asList(new Object[][]{
                {new KeyListChain(), new KeyListChain()},
                {new MasterPrivateChain(MasterPrivateKey.createNew()), new MasterPrivateChain(MasterPrivateKey.createNew())}
        });
    }

    @Parameterized.Parameter(0)
    public KeyChain senderKeyChain;
    private Account senderAccount;

    @Parameterized.Parameter(1)
    public AddressChain receiverAddressChain;
    private ReadOnlyAccount receiverAccount;
    Address sendersReceiverAddress;

    AccountListener accountListener;

    private static Outpoint zeroOutpoint = new Outpoint(TID.INVALID, 0);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() throws HyperLedgerException {
        Security.addProvider(new BouncyCastleProvider());
        if (senderKeyChain instanceof KeyListChain) {
            KeyListChain keyChain = (KeyListChain) senderKeyChain;
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
        } else if (senderAccount instanceof MasterPrivateChain) {
            // nothing to initialize
        }
        senderAccount = new BaseAccount(senderKeyChain);
        sendersReceiverAddress = senderKeyChain.getNextReceiverAddress();

        if (receiverAddressChain instanceof KeyListChain) {
            KeyListChain keyChain = (KeyListChain) receiverAddressChain;
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
            keyChain.addKey(PrivateKey.createNew(true));
        } else if (receiverAddressChain instanceof MasterPrivateChain) {
            // nothing to initialize
        }
        receiverAccount = new BaseReadOnlyAccount(receiverAddressChain);

        // it cannot be a lambda, because Mockito cannot handle that
        AccountListener backingListener = new AccountListener() {
            @Override
            public void accountChanged(ReadOnlyAccount account, APITransaction t) {
            }
        };
        accountListener = Mockito.spy(backingListener);
        senderAccount.addAccountListener(accountListener);
        receiverAccount.addAccountListener(accountListener);
    }

    @Test
    public void paymentTest() throws HyperLedgerException {
        final APITransaction transaction = APITransaction.create()
                .inputs(
                        TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(200000L).build())
                .build();

        senderAccount.process(transaction);

        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(senderAccount, transaction);
        assertCoinsAndValues(senderAccount, null, null, 200000L, null, 200000L);

        final APITransaction transactionConfirmed = APITransaction.create()
                .inputs(TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(200000L).build())
                .blockID(BID.INVALID)
                .build();
        assertEquals(transactionConfirmed.getID(), transaction.getID());

        senderAccount.process(transactionConfirmed);

        Mockito.verify(accountListener, Mockito.times(2)).accountChanged(senderAccount, transaction);
        assertCoinsAndValues(senderAccount, null, null, null, 200000L, 200000L);

        BaseTransactionFactory transactionFactory = new BaseTransactionFactory(senderAccount);

        PaymentOptions options = PaymentOptions.fixedOutputOrder;
        Transaction tx = transactionFactory.propose(receiverAccount.getChain().getNextReceiverAddress(), 120000L, options).sign(senderKeyChain);
        assertEquals(120000L, tx.getOutput(0).getValue());
        assertEquals(75000L, tx.getOutput(1).getValue());

        APITransaction transaction2 = new APITransaction(tx, null);
        senderAccount.process(transaction2);
        receiverAccount.process(transaction2);

        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(senderAccount, transaction2);
        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(receiverAccount, transaction2);
        assertCoinsAndValues(senderAccount, null, 75000L, null, null, 75000L);
        assertCoinsAndValues(receiverAccount, null, null, 120000L, null, 120000L);
    }

    @Test
    public void paymentsWithDifferentOptionsTest() throws HyperLedgerException {
        final APITransaction transaction1 = APITransaction.create()
                .inputs(TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(200000L).build())
                .build();
        senderAccount.process(transaction1);

        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(senderAccount, transaction1);
        assertCoinsAndValues(senderAccount, null, null, 200000L, null, 200000L);

        final APITransaction transactionConfirmed = APITransaction.create()
                .inputs(TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(200000L).build())
                .blockID(BID.INVALID)
                .build();
        assertEquals(transactionConfirmed.getID(), transaction1.getID());
        senderAccount.process(transactionConfirmed);

        Mockito.verify(accountListener, Mockito.times(2)).accountChanged(senderAccount, transaction1);
        assertCoinsAndValues(senderAccount, null, null, null, 200000L, 200000L);

        BaseTransactionFactory transactionFactory = new BaseTransactionFactory(senderAccount);
        PaymentOptions options = PaymentOptions.receiverPaysFee;
        Transaction tx = transactionFactory.propose(receiverAccount.getChain().getNextReceiverAddress(), 80000L, options).sign(senderKeyChain);

        assertEquals(75000L, tx.getOutput(0).getValue());
        assertEquals(120000L, tx.getOutput(1).getValue());

        APITransaction transaction2 = new APITransaction(tx, null);
        senderAccount.process(transaction2);
        receiverAccount.process(transaction2);

        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(senderAccount, transaction2);
        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(receiverAccount, transaction2);
        assertCoinsAndValues(senderAccount, null, 120000L, null, null, 120000L);
        assertCoinsAndValues(receiverAccount, null, null, 75000L, null, 75000L);

        options = PaymentOptions.fixedOutputOrder;
        Transaction tx2 = transactionFactory.propose(receiverAccount.getChain().getNextReceiverAddress(), 20000L, options).sign(senderKeyChain);
        assertEquals(20000L, tx2.getOutput(0).getValue());

        APITransaction transaction3 = new APITransaction(tx2, null);
        senderAccount.process(transaction3);
        receiverAccount.process(transaction3);

        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(senderAccount, transaction3);
        Mockito.verify(accountListener, Mockito.times(1)).accountChanged(receiverAccount, transaction3);
        assertCoinsAndValues(senderAccount, null, 95000L, null, null, 95000L);
        assertCoinsAndValues(receiverAccount, null, null, 95000L, null, 95000L);

    }

    @Test
    public void mempoolConfirmUnconfirm() throws HyperLedgerException {

        final APITransaction transaction = APITransaction.create()
                .inputs(
                        TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(1L).build())
                .build();

        // it cannot be a lambda, because Mockito cannot handle that
        AccountListener backingListener = new AccountListener() {
            @Override
            public void accountChanged(ReadOnlyAccount account, APITransaction t) {
                assertEquals(t.getID(), transaction.getID());
            }
        };
        AccountListener listener = Mockito.spy(backingListener);
        senderAccount.addAccountListener(listener);

        senderAccount.process(transaction);

        Mockito.verify(listener, Mockito.times(1)).accountChanged(Matchers.any(), Matchers.any());
        assertCoinsAndValues(senderAccount, null, null, 1L, null, 1L);
        assertTrue(senderAccount.getTransactions().contains(transaction));
        assertTrue(senderAccount.getReceivingCoins().contains(transaction.getCoin(0)));

        final APITransaction transactionConfirmed = APITransaction.create()
                .inputs(TransactionInput.create().source(zeroOutpoint).build())
                .outputs(TransactionOutput.create().payTo(sendersReceiverAddress).value(1).build())
                .blockID(BID.INVALID)
                .build();
        assertEquals(transactionConfirmed.getID(), transaction.getID());

        senderAccount.process(transactionConfirmed);

        Mockito.verify(listener, Mockito.times(2)).accountChanged(Matchers.any(), Matchers.any());
        assertCoinsAndValues(senderAccount, null, null, null, 1L, 1L);
        assertTrue(senderAccount.getTransactions().contains(transaction));
        assertTrue(senderAccount.getConfirmedCoins().contains(transactionConfirmed.getCoin(0)));

        senderAccount.unconfirmed(transactionConfirmed);

        Mockito.verify(listener, Mockito.times(3)).accountChanged(Matchers.any(), Matchers.any());
        assertCoinsAndValues(senderAccount, null, null, 1L, null, 1L);
        assertTrue(senderAccount.getTransactions().contains(transaction));
        assertTrue(senderAccount.getReceivingCoins().contains(transactionConfirmed.getCoin(0)));

        senderAccount.doubleSpent(transactionConfirmed, transaction);
        Mockito.verify(listener, Mockito.times(4)).accountChanged(Matchers.any(), Matchers.any());
        assertCoinsAndValues(senderAccount, null, null, null, null, null);
        assertFalse(senderAccount.getTransactions().contains(transaction));
        assertFalse(senderAccount.getReceivingCoins().contains(transactionConfirmed.getCoin(0)));
    }

}
