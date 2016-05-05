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

import org.hyperledger.account.*;
import org.hyperledger.common.Address;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.PublicKey;
import org.hyperledger.common.TransactionOutput;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.DigitalAssetAnnotation;
import org.hyperledger.common.color.ForeignAsset;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ReadOnlyColorIssuer extends BaseReadOnlyAccount {

    protected ReadOnlyColorIssuer(AddressChain chain) {
        super(chain);
    }

    public ReadOnlyColorIssuer(Address address) {
        super(new AddressListChain(address));
    }

    public ReadOnlyColorIssuer(PublicKey rootKey, byte[] offset) throws HyperLedgerException {
        super(new AddressListChain(rootKey.offsetKey(new BigInteger(1, offset)).getAddress()));
    }

    public Address getFundingAddress() {
        try {
            return getChain().getNextChangeAddress();
        } catch (HyperLedgerException e) {
            return null; // can't happen
        }
    }

    public Color getColor() {
        return new ForeignAsset(getFundingAddress());
    }

    public TransactionProposal issueTokenTransaction(Address receiver, long quantity, long carrier, long fee) throws HyperLedgerException {
        CoinBucket sources = createTransactionFactory().getSufficientSources(carrier, fee);

        long availableCarrier = sources.getTotalSatoshis();

        TransactionOutput tokens = new ColoredTransactionOutput(carrier, receiver.getAddressScript(),
                getColor(), quantity);
        TransactionOutput annotation = new TransactionOutput(0L, DigitalAssetAnnotation.indicateColors(quantity));

        List<TransactionOutput> sinks = new ArrayList<>();
        sinks.add(tokens);
        sinks.add(annotation);
        if (availableCarrier - carrier - fee > TransactionFactory.DUST_LIMIT) {
            TransactionOutput change = new TransactionOutput(availableCarrier - carrier - fee,
                    getFundingAddress().getAddressScript());
            sinks.add(change);
        }

        return BaseTransactionFactory.createTransaction(sources.getCoins(), sinks, fee);
    }
}
