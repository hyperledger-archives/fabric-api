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
package org.hyperledger.account.color;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.account.*;
import org.hyperledger.api.APITransaction;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ForeignAsset;
import org.junit.Before;
import org.junit.Test;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoundingFailingTest {
    @Before
    public void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void newTest() throws HyperLedgerException {
        Random random = new Random(1);
        for (int j = 0; j < 100; ++j) {
            MasterPrivateKey fundedAccountMasterPrivateKey = MasterPrivateKey.createNew();
            Account fundingAccount = new BaseAccount(new KeyListChain(fundedAccountMasterPrivateKey.getMaster()));

            UIAddress fundingAddress = new UIAddress(UIAddress.Network.TEST, fundedAccountMasterPrivateKey.getMaster().getAddress());
            Transaction funding = Transaction.create().inputs(TransactionInput.create().source(TID.INVALID, 0).build())
                    .outputs(
                            TransactionOutput.create().payTo(fundingAddress.getAddress()).value(BTCToSatoshi(2000)).build()
                    ).build();

            fundingAccount.confirmed(new APITransaction(funding, BID.INVALID));

            MasterPrivateKey otherMasterPrivateKey = MasterPrivateKey.createNew();
            ColoredReadOnlyAccount otherAccount = new ColoredBaseReadOnlyAccount(new MasterPublicChain(otherMasterPrivateKey.getMasterPublic()));

            List<ReadOnlyColorIssuer> issuanceAccounts = new ArrayList<>();
            List<TransactionOutput> outputs = new ArrayList<>();
            int bound = random.ints(2, 16).findFirst().getAsInt();
            for (int i = 0; i < bound; ++i) {
                Address address = otherAccount.getChain().getNextReceiverAddress();
                issuanceAccounts.add(new ReadOnlyColorIssuer(address));
                outputs.add(TransactionOutput.create().payTo(address).value(BTCToSatoshi(1)).build());
            }

            Transaction t = fundingAccount.createTransactionFactory().propose(PaymentOptions.common, outputs).sign(fundingAccount);
            for (ReadOnlyColorIssuer issuanceAccount : issuanceAccounts) {
                issuanceAccount.confirmed(new APITransaction(t, BID.INVALID));
            }

            Address issuanceLanding = fundingAccount.getChain().getNextReceiverAddress();
            ColoredReadOnlyAccount a = new ColoredBaseReadOnlyAccount(new AddressListChain(issuanceLanding));
            for (ReadOnlyColorIssuer account : issuanceAccounts) {
                TransactionProposal proposal = account
                        .issueTokenTransaction(
                                issuanceLanding,
                                random.ints(2, 1000).findFirst().getAsInt(),
                                random.ints(1000000, 50000000).findFirst().getAsInt(),
                                BaseTransactionFactory.MINIMUM_FEE
                        );
                a.confirmed(new APITransaction(proposal.sign(fundingAccount), BID.INVALID));
            }
            ColoredSwapTransactionBuilder swapTransactionBuilder = new ColoredSwapTransactionBuilder();
            for (ReadOnlyColorIssuer issuanceAccount : issuanceAccounts) {
                Color color = new ForeignAsset(issuanceAccount.getChain().getNextReceiverAddress());
                swapTransactionBuilder.addLeg(a, color, random.ints(1, (int) a.getCoins(color).getTotalQuantity()).findFirst().getAsInt(), otherAccount);
            }
            swapTransactionBuilder.proposeSwap();
        }
    }

    private static long BTCToSatoshi(long btc) {
        return (btc * 100000000);
    }
}
