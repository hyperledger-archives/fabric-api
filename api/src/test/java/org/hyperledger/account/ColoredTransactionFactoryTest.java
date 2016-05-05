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
import org.hyperledger.account.color.ColorIssuer;
import org.hyperledger.account.color.ColoredBaseTransactionFactory;
import org.hyperledger.account.color.ColoredTransactionFactory;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.DigitalAssetAnnotation;
import org.junit.Test;

import java.security.Security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ColoredTransactionFactoryTest {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void issueTest() throws HyperLedgerException {
        PrivateKey key = PrivateKey.createNew();

        ColorIssuer colorIssuer = new ColorIssuer(key);
        Color color = colorIssuer.getColor();

        Transaction funding = Transaction.create().inputs(TransactionInput.create().source(TID.INVALID, 0).build())
                .outputs(TransactionOutput.create().payTo(key.getAddress()).value(1000000).build()).build();

        colorIssuer.confirmed(new APITransaction(funding, BID.INVALID));

        PrivateKey receiver = PrivateKey.createNew();
        BaseAccount receiverAccount = new BaseAccount(new KeyListChain(receiver));
        Transaction issuing = colorIssuer.issueTokens(receiver.getAddress(), 100, 50000, TransactionFactory.MINIMUM_FEE);

        assertEquals(new ColoredTransactionOutput(
                new TransactionOutput(50000, receiver.getAddress().getAddressScript()), color, 100), issuing.getOutput(0));

        assertTrue(issuing.getOutput(1).getScript().equals(DigitalAssetAnnotation.indicateColors(100)));

        assertEquals(new TransactionOutput(1000000 - 50000 - TransactionFactory.MINIMUM_FEE,
                key.getAddress().getAddressScript()), issuing.getOutput(2));

        // this is done by the server
        APITransaction apit = new APITransaction(issuing, BID.INVALID);
        receiverAccount.confirmed(apit);

        assertEquals(100, receiverAccount.getConfirmedCoins().getColoredCoins().getCoins(color).getTotalQuantity());

        PrivateKey target = PrivateKey.createNew(true);
        ColoredTransactionFactory transactionFactory = new ColoredBaseTransactionFactory(receiverAccount);
        Transaction transfer = transactionFactory.proposeColored(target.getAddress(), color, 10).sign(receiverAccount.getChain());
        assertTrue(transfer.getInput(0).getSource().equals(new Outpoint(apit.getID(), 0)));
        assertTrue(transfer.getInputs().size() == 1);
        assertTrue(transfer.getOutput(0).getScript().equals(DigitalAssetAnnotation.indicateColors(10, 90)));
        assertTrue(transfer.getOutputs().size() == 3);
        assertEquals(new ColoredTransactionOutput(new TransactionOutput(4500, target.getAddress().getAddressScript()),
                color, 10), transfer.getOutput(1));
        assertEquals(new ColoredTransactionOutput(new TransactionOutput(40500, receiver.getAddress().getAddressScript()),
                color, 90), transfer.getOutput(2));
    }
}
