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

import org.hyperledger.account.CoinBucket;
import org.hyperledger.common.Coin;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.TransactionOutput;
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;

import java.util.Map;

public class ColoredCoinBucket extends CoinBucket {

    public ColoredCoinBucket() {
    }

    public ColoredCoinBucket(CoinBucket coinBucket) {
        add(coinBucket);
    }

    public ColoredCoinBucket(ColoredCoinBucket other) {
        this(new CoinBucket(other));
    }

    @Override
    public synchronized ColoredCoinBucket add(CoinBucket other) {
        for (Coin s : other.getCoins()) {
            add(s);
        }
        return this;
    }

    public synchronized boolean add(Coin coin) {
        TransactionOutput previous = coins.put(coin.getOutpoint(), coin.getOutput().toColor());
        return previous == null || !previous.equals(coin.getOutput());
    }

    public synchronized long getTotalQuantity() {
        return coins.values().stream()
                .mapToLong(output -> output.toColor().getQuantity())
                .sum();
    }

    public synchronized ColoredCoinBucket getCoins(Color color) {
        ColoredCoinBucket result = new ColoredCoinBucket();
        for (Map.Entry<Outpoint, TransactionOutput> e : coins.entrySet()) {
            ColoredTransactionOutput o = e.getValue().toColor();
            if (o.getColor().equals(color)) {
                result.add(new ColoredCoin(e.getKey(), o));

            }
        }
        return result;
    }

}
