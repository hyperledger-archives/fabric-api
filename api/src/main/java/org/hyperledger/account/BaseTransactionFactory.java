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

import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Create transaction proposals.  The order of outputs is preserved. */
public class BaseTransactionFactory implements TransactionFactory {
    private static final Logger log = LoggerFactory.getLogger(BaseTransactionFactory.class);

    private static final SecureRandom random = new SecureRandom();

    private final ReadOnlyAccount account;

    @Override
    public ReadOnlyAccount getAccount() {
        return account;
    }

    public BaseTransactionFactory(ReadOnlyAccount account) {
        this.account = account;
    }

    public static TransactionProposal createTransaction(List<Coin> sources, List<TransactionOutput> sinks, long fee) throws HyperLedgerException {
        if (fee < 0 || fee > MAXIMUM_FEE) {
            throw new HyperLedgerException("You unlikely want to do that");
        }

        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        long sumOut = 0;
        for (TransactionOutput o : sinks) {
            if (!o.getScript().isPrunable() && o.getValue() < DUST_LIMIT)
                throw new HyperLedgerException("The transaction would be rejected as dust.");
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
        if (sumInput != (sumOut + fee)) {
            throw new HyperLedgerException("Sum of sinks (+fee) does not match sum of sources");
        }

        return new TransactionProposal(sources, outputs);
    }

    @Override
    public TransactionProposal propose(Address receiver, long amount) throws HyperLedgerException {
        return propose(PaymentOptions.common, TransactionOutput.create().payTo(receiver).value(amount).build());
    }

    @Override
    public TransactionProposal propose(Address receiver, long amount, PaymentOptions options) throws HyperLedgerException {
        return propose(options, TransactionOutput.create().payTo(receiver).value(amount).build());
    }

    @Override
    public TransactionProposal propose(PaymentOptions options, TransactionOutput... outputs) throws HyperLedgerException {
        ArrayList<TransactionOutput> outs = new ArrayList<>();
        Collections.addAll(outs, outputs);
        return propose(options, outs);
    }

    @Override
    public CoinBucket getSufficientSources(long amount, long fee) throws HyperLedgerException {
        List<Coin> result = new ArrayList<>();

        List<Coin> candidates = filter(account.getConfirmedCoins().getCoins());
        candidates.sort(spendPreferenceOrder);
        for (Coin c : candidates) {
            result.add(c);
            if (total(result) >= amount + fee)
                return new CoinBucket(result);
        }

        candidates = account.getChangeCoins().getCoins();
        candidates.sort(spendPreferenceOrder);
        for (Coin c : candidates) {
            result.add(c);
            if (total(result) >= amount + fee)
                return new CoinBucket(result);
        }

        candidates = account.getReceivingCoins().getCoins();
        candidates.sort(spendPreferenceOrder);
        for (Coin c : candidates) {
            result.add(c);
            if (total(result) >= amount + fee)
                return new CoinBucket(result);
        }

        // even confirmed and change combined was not enough
        throw new HyperLedgerException("Insufficient sources for " + amount + " + " + fee);
    }

    private long total(List<Coin> coins) {
        long s = 0;
        for (Coin c : coins) {
            s += c.getOutput().getValue();
        }
        return s;
    }


    private static long[] splitChange(long change, int n) {
        if (n == 1 || change <= (n * MINIMUM_FEE)) {
            return new long[]{change};
        } else {
            long[] changes = new long[n];
            boolean dust = false;

            do {
                double[] proportions = new double[n];
                double s = 0;
                for (int i = 0; i < n; ++i) {
                    s += proportions[i] = Math.exp(1 - random.nextDouble());
                }
                long cs = 0;
                for (int i = 0; i < n; ++i) {
                    cs += changes[i] = ((long) Math.floor(proportions[i] / s * change / MINIMUM_FEE)) * MINIMUM_FEE;
                }
                changes[0] += change - cs;
                for (long c : changes) {
                    if (c <= DUST_LIMIT) {
                        dust = true;
                    }
                }
            } while (dust);
            return changes;
        }
    }

    private TransactionProposal payFixed(PaymentOptions options, List<TransactionOutput> receiver) throws HyperLedgerException {
        long amount = 0;
        for (TransactionOutput a : receiver) {
            amount += a.getValue();
        }
        if (log.isTraceEnabled()) {
            if (options.isPaidBySender()) {
                log.trace("pay {} + {}", amount, options);
            } else {
                log.trace("pay {}", amount);
            }
        }
        List<Coin> sources = getSufficientSources(amount, options.isPaidBySender() ? options.getFee() : 0).getCoins();
        if (sources == null) {
            throw new HyperLedgerException("Insufficient funds to pay " + amount + " " + options);
        }
        long in = 0;
        for (Coin o : sources) {
            if (log.isTraceEnabled()) {
                log.trace("using input {} {}", o.getOutpoint(), o.getOutput().getValue());
            }
            in += o.getOutput().getValue();
        }

        List<TransactionOutput> sinks = new ArrayList<>();
        sinks.addAll(receiver);

        long txfee = options.getFee();
        if (!options.isPaidBySender()) {
            long feeCollected = 0;
            while (!sinks.isEmpty() && feeCollected < txfee) {
                TransactionOutput last = sinks.get(sinks.size() - 1);
                long feeAvaialable = Math.min(last.getValue(), options.getFee() - feeCollected);
                if (feeAvaialable == last.getValue()) {
                    sinks.remove(sinks.size() - 1);
                } else {
                    sinks.set(sinks.size() - 1, new TransactionOutput(last.getValue() - feeAvaialable, last.getScript()));
                }
                feeCollected += feeAvaialable;
            }
            if (feeCollected < txfee) {
                throw new HyperLedgerException("Can not cover fees by reducing outputs");
            }
            if (sinks.isEmpty()) {
                throw new HyperLedgerException("No output left after paying fees");
            }
        }
        if (((in - amount) - (options.isPaidBySender() ? options.getFee() : 0)) > DUST_LIMIT) {
            for (long change : splitChange(in - amount - (options.isPaidBySender() ? options.getFee() : 0), Math.max(1, options.getChange()))) {
                Address changeAddress = account.getChain().getNextChangeAddress();
                TransactionOutput changeOutput = new TransactionOutput(change, changeAddress.getAddressScript());
                if (log.isTraceEnabled()) {
                    log.trace("change to {} {}", changeAddress, changeOutput.getValue());
                }
                sinks.add(changeOutput);
            }
        } else {
            if (options.isPaidBySender()) {
                txfee = in - amount;
            }
        }
        if (options.isShuffled()) {
            Collections.shuffle(sinks);
        }
        return createTransaction(sources, sinks, txfee);
    }

    @Override
    public TransactionProposal propose(PaymentOptions options, List<TransactionOutput> receiver) throws HyperLedgerException {
        TransactionProposal t;

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

    public static long estimateFee(Transaction t, PaymentOptions.Priority priority) {
        WireFormat.SizeWriter writer = new WireFormat.SizeWriter();
        try {
            HyperLedgerSettings.getInstance().getTxWireFormatter().toWire(t, writer);
        } catch (IOException e) {
        }
        int tsd = (writer.size() + 1000) / 1000;
        return Math.min(MAXIMUM_FEE, priority == PaymentOptions.Priority.LOW ? MINIMUM_FEE : Math.max(tsd * (priority == PaymentOptions.Priority.NORMAL ? KB_FEE : MINIMUM_FEE), MINIMUM_FEE));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseTransactionFactory that = (BaseTransactionFactory) o;

        return account.equals(that.account);

    }

    @Override
    public int hashCode() {
        return account.hashCode();
    }

    protected List<Coin> filter(List<Coin> coins) {
        return coins;
    }
}
