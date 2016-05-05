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

import org.hyperledger.account.color.ColoredCoinBucket;
import org.hyperledger.common.Coin;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.TransactionOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinBucket {
    protected final HashMap<Outpoint, TransactionOutput> coins;

    @SuppressWarnings("unchecked")
    public CoinBucket(CoinBucket other) {
        coins = (HashMap<Outpoint, TransactionOutput>) other.coins.clone();
    }

    public CoinBucket() {
        this.coins = new HashMap<>();
    }

    public CoinBucket(List<Coin> coins) {
        this.coins = new HashMap<>();
        for (Coin c : coins) {
            this.coins.put(c.getOutpoint(), c.getOutput());
        }
    }

    public synchronized void clear() {
        coins.clear();
    }

    public synchronized boolean add(Coin coin) {
        TransactionOutput previous = coins.put(coin.getOutpoint(), coin.getOutput());
        return previous == null || !previous.equals(coin.getOutput());
    }

    public synchronized void addAll(List<Coin> coins) {
        for (Coin c : coins) {
            add(c);
        }
    }

    public synchronized TransactionOutput remove(Outpoint outpoint) {
        return coins.remove(outpoint);
    }

    public synchronized CoinBucket add(CoinBucket other) {
        coins.putAll(other.coins);
        return this;
    }

    public synchronized boolean contains(Outpoint outpoint) {
        return coins.containsKey(outpoint);
    }

    public synchronized boolean contains(Coin coin) {
        TransactionOutput output = coins.get(coin.getOutpoint());
        return output != null && output.equals(coin.getOutput());
    }

    public synchronized long getTotalSatoshis() {
        long s = 0;
        for (TransactionOutput o : coins.values()) {
            s += o.getValue();
        }
        return s;
    }

    public synchronized List<Coin> getCoins() {
        List<Coin> cl = new ArrayList<>();
        for (Map.Entry<Outpoint, TransactionOutput> entry : coins.entrySet()) {
            cl.add(new Coin(entry.getKey(), entry.getValue()));
        }
        return cl;
    }

    public synchronized Coin getCoin(Outpoint outpoint) {
        if (coins.containsKey(outpoint))
            return new Coin(outpoint, coins.get(outpoint));
        return null;
    }

    public synchronized ColoredCoinBucket getColoredCoins() {
        return new ColoredCoinBucket(this);
    }
}
