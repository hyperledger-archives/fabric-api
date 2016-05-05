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

import org.hyperledger.api.*;
import org.hyperledger.common.BID;

import java.util.List;

/**
 * An account is able to snapshot and monitor coins on block chain.
 * The scope of an account is defined by its address chain.
 * <p>
 * To get a snapshot of the account use sync(api)
 * <p>
 * Register an account as listener for Transactions, Rejects and Confirmations at the BCSAPI
 * and at a ConfirmationManager instance for real-time changes
 *
 * @see BCSAPI
 * @see ConfirmationManager
 */
public interface ReadOnlyAccount extends ConfirmationListener, TransactionListener, RejectListener {

    /**
     * Get a snapshot of holdings by sync-ing
     *
     * @param api a connection to the block chain server
     * @throws BCSAPIException communication problem with the server
     */
    void sync(BCSAPI api) throws BCSAPIException;

    /**
     * Get the address chain that defines the scope of the account.
     *
     * @return an address chain
     */
    AddressChain getChain();

    /**
     * Get all transactions that affected the addresses in scope.
     *
     * @return
     */
    List<APITransaction> getTransactions();

    /**
     * Get coins already on block chain
     *
     * @return a set of coins
     */
    CoinBucket getConfirmedCoins();

    /**
     * Get coins the account is currently sending, not yet on block chain
     *
     * @return a set of coins
     */
    CoinBucket getSendingCoins();


    /**
     * Get coins the account is currently receiving, not yet on block chain
     *
     * @return a set of coins
     */
    CoinBucket getReceivingCoins();

    /**
     * Get coins the account is currently receiving from its own transactions, not yet on block chain
     *
     * @return a set of coins
     */
    CoinBucket getChangeCoins();

    /**
     * Get all coins the account has, that is confirmed + receiving + change
     *
     * @return a set of coins
     */
    CoinBucket getCoins();

    /**
     * Register a listener for changes in this account
     *
     * @param listener an account listener
     */
    void addAccountListener(AccountListener listener);

    /**
     * Remove a previously registered listener
     *
     * @param listener an account listener
     */
    void removeAccountListener(AccountListener listener);

    /**
     * Create a transaction factory that prepares transactions spending holdings of this account
     *
     * @return a new transaction factory
     */
    TransactionFactory createTransactionFactory();

    /**
     * Snapshot the account as it was just before the specified block
     *
     * @param blockId the block ID to snapshot before
     * @param manager the confirmation manager to use for retrieving block heights
     * @return a temporary account with a subset of transactions.  This account is not listening to any updates.
     */
    ReadOnlyAccount snapshot(BID blockId, ConfirmationManager manager);
}
