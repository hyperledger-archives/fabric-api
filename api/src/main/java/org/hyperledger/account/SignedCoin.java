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

import org.hyperledger.common.Coin;
import org.hyperledger.common.Outpoint;
import org.hyperledger.common.Script;
import org.hyperledger.common.TransactionOutput;

public class SignedCoin extends Coin {
    private final Script signature;

    public SignedCoin(Coin coin, Script signature) {
        super(coin.getOutpoint(), coin.getOutput());
        this.signature = signature;
    }

    public SignedCoin(Outpoint outpoint, TransactionOutput output, Script signature) {
        super(outpoint, output);
        this.signature = signature;
    }

    public Script getSignature() {
        return signature;
    }
}
