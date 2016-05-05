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
import org.hyperledger.account.PaymentOptions;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.TransactionOutput;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construct a swap transaction - where multiple parties can swap assets.
 *
 * Construction is performed in a deterministic way.  The order of outputs in the transaction is identical to the order
 * of calls to addLeg / addBurn.
 */
public class ColoredSwapTransactionBuilder {
    private final Map<ColoredTransactionFactory, Map<Color, Map<AddressChain, Long>>> senders = new LinkedHashMap<>();

    public ColoredSwapTransactionBuilder addLeg(ColoredReadOnlyAccount sender, Color color, long quantity, ColoredReadOnlyAccount recipient) {
        return addLeg(sender.createTransactionFactory(), color, quantity, recipient.getChain());
    }

    public ColoredSwapTransactionBuilder addLeg(ColoredTransactionFactory sender, Color color, long quantity, AddressChain recipient) {
        return addLeg(sender, color, quantity, recipient, false);
    }

    public ColoredSwapTransactionBuilder addBurn(ColoredReadOnlyAccount sender, Color color, long quantity) {
        return addLeg(sender.createTransactionFactory(), color, quantity, null, true);
    }

    public ColoredSwapTransactionBuilder addBurn(ColoredTransactionFactory sender, Color color, long quantity) {
        return addLeg(sender, color, quantity, null, true);
    }

    private ColoredSwapTransactionBuilder addLeg(ColoredTransactionFactory sender, Color color, long quantity, AddressChain recipient, boolean allowBurn) {
        if (recipient == null && !allowBurn) {
            throw new IllegalArgumentException("recipient is null. to perform a burn, use addBurn");
        }
        Map<Color, Map<AddressChain, Long>> legs = senders.get(sender);
        if (legs == null) {
            legs = new LinkedHashMap<>();
            senders.put(sender, legs);
        }
        Map<AddressChain, Long> allocation = legs.get(color);
        if (allocation == null) {
            allocation = new LinkedHashMap<>();
            legs.put(color, allocation);
        }
        allocation.put(recipient, allocation.get(recipient) == null ? quantity : allocation.get(recipient) + quantity);
        return this;
    }

    public ColoredTransactionProposal proposeSwap() throws HyperLedgerException {
        ColoredTransactionProposal merged = null;
        for (ColoredTransactionFactory sender : senders.keySet()) {
            List<ColoredTransactionOutput> outs = new ArrayList<>();
            for (Color c : senders.get(sender).keySet()) {
                for (Map.Entry<AddressChain, Long> e : senders.get(sender).get(c).entrySet()) {
                    TransactionOutput output;
                    if (e.getKey() == null) {
                        output = TransactionOutput.create().burn().build();
                    } else {
                        output = TransactionOutput.create().value(e.getValue()).payTo(e.getKey().getNextReceiverAddress()).build();
                    }
                    outs.add(new ColoredTransactionOutput(output, c, e.getValue()));
                }
            }
            ColoredTransactionProposal proposal = sender.proposeColored(PaymentOptions.common, outs);
            if (merged == null)
                merged = proposal;
            else
                merged = ColoredTransactionProposal.merge(merged, proposal);
        }
        return merged;
    }
}
