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
import org.hyperledger.account.color.*;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.junit.Test;

import java.security.Security;

import static org.junit.Assert.assertEquals;

public class ColoredTransactionMultiOutputsTest {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void transactionTest() throws HyperLedgerException {
        PrivateKey key1 = PrivateKey.createNew();
        PrivateKey key2 = PrivateKey.createNew();
        PrivateKey key3 = PrivateKey.createNew();
        PrivateKey key4 = PrivateKey.createNew();
        PrivateKey key5 = PrivateKey.createNew();
        PrivateKey key6 = PrivateKey.createNew();

        ColorIssuer colorIssuer1 = new ColorIssuer(key1);
        ColorIssuer colorIssuer2 = new ColorIssuer(key2);
        ColorIssuer colorIssuer3 = new ColorIssuer(key3);
        ColorIssuer colorIssuer4 = new ColorIssuer(key4);
        ColorIssuer colorIssuer5 = new ColorIssuer(key5);
        ColorIssuer colorIssuer6 = new ColorIssuer(key6);

        Color color1 = colorIssuer1.getColor();
        Color color2 = colorIssuer2.getColor();
        Color color3 = colorIssuer3.getColor();
        Color color4 = colorIssuer4.getColor();
        Color color5 = colorIssuer5.getColor();
        Color color6 = colorIssuer6.getColor();

        Transaction funding = Transaction.create().inputs(TransactionInput.create().source(TID.INVALID, 0).build())
                .outputs(
                        TransactionOutput.create().payTo(key1.getAddress()).value(1000000).build(),
                        TransactionOutput.create().payTo(key2.getAddress()).value(1000000).build(),
                        TransactionOutput.create().payTo(key3.getAddress()).value(1000000).build(),
                        TransactionOutput.create().payTo(key4.getAddress()).value(1000000).build(),
                        TransactionOutput.create().payTo(key5.getAddress()).value(1000000).build(),
                        TransactionOutput.create().payTo(key6.getAddress()).value(1000000).build()
                ).build();

        colorIssuer1.confirmed(new APITransaction(funding, BID.INVALID));
        colorIssuer2.confirmed(new APITransaction(funding, BID.INVALID));
        colorIssuer3.confirmed(new APITransaction(funding, BID.INVALID));
        colorIssuer4.confirmed(new APITransaction(funding, BID.INVALID));
        colorIssuer5.confirmed(new APITransaction(funding, BID.INVALID));
        colorIssuer6.confirmed(new APITransaction(funding, BID.INVALID));

        assertEquals(1000000, colorIssuer1.getCoins().getTotalSatoshis());
        assertEquals(1000000, colorIssuer2.getCoins().getTotalSatoshis());
        assertEquals(1000000, colorIssuer3.getCoins().getTotalSatoshis());
        assertEquals(1000000, colorIssuer4.getCoins().getTotalSatoshis());
        assertEquals(1000000, colorIssuer5.getCoins().getTotalSatoshis());
        assertEquals(1000000, colorIssuer6.getCoins().getTotalSatoshis());

        PrivateKey receiver1 = PrivateKey.createNew();
        PrivateKey receiver2 = PrivateKey.createNew();
        PrivateKey receiver3 = PrivateKey.createNew();
        PrivateKey receiver4 = PrivateKey.createNew();
        PrivateKey receiver5 = PrivateKey.createNew();
        PrivateKey receiver6 = PrivateKey.createNew();

        Transaction issuing1 = colorIssuer1.issueTokens(receiver1.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);
        Transaction issuing2 = colorIssuer2.issueTokens(receiver2.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);
        Transaction issuing3 = colorIssuer3.issueTokens(receiver3.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);
        Transaction issuing4 = colorIssuer4.issueTokens(receiver4.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);
        Transaction issuing5 = colorIssuer5.issueTokens(receiver5.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);
        Transaction issuing6 = colorIssuer6.issueTokens(receiver6.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);

        ColoredAccount holder1 = new ColoredBaseAccount(new KeyListChain(receiver1, receiver2));
        ColoredAccount holder2 = new ColoredBaseAccount(new KeyListChain(receiver3, receiver4));
        ColoredAccount holder3 = new ColoredBaseAccount(new KeyListChain(receiver5, receiver6));

        // simulates sending the issue to server
        holder1.confirmed(new APITransaction(issuing1, BID.INVALID));
        holder1.confirmed(new APITransaction(issuing2, BID.INVALID));
        holder2.confirmed(new APITransaction(issuing3, BID.INVALID));
        holder2.confirmed(new APITransaction(issuing4, BID.INVALID));
        holder3.confirmed(new APITransaction(issuing5, BID.INVALID));
        holder3.confirmed(new APITransaction(issuing6, BID.INVALID));

        assertEquals(100, holder1.getCoins(color1).getTotalQuantity());
        assertEquals(100, holder1.getCoins(color2).getTotalQuantity());
        assertEquals(100, holder2.getCoins(color3).getTotalQuantity());
        assertEquals(100, holder2.getCoins(color4).getTotalQuantity());
        assertEquals(100, holder3.getCoins(color5).getTotalQuantity());
        assertEquals(100, holder3.getCoins(color6).getTotalQuantity());

        assertEquals(100000, holder1.getConfirmedCoins().getTotalSatoshis());
        assertEquals(100000, holder2.getConfirmedCoins().getTotalSatoshis());
        assertEquals(100000, holder3.getConfirmedCoins().getTotalSatoshis());

        ColoredSwapTransactionBuilder swapTransactionFactory = new ColoredSwapTransactionBuilder();
        swapTransactionFactory.addLeg(holder1, color1, 30, holder2)
                .addLeg(holder1, color2, 40, holder2)
                .addLeg(holder1, color2, 10, holder3)
                .addLeg(holder2, color3, 50, holder1)
                .addLeg(holder2, color4, 60, holder1)
                .addLeg(holder2, color4, 10, holder3)
                .addLeg(holder3, color5, 70, holder1)
                .addLeg(holder3, color6, 80, holder2);

        ColoredTransactionProposal swap = swapTransactionFactory.proposeSwap();

        swap.partialSign(holder1);
        swap.partialSign(holder2);
        Transaction t = swap.sign(holder3);

        holder1.confirmed(new APITransaction(t, BID.INVALID));
        holder2.confirmed(new APITransaction(t, BID.INVALID));
        holder3.confirmed(new APITransaction(t, BID.INVALID));

        assertEquals(30, holder2.getCoins(color1).getTotalQuantity());
        assertEquals(40, holder2.getCoins(color2).getTotalQuantity());
        assertEquals(50, holder1.getCoins(color3).getTotalQuantity());
        assertEquals(60, holder1.getCoins(color4).getTotalQuantity());
        assertEquals(70, holder1.getCoins(color5).getTotalQuantity());


        assertEquals(70, holder1.getCoins(color1).getTotalQuantity());
        assertEquals(50, holder1.getCoins(color2).getTotalQuantity());
        assertEquals(50, holder2.getCoins(color3).getTotalQuantity());
        assertEquals(30, holder2.getCoins(color4).getTotalQuantity());
        assertEquals(80, holder2.getCoins(color6).getTotalQuantity());

        assertEquals(10, holder3.getCoins(color2).getTotalQuantity());
        assertEquals(10, holder3.getCoins(color4).getTotalQuantity());


    }
}
