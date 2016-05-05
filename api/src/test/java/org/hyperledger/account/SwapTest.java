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

import org.hyperledger.account.color.*;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SwapTest {
    @Test
    public void test() throws HyperLedgerException {
        PrivateKey aKey = PrivateKey.createNew();
        PrivateKey bKey = PrivateKey.createNew();

        ColorIssuer aIssuer = new ColorIssuer(aKey);
        ColorIssuer bIssuer = new ColorIssuer(bKey);
        Color aColor = aIssuer.getColor();
        Color bColor = bIssuer.getColor();


        // simulate funding to issuerKeys
        aIssuer.confirmed(new APITransaction(Transaction.create().outputs(
                TransactionOutput.create().payTo(aIssuer.getFundingAddress()).value(1000000).build()).build(), BID.INVALID));
        bIssuer.confirmed(new APITransaction(Transaction.create().outputs(
                TransactionOutput.create().payTo(bIssuer.getFundingAddress()).value(1000000).build()).build(), BID.INVALID));

        PrivateKey aHolderKey = PrivateKey.createNew();
        PrivateKey bHolderKey = PrivateKey.createNew();


        Address aHolderAddess = aHolderKey.getAddress();
        Address bHolderAddress = bHolderKey.getAddress();

        ColoredAccount aHolder = new ColoredBaseAccount(new KeyListChain(aHolderKey));
        ColoredAccount bHolder = new ColoredBaseAccount(new KeyListChain(bHolderKey));

        Transaction issueA = aIssuer.issueTokens(aHolderAddess, 1000, 1000000 - TransactionFactory.MINIMUM_FEE, TransactionFactory.MINIMUM_FEE);
        Transaction issueB = bIssuer.issueTokens(bHolderAddress, 1000, 1000000 - TransactionFactory.MINIMUM_FEE, TransactionFactory.MINIMUM_FEE);

        // simulates sending the issue to server
        aHolder.confirmed(new APITransaction(issueA, BID.INVALID));
        bHolder.confirmed(new APITransaction(issueB, BID.INVALID));

        assertEquals(1000, aHolder.getCoins(aIssuer.getColor()).getTotalQuantity());
        assertEquals(1000, bHolder.getCoins(bIssuer.getColor()).getTotalQuantity());


        ColoredSwapTransactionBuilder swapTransactionFactory = new ColoredSwapTransactionBuilder()
                .addLeg(aHolder.createTransactionFactory(), aColor, 30, bHolder.getChain())
                .addLeg(bHolder.createTransactionFactory(), bColor, 50, aHolder.getChain());
        ColoredTransactionProposal swap = swapTransactionFactory.proposeSwap();


        Transaction halfSigned = swap.sign(aHolder.getChain());
        TransactionProposal reparsed = new TransactionProposal(swap.getSourceCoins(), halfSigned);
        Transaction fullySigned = reparsed.sign(bHolder.getChain());

        // simulates sending fully signed to server
        aHolder.confirmed(new APITransaction(fullySigned, BID.INVALID));
        bHolder.confirmed(new APITransaction(fullySigned, BID.INVALID));

        // check results
        assertEquals(30, bHolder.getCoins(aIssuer.getColor()).getTotalQuantity());
        assertEquals(1000 - 30, aHolder.getCoins(aIssuer.getColor()).getTotalQuantity());
        assertEquals(50, aHolder.getCoins(bIssuer.getColor()).getTotalQuantity());
        assertEquals(1000 - 50, bHolder.getCoins(bIssuer.getColor()).getTotalQuantity());
    }
}
