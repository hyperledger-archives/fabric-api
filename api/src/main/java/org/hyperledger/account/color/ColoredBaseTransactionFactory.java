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
import org.hyperledger.account.PaymentOptions;
import org.hyperledger.account.ReadOnlyAccount;
import org.hyperledger.account.TransactionProposal;
import org.hyperledger.common.*;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;
import org.hyperledger.common.color.DigitalAssetAnnotation;
import org.hyperledger.common.color.NativeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ColoredBaseTransactionFactory implements ColoredTransactionFactory {
    private static final Logger log = LoggerFactory.getLogger(ColoredBaseTransactionFactory.class);

    private final ReadOnlyAccount account;
    private final WireFormatter wireFormatter;

    public ColoredBaseTransactionFactory(ReadOnlyAccount account) {
        this(account, HyperLedgerSettings.getInstance().getTxWireFormatter());
    }

    ColoredBaseTransactionFactory(ReadOnlyAccount account, WireFormatter wireFormatter) {
        this.account = account;
        this.wireFormatter = wireFormatter;
    }

    @Override
    public ReadOnlyAccount getAccount() {
        return account;
    }

    @Override
    public CoinBucket getSufficientSources(long amount, long fee) throws HyperLedgerException {
        return getSufficientSources(Color.BITCOIN, amount + fee);
    }

    @Override
    public ColoredCoinBucket getSufficientSources(Color color, long quantity) throws HyperLedgerException {
        List<Coin> candidates;
        if (account instanceof ColoredReadOnlyAccount) {
            ColoredCoinBucket coinBucket = ((ColoredReadOnlyAccount)account).getCoins(color);
            if (coinBucket == null) {
                throw new HyperLedgerException("No coin bucket for color [" + color + "]");
            }
            candidates = coinBucket.getCoins();
        } else {
            candidates = new ColoredCoinBucket(account.getConfirmedCoins()).getCoins(color).getCoins();
            candidates.sort(spendPreferenceOrder);
            List<Coin> change = new ColoredCoinBucket(account.getChangeCoins()).getCoins(color).getCoins();
            change.sort(spendPreferenceOrder);
            candidates.addAll(change);
            List<Coin> receiving = new ColoredCoinBucket(account.getReceivingCoins()).getCoins(color).getCoins();
            receiving.sort(spendPreferenceOrder);
            candidates.addAll(receiving);
        }

        List<Coin> result = new ArrayList<>();
        long sum = 0;
        for (Coin o : candidates) {
            sum += ((ColoredTransactionOutput) o.getOutput()).getQuantity();
            result.add(o);
            if (sum >= quantity) {
                return new ColoredCoinBucket(new CoinBucket(result));
            }
        }
        throw new HyperLedgerException("Insufficent sources for " + quantity + " " + color);
    }

    @Override
    public TransactionProposal propose(Address receiver, long amount) throws HyperLedgerException {
        return propose(PaymentOptions.common,
                new ColoredTransactionOutput(amount, receiver.getAddressScript(), Color.BITCOIN, amount));
    }

    @Override
    public TransactionProposal propose(Address receiver, long amount, PaymentOptions options) throws HyperLedgerException {
        return propose(options,
                new ColoredTransactionOutput(amount, receiver.getAddressScript(), Color.BITCOIN, amount));
    }

    @Override
    public TransactionProposal propose(PaymentOptions options, TransactionOutput... outputs) throws HyperLedgerException {
        List<ColoredTransactionOutput> outs = new ArrayList<>();
        for (TransactionOutput o : outputs) {
            outs.add(new ColoredTransactionOutput(o, Color.BITCOIN, o.getValue()));
        }
        return proposeColored(options, outs);
    }

    @Override
    public TransactionProposal propose(PaymentOptions options, List<TransactionOutput> outputs) throws HyperLedgerException {
        List<ColoredTransactionOutput> outs = new ArrayList<>();
        for (TransactionOutput o : outputs) {
            outs.add(new ColoredTransactionOutput(o, Color.BITCOIN, o.getValue()));
        }
        return proposeColored(options, outs);
    }

    @Override
    public ColoredTransactionProposal proposeColored(ColoredTransactionOutput receiver) throws HyperLedgerException {
        return proposeColored(PaymentOptions.common, receiver);
    }

    @Override
    public ColoredTransactionProposal proposeColored(PaymentOptions options, ColoredTransactionOutput... receiver) throws HyperLedgerException {
        List<ColoredTransactionOutput> outs = new ArrayList<>();
        for (ColoredTransactionOutput o : receiver) {
            outs.add(o);
        }
        return proposeColored(options, outs);
    }

    @Override
    public ColoredTransactionProposal proposeColored(PaymentOptions options, List<ColoredTransactionOutput> receiver) throws HyperLedgerException {
        ColoredTransactionProposal t;

        if (options.isCalculated()) {
            long estimate = 0;
            long txfee = options.isLowPriority() ? 0 : MINIMUM_FEE;
            do {
                txfee = Math.max(txfee, estimate);
                t = payFixed(
                        new PaymentOptions(txfee, PaymentOptions.FeeCalculation.FIXED, options.getSource(), options.getPriority(),
                                options.getOutputOrder(), options.getChange()),
                        receiver);
                estimate = estimateFee(t.getTransaction(), options.getPriority());
                if (txfee < estimate) {
                    log.trace("The transaction requires more network fees. Reassembling.");
                }
            } while (options.getCalculation() == PaymentOptions.FeeCalculation.CALCULATED && txfee < estimate);
        } else {
            t = payFixed(options, receiver);
        }
        return t;
    }

    @Override
    public ColoredTransactionProposal proposeColored(Address receiver, Color color, long quantity) throws HyperLedgerException {
        return proposeColored(PaymentOptions.common, ColoredTransactionOutput.create().payTo(receiver).color(color).quantity(quantity).build());
    }

    public static long estimateFee(Transaction t, PaymentOptions.Priority priority) {
        WireFormat.SizeWriter writer = new WireFormat.SizeWriter();
        try {
            HyperLedgerSettings.getInstance().getTxWireFormatter().toWire(t, writer);
        } catch (IOException e) {
        }
        int tsd = (writer.size() + 1000) / 1000;
        return Math.min(MAXIMUM_FEE, priority == PaymentOptions.Priority.LOW ? 0 : Math.max(tsd * (priority == PaymentOptions.Priority.NORMAL ? KB_FEE : MINIMUM_FEE), MINIMUM_FEE));
    }

    // TODO: additional color related checks
    public static ColoredTransactionProposal createProposal(List<Coin> sources, List<TransactionOutput> sinks, long fee) throws HyperLedgerException {
        if (fee < 0 || fee > MAXIMUM_FEE) {
            throw new HyperLedgerException("You unlikely want to do that");
        }

        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        long sumOut = 0;
        for (TransactionOutput o : sinks) {
            if (!o.getScript().isPrunable()) {
                if (o instanceof ColoredTransactionOutput) {
                    Color color = ((ColoredTransactionOutput) o).getColor();
                    if (color.isToken() && o.getValue() == 0)
                        throw new HyperLedgerException("Zero output");
                } else {
                    // check value only for non-colored transactions
                    // keep the order of the below value checks
                    if (o.getValue() == 0)
                        throw new HyperLedgerException("Zero output");
                    if (o.getValue() < DUST_LIMIT)
                        throw new HyperLedgerException("The transaction would be rejected as dust.");
                }
            }
            sumOut += o.getValue();
            outputs.add(o);
        }

        ArrayList<TransactionInput> inputs = new ArrayList<>();
        long sumInput = 0;
        for (Coin o : sources) {
            TransactionInput i = TransactionInput.create().source(o.getOutpoint()).build();
            sumInput += o.getOutput().getValue();

            inputs.add(i);
        }
        long remainder = sumInput - (sumOut + fee);
        if (remainder > MINIMUM_FEE || remainder < 0) {
            throw new HyperLedgerException("Sum of sinks (+fee) does not match sum of sources");
        }

        return new ColoredTransactionProposal(sources, outputs);
    }

    private ColoredTransactionProposal payFixedForeign(PaymentOptions options, List<ColoredTransactionOutput> receiver) throws HyperLedgerException {
        Map<Color, Long> needByColor = new LinkedHashMap<>();
        Map<Color, List<ScriptUnit>> receiverByColor = new LinkedHashMap<>();
        for (int i = 0; i < receiver.size(); ++i) {
            Color c = receiver.get(i).getColor();
            if (!needByColor.containsKey(c)) {
                needByColor.put(c, receiver.get(i).getQuantity());
            } else {
                needByColor.put(c, receiver.get(i).getQuantity() + needByColor.get(c));
            }
            List<ScriptUnit> targets = receiverByColor.get(c);
            if (targets == null) {
                targets = new ArrayList<>();
                receiverByColor.put(c, targets);
            }
            targets.add(new ScriptUnit(receiver.get(i).getScript(), receiver.get(i).getQuantity()));
        }

        List<Color> colors = new ArrayList<>();
        colors.addAll(needByColor.keySet());
        Collections.sort(colors, BitcoinComparator.INSTANCE); // bitcoin last, others stable

        LinkedList<TransactionOutput> sinks = new LinkedList<>();
        List<Long> colorQuantities = new ArrayList<>();
        List<Coin> sources = new ArrayList<>();
        boolean hasColor = false;
        long feePerColor = (long) Math.ceil((double) options.getFee() / colors.size());
        for (Color c : colors) {
            ColoredCoinBucket s = getSufficientSources(c, needByColor.get(c));
            sources.addAll(s.getCoins());

            long availableCarrier = s.getTotalSatoshis();
            long availableColor = s.getTotalQuantity();

            double carrierByUnit = (double) (availableCarrier - feePerColor) / availableColor;

            hasColor |= !c.isToken();

            for (ScriptUnit au : receiverByColor.get(c)) {
                long carrier = (long) (carrierByUnit * au.quantity);
                availableCarrier -= carrier;
                availableColor -= au.quantity;
                if (!c.isToken()) {
                    colorQuantities.add(au.quantity);
                    sinks.add(new ColoredTransactionOutput(carrier, au.script, c, au.quantity));
                } else {
                    sinks.add(new TransactionOutput(carrier, au.script));
                }
            }
            if (availableColor > 0 && !c.isToken()) {
                if (availableCarrier - feePerColor < DUST_LIMIT)
                    throw new HyperLedgerException("Not enough carrier token for color output " + c);
                colorQuantities.add(availableColor);
                sinks.add(
                        new ColoredTransactionOutput(
                                new TransactionOutput(availableCarrier - feePerColor,
                                        account.getChain().getNextChangeAddress().getAddressScript()),
                                c, availableColor));
            } else if (availableCarrier - feePerColor > DUST_LIMIT) {
                sinks.add(new TransactionOutput(availableCarrier - feePerColor, account.getChain().getNextChangeAddress().getAddressScript()));
            }
        }

        if (hasColor)
            sinks.addFirst(DigitalAssetAnnotation.indicateColors(colorQuantities));

        return createProposal(sources, sinks, options.getFee());
    }

    private ColoredTransactionProposal payFixedNative(PaymentOptions options, List<ColoredTransactionOutput> receivers) throws HyperLedgerException {
        List<Coin> sources = new ArrayList<>();
        LinkedList<TransactionOutput> sinks = new LinkedList<>();
        Map<Color, Long> needByColor = new LinkedHashMap<>();
        Map<Color, List<ScriptUnit>> receiverByColor = new LinkedHashMap<>();

        for (ColoredTransactionOutput receiver : receivers) {
            Color c = receiver.getColor();

            if (!c.isToken()) {
                NativeAsset asset = (NativeAsset) c;
                if (asset.isBeingDefined()) {
                    if (asset.getIndex() != sources.size())
                        throw new IllegalArgumentException("new asset definition indexes must start at zero and increment contiguously");
                    sources.add(new Coin(Outpoint.NULL, new TransactionOutput(0, new Script())));
                    sinks.add(receiver);
                    continue;
                }
            }
            if (!needByColor.containsKey(c)) {
                needByColor.put(c, receiver.getQuantity());
            } else {
                needByColor.put(c, receiver.getQuantity() + needByColor.get(c));
            }
            List<ScriptUnit> targets = receiverByColor.get(c);
            if (targets == null) {
                targets = new ArrayList<>();
                receiverByColor.put(c, targets);
            }
            targets.add(new ScriptUnit(receiver.getScript(), c.isToken() ? receiver.getValue() : receiver.getQuantity()));
        }

        boolean nativeAssetWithNoBtc = false;
        if (!needByColor.containsKey(Color.BITCOIN)) {
            if (HyperLedgerSettings.getInstance().isNaEnabled()) {
                nativeAssetWithNoBtc = true;
            } else {
                needByColor.put(Color.BITCOIN, 0L);
            }
        }

        List<Color> colors = new ArrayList<>();
        colors.addAll(needByColor.keySet());
        Collections.sort(colors, BitcoinComparator.INSTANCE); // bitcoin last, others stable

        for (Color c : colors) {
            ColoredCoinBucket s = getSufficientSources(c, needByColor.get(c));
            sources.addAll(s.getCoins());

            long availableColor = s.getTotalQuantity();
            long availableTokens = s.getTotalSatoshis();

            List<ScriptUnit> scripts = receiverByColor.get(c);
            if (scripts == null && !c.equals(Color.BITCOIN))
                throw new IllegalStateException("no receivers for color");
            if (scripts != null) {
                for (ScriptUnit au : scripts) {
                    if (!c.isToken()) {
                        sinks.add(new ColoredTransactionOutput(0, au.script, c, au.quantity));
                        availableColor -= au.quantity;
                    } else {
                        sinks.add(new TransactionOutput(au.quantity, au.script));
                        availableTokens -= au.quantity;
                    }
                }
            }
            if (availableColor > 0 && !c.isToken()) {
                sinks.add(
                        new ColoredTransactionOutput(
                                new TransactionOutput(0,
                                        account.getChain().getNextChangeAddress().getAddressScript()),
                                c, availableColor));
            } else if (availableTokens - options.getFee() > DUST_LIMIT && c.isToken()) {
                sinks.add(new TransactionOutput(availableTokens - options.getFee(), account.getChain().getNextChangeAddress().getAddressScript()));
            }
        }

        return createProposal(sources, sinks, nativeAssetWithNoBtc ? 0 : options.getFee());
    }

    private ColoredTransactionProposal payFixed(PaymentOptions options, List<ColoredTransactionOutput> receiver) throws HyperLedgerException {
        for (ColoredTransactionOutput output : receiver) {
            Color color = output.getColor();
            if (!color.isToken()) {
                if (color.isNative() != wireFormatter.isNativeAssets())
                    throw new IllegalArgumentException("receiver native/foreign color type doesn't match setting");
            }
        }

        if (wireFormatter.isNativeAssets())
            return payFixedNative(options, receiver);
        else
            return payFixedForeign(options, receiver);
    }

    private static class ScriptUnit {
        private final Script script;
        private final long quantity;

        public ScriptUnit(Script script, long quantity) {
            this.script = script;
            this.quantity = quantity;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColoredBaseTransactionFactory that = (ColoredBaseTransactionFactory) o;

        return account.equals(that.account);

    }

    @Override
    public int hashCode() {
        return account.hashCode();
    }
}
