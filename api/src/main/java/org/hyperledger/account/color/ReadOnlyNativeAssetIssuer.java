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

import org.hyperledger.account.AddressChain;
import org.hyperledger.account.AddressListChain;
import org.hyperledger.account.BaseReadOnlyAccount;
import org.hyperledger.account.TransactionProposal;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.NativeAsset;

import java.util.Arrays;
import java.util.List;

public class ReadOnlyNativeAssetIssuer extends BaseReadOnlyAccount {

    public ReadOnlyNativeAssetIssuer(Address issueTo) {
        super(new AddressListChain(issueTo));
    }

    protected ReadOnlyNativeAssetIssuer(AddressChain chain) {
        super(chain);
    }

    private Address issuerAddress() throws HyperLedgerException {
        return getChain().getNextChangeAddress();
    }

    public TransactionProposal proposeNativeAssetIssuance(Address receiver, long quantity, long fee) throws HyperLedgerException {
        Coin nullInput = new Coin(Outpoint.NULL, TransactionOutput.create().build());
        Coin bitcoinInput = null;
        for(Coin c: getCoins().getCoins()) {
            if (c.getOutput().getValue() >= fee) {
                bitcoinInput = c;
            }
        }
        if (bitcoinInput == null) {
            throw new HyperLedgerException("Insufficient sources for bitcoin input (" + fee + ")!");
        }
        List<Coin> inputs = Arrays.asList(nullInput, bitcoinInput);

        TransactionOutput coloredCoin = ColoredTransactionOutput.create()
                .quantity(quantity)
                .color(new NativeAsset(0))
                .payTo(receiver)
                .build();
        TransactionOutput bitcoinOutput = TransactionOutput.create()
                .value(bitcoinInput.getOutput().getValue() - fee)
                .payTo(issuerAddress())
                .build();
        List<TransactionOutput> outputs = Arrays.asList(coloredCoin, bitcoinOutput);

        return new TransactionProposal(inputs, outputs);
    }

    /**
     * In the issuance transaction, the ColoredTransactionOutput has invalid
     * color information, it contains TID.INVALID in the NativeAsset. You can
     * use this function to query the color of the issuance transaction. Later,
     * if we use issuance by scriptPubKey, it will be no longer needed.
     */
    public static Color getIssuanceColor(Transaction transaction) {
        boolean hasTwoInputs = transaction.getInputs().size() == 2;
        boolean hasTwoOutputs = transaction.getOutputs().size() == 2;
        boolean firstInputNull = transaction.getInputs().get(0).getSource() == Outpoint.NULL;
        if (hasTwoInputs && hasTwoOutputs && firstInputNull) {
            return new NativeAsset(transaction.getID(), 0);
        }
        return Color.BITCOIN;
    }
}
