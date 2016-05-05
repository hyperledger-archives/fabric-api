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

/**
 * Interface for a listener of confirmations
 *
 * @see ConfirmationManager
 */
public interface ConfirmationListener {
    /**
     * Called by the confirmation manager to ask if this listener cares of a transaction
     *
     * @param t a transaction wrapped into an APITransaction that also holds the block ID
     * @return true if this listener wants notifications on change of confirmation status for this transaction
     */
    boolean isKnownTransaction(APITransaction t);

    /**
     * Called if a transaction is included into a block
     *
     * @param t a transaction wrapped into an APITransaction that also holds the block ID
     */
    void confirmed(APITransaction t);

    /**
     * Called if a transaction that was previously in a block is un-confirmed through reorganisation of the chain
     *
     * @param t a transaction wrapped into an APITransaction that also holds the block ID
     */
    void unconfirmed(APITransaction t);

    /**
     * Called if a transaction was replaced by an other spending the same inputs.
     *
     * @param old         a transaction previously in a block
     * @param replacement a new transaction in a block
     */
    void doubleSpent(APITransaction old, APITransaction replacement);

    /**
     * Called to notify that the chain got longer
     *
     * @param height
     */
    void newHeight(int height);
}
