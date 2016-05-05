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

import org.hyperledger.api.APITransaction;
import org.hyperledger.api.BCSAPI;
import org.hyperledger.api.BCSAPIException;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BaseReadOnlyAccount implements ReadOnlyAccount {
    private static final Logger log = LoggerFactory.getLogger(BaseReadOnlyAccount.class);

    private final Set<AccountListener> accountListener = new HashSet<>();
    private final AddressChain addressChain;
    private final Map<TID, APITransaction> transactions = new LinkedHashMap<>();

    public BaseReadOnlyAccount(AddressChain addressChain) {
        this.addressChain = addressChain;
    }

    public BaseReadOnlyAccount(AddressChain addressChain, List<Coin> receiving) {
        this.receiving.addAll(receiving);
        this.addressChain = addressChain;
    }

    private final CoinBucket confirmed = new CoinBucket();
    private final CoinBucket change = new CoinBucket();
    private final CoinBucket receiving = new CoinBucket();
    private final CoinBucket sending = new CoinBucket();

    public synchronized void reset() {
        confirmed.clear();
        change.clear();
        receiving.clear();
        sending.clear();
        transactions.clear();
    }

    @Override
    public synchronized List<APITransaction> getTransactions() {
        List<APITransaction> ts = new ArrayList<>();
        ts.addAll(transactions.values());
        return ts;
    }

    private boolean updateWithConfirmedTransaction(APITransaction t) {
        boolean spending = processInputs(t);
        boolean modified = processConfirmedOutputs(t, spending);
        if (modified || spending) {
            transactions.put(t.getID(), t);
        }
        return modified;
    }

    private boolean processConfirmedOutputs(APITransaction t, boolean spending) {
        boolean modified = spending;
        boolean reprocessing = transactions.containsKey(t.getID());
        for (Coin coin : t.getCoins()) {
            Outpoint p = coin.getOutpoint();
            TransactionOutput o = coin.getOutput();
            TransactionOutput removed = removeOutput(p);
            // If we have seen this transaction, and already spent the output, don't re-add
            if (reprocessing && removed == null)
                continue;

            boolean added = false;
            if (addressChain.getRelevantAddresses().contains(o.getOutputAddress())) {
                added = confirmed.add(coin);
                modified |= added;
                if (log.isTraceEnabled()) {
                    log.trace("Confirmed {} [{}] ({}) {}", t.getID(), p.getOutputIndex(), o.getOutputAddress(), o.getValue());
                }
            } else if (spending) {
                sending.add(coin);
                if (log.isTraceEnabled()) {
                    log.trace("Sending {} [{}] ({}) {}", t.getID(), p.getOutputIndex(), o.getOutputAddress(), o.getValue());
                }
            }
            if (added)
                onCoinAdded(coin);
        }
        return modified;
    }

    protected void onCoinAdded(Coin coin) {
    }

    private void removeOutput(APITransaction t) {
        for (Coin c : t.getCoins()) {
            removeOutput(c.getOutpoint());
        }
    }

    private TransactionOutput removeOutput(Outpoint p) {
        TransactionOutput out;
        out = confirmed.remove(p);
        if (out == null) {
            out = change.remove(p);
        }
        if (out == null) {
            out = receiving.remove(p);
        }
        if (out == null) {
            out = sending.remove(p);
        }
        if (out != null) {
            onCoinRemoved(out, p);
            if (log.isTraceEnabled()) {
                log.trace("Remove {} [{}] ({}) {}", p.getTransactionId(), p.getOutputIndex(), out.getOutputAddress(), out.getValue());
            }
        }
        return out;
    }

    protected void onCoinRemoved(TransactionOutput out, Outpoint outpoint) {
    }

    private boolean processInputs(Transaction t) {
        boolean spending = false;
        for (TransactionInput input : t.getInputs()) {
            TransactionOutput spend;
            Outpoint outpoint = new Outpoint(input.getSourceTransactionID(), input.getOutputIndex());
            spend = confirmed.remove(outpoint);
            if (spend != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Spend settled output {} [{}] {}", input.getSourceTransactionID(), input.getOutputIndex(), spend.getValue());
                }
            } else {
                spend = change.remove(outpoint);
                if (spend != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Spend change output {} [{}] {}", input.getSourceTransactionID(), input.getOutputIndex(), spend.getValue());
                    }
                } else {
                    spend = receiving.remove(outpoint);
                    if (spend != null) {
                        if (log.isTraceEnabled()) {
                            log.trace("Spend receiving output {} [{}] {}", input.getSourceTransactionID(), input.getOutputIndex(), spend.getValue());
                        }
                    }
                }
            }
            if (spend != null)
                onCoinRemoved(spend, outpoint);
            spending |= spend != null;
        }
        return spending;
    }

    @Override
    public synchronized void process(APITransaction t) {
        boolean modified;
        if (t.getBlockID() != null) {
            modified = updateWithConfirmedTransaction(t);
        } else {
            modified = updateWithMempoolTransaction(t);
        }
        if (modified) {
            notifyListener(t);
        }
    }

    private boolean updateWithMempoolTransaction(APITransaction t) {
        boolean spending = processInputs(t);
        boolean modified = processMempoolOutputs(t, spending);
        if (modified || spending) {
            transactions.put(t.getID(), t);
        }
        return modified;
    }

    private boolean processMempoolOutputs(APITransaction t, boolean spending) {
        boolean modified = spending;
        boolean reprocessing = transactions.containsKey(t.getID());
        for (Coin coin : t.getCoins()) {
            Outpoint p = coin.getOutpoint();
            TransactionOutput o = coin.getOutput();
            TransactionOutput removed = removeOutput(p);
            // If we have seen this transaction, and already spent the output, don't re-add
            if (reprocessing && removed == null)
                continue;

            boolean added = false;
            if (addressChain.getRelevantAddresses().contains(o.getOutputAddress())) {
                if (spending) {
                    added = change.add(coin);
                    if (log.isTraceEnabled()) {
                        log.trace("Change {} [{}] ({}) {}", t.getID(), p.getOutputIndex(), o.getOutputAddress(), o.getValue());
                    }
                } else {
                    added = receiving.add(coin);
                    modified |= added;
                    if (log.isTraceEnabled()) {
                        log.trace("Receiving {} [{}] ({}) {}", t.getID(), p.getOutputIndex(), o.getOutputAddress(), o.getValue());
                    }
                }
            }
            if (added)
                onCoinAdded(coin);
        }
        return modified;
    }

    @Override
    public synchronized void rejected(String command, Hash hash, String reason, int rejectionCode) {
        APITransaction revert = null;
        synchronized (this) {
            if (command.equals("tx") && transactions.containsKey(new TID(hash))) {
                revert = transactions.get(new TID(hash));

                removeOutput(revert);

                for (TransactionInput input : revert.getInputs()) {
                    APITransaction prev = transactions.get(input.getSourceTransactionID());
                    boolean spend = false;
                    for (TransactionInput pin : prev.getInputs()) {
                        if (transactions.containsKey(pin.getSourceTransactionID())) {
                            spend = true;
                            break;
                        }
                    }
                    boolean added;
                    Coin coin = prev.getCoin(input.getOutputIndex());
                    if (spend) {
                        added = change.add(coin);
                    } else {
                        added = receiving.add(coin);
                    }
                    if (added)
                        onCoinAdded(coin);
                }

                transactions.remove(new TID(hash));
            }
        }
        if (revert != null) {
            notifyListener(revert);
        }
    }

    @Override
    public synchronized boolean isKnownTransaction(APITransaction t) {
        return transactions.containsKey(t.getID());
    }

    @Override
    public synchronized void addAccountListener(AccountListener listener) {
        accountListener.add(listener);
    }

    @Override
    public synchronized void removeAccountListener(AccountListener listener) {
        accountListener.remove(listener);
    }

    private synchronized void notifyListener(APITransaction t) {
        for (AccountListener l : accountListener) {
            try {
                l.accountChanged(this, t);
            } catch (Exception e) {
                log.error("Uncaught exception in account listener", e);
            }
        }
    }

    @Override
    public void sync(BCSAPI api) throws BCSAPIException {
        reset();
        addressChain.sync(api, this);
    }


    @Override
    public AddressChain getChain() {
        return addressChain;
    }

    @Override
    public TransactionFactory createTransactionFactory() {
        return new BaseTransactionFactory(this);
    }

    @Override
    public synchronized ReadOnlyAccount snapshot(BID blockId, ConfirmationManager confirmer) {
        long height = confirmer.getBlockHeight(blockId);
        if (height < 0)
            throw new IllegalArgumentException("block is not known");
        BaseReadOnlyAccount snapshot = new BaseReadOnlyAccount(addressChain);
        for (APITransaction tx : transactions.values()) {
            if (tx.getBlockID() == null) break;
            long txHeight = confirmer.getBlockHeight(tx.getBlockID());
            if (txHeight >= height) break;
            snapshot.process(tx);
        }
        return snapshot;
    }

    @Override
    public synchronized CoinBucket getConfirmedCoins() {
        return new CoinBucket(confirmed);
    }

    @Override
    public synchronized CoinBucket getSendingCoins() {
        return new CoinBucket(sending);
    }

    @Override
    public synchronized CoinBucket getReceivingCoins() {
        return new CoinBucket(receiving);
    }

    @Override
    public synchronized CoinBucket getChangeCoins() {
        return new CoinBucket(change);
    }

    @Override
    public synchronized CoinBucket getCoins() {
        CoinBucket coins = new CoinBucket();
        coins.add(confirmed);
        coins.add(change);
        coins.add(receiving);
        return coins;
    }

    @Override
    public synchronized void confirmed(APITransaction t) {
        if (updateWithConfirmedTransaction(t)) {
            notifyListener(t);
        }
    }

    @Override
    public synchronized void unconfirmed(APITransaction t) {
        processMempoolOutputs(t, false);
        notifyListener(t);
    }

    @Override
    public synchronized void doubleSpent(APITransaction t, APITransaction offendingTransaction) {
        removeOutput(t);
        transactions.remove(t.getID());
        notifyListener(t);
    }

    @Override
    public synchronized void newHeight(int height) {
    }
}
