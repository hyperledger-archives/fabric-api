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

import org.hyperledger.account.ReadOnlyAccount;
import org.hyperledger.common.color.Color;

/**
 * A colored read-only account
 */
public interface ColoredReadOnlyAccount extends ReadOnlyAccount {
    /**
     * Get transaction factory for this account
     *
     * @return a color aware transaction factory
     */
    @Override
    ColoredTransactionFactory createTransactionFactory();

    /**
     * @return all coins owned by the account
     */
    ColoredCoinBucket getColoredCoins();

    /**
     * Get coins of a given color owned by the account
     *
     * @param color
     * @return coins of given color in this account
     */
    ColoredCoinBucket getCoins(Color color);
}
