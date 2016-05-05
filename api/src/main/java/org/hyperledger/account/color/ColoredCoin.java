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

import org.hyperledger.common.Coin;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.Transaction;
import org.hyperledger.common.color.ColoredTransactionOutput;

public class ColoredCoin extends Coin {
    public ColoredCoin(Outpoint outpoint, ColoredTransactionOutput output) {
        super(outpoint, output);
    }

    public ColoredCoin(Transaction transaction, int ix) {
        super(transaction, ix);
    }

    @Override
    public ColoredTransactionOutput getOutput() {
        return (ColoredTransactionOutput) super.getOutput();
    }
}
