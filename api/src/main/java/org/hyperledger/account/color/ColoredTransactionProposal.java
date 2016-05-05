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

import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.account.CoinBucket;
import org.hyperledger.account.TransactionProposal;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.Coin;
import org.hyperledger.common.Transaction;
import org.hyperledger.common.TransactionOutput;
import org.hyperledger.common.color.DigitalAssetAnnotation;

import java.util.ArrayList;
import java.util.List;

public class ColoredTransactionProposal extends TransactionProposal {
    public ColoredTransactionProposal(List<Coin> sources, List<TransactionOutput> sinks) {
        super(sources, sinks);
    }

    public ColoredTransactionProposal(CoinBucket coins, Transaction transaction) throws HyperLedgerException {
        super(coins, transaction);
    }

    public static ColoredTransactionProposal merge(ColoredTransactionProposal a, ColoredTransactionProposal b) throws HyperLedgerException {
        if (HyperLedgerSettings.getInstance().isNaEnabled()) {
            return mergeNativeAsset(a, b);
        } else {
            return mergeForeingAsset(a, b);
        }
    }

    public static ColoredTransactionProposal mergeForeingAsset(ColoredTransactionProposal a, ColoredTransactionProposal b) throws HyperLedgerException {
        List<TransactionOutput> sinks = new ArrayList<>();
        List<Coin> sources = new ArrayList<>();
        sinks.addAll(a.getSinks().subList(1, a.getSinks().size()));
        sources.addAll(a.getSources());
        sources.addAll(b.getSources());
        List<Long> colors = DigitalAssetAnnotation.getColors(a.getSink(0).getScript());
        sinks.addAll(b.getSinks().subList(1, b.getSinks().size()));
        colors.addAll(DigitalAssetAnnotation.getColors(b.getSink(0).getScript()));
        if (!colors.isEmpty())
            sinks.add(0, DigitalAssetAnnotation.indicateColors(colors));

        return new ColoredTransactionProposal(sources, sinks);

    }

    public static ColoredTransactionProposal mergeNativeAsset(ColoredTransactionProposal a, ColoredTransactionProposal b) throws HyperLedgerException {
        List<TransactionOutput> sinks = new ArrayList<>(a.getSinks().size() + b.getSinks().size());
        List<Coin> sources = new ArrayList<>(a.getSources().size() + b.getSources().size());
        sinks.addAll(a.getSinks());
        sinks.addAll(b.getSinks());
        sources.addAll(a.getSources());
        sources.addAll(b.getSources());
        return new ColoredTransactionProposal(sources, sinks);
    }
}
