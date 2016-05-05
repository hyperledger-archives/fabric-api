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
package org.hyperledger.api;

import org.hyperledger.common.*;

import java.util.List;
import java.util.Set;

/**
 * This is the low level API to the HyperLedger block chain server
 */
public interface BCSAPI {
    /**
     * retrieves client version
     *
     * @return client version e.g. 4.0.0
     */
    String getClientVersion() throws BCSAPIException;

    /**
     * retrieves server version
     *
     * @return server version e.g. 4.0.0
     */
    String getServerVersion() throws BCSAPIException;

    /**
     * Returns nounce while doing a full roundtrip to the server.
     * Useful only to check connectivity. The server returns the same
     * nonce with a full roundtrip.
     *
     * @param nonce - random nonce
     * @return the same nonce
     * @throws BCSAPIException
     */
    long ping(long nonce) throws BCSAPIException;

    /**
     * Sets the alert listener for the connections.
     * This would only be called if an alert message was available on the P2P network
     * In case of the Bitcoin network the alert message would have to be signed with
     * a key satoshi left to some developer.
     *
     * @param listener - an alert listener
     * @throws BCSAPIException
     */
    void addAlertListener(AlertListener listener) throws BCSAPIException;

    void removeAlertListener(AlertListener listener);

    /**
     * Get chain height of the trunk list
     *
     * @throws BCSAPIException
     */
    int getChainHeight() throws BCSAPIException;

    /**
     * Get a list of block ids starting from the block with the given id
     *
     * @param blockId - block hash to start the listing from, zero hash means start from the top block
     * @param count   - how many block ids to return, default is 20 if a non-positive number is provided
     * @return header list or null if block id is unknown
     * @throws BCSAPIException
     */
    APIBlockIdList getBlockIds(BID blockId, int count) throws BCSAPIException;

    /**
     * Get block header for the hash
     *
     * @param hash - block hash
     * @return block header or null if hash is unknown
     * @throws BCSAPIException
     */
    APIHeader getBlockHeader(BID hash) throws BCSAPIException;

    /**
     * Get block for the hash
     *
     * @param hash - block hash
     * @return block or null if hash is unknown
     * @throws BCSAPIException
     */
    APIBlock getBlock(BID hash) throws BCSAPIException;

    /**
     * Get the transaction identified by the hash, if it is on the current trunk (longest chain)
     *
     * @param hash - transaction hash (id)
     * @return transaction or null if no transaction with that hash on the trunk
     * @throws BCSAPIException
     */
    APITransaction getTransaction(TID hash) throws BCSAPIException;

    /**
     * Get the input transactions of the given transaction
     *
     * @param txId - transaction id
     * @return list of input transactions
     */
    List<APITransaction> getInputTransactions(TID txId) throws BCSAPIException;

    /**
     * Send a signed transaction to the network.
     *
     * @param transaction - a signed transaction
     * @throws BCSAPIException
     */
    void sendTransaction(Transaction transaction) throws BCSAPIException;

    /**
     * Register a reject message listener.
     * A connected node might reject a transaction or block message of this server.
     * It could be an indication of a problem on this end, but it might be byzantine behaviour of the other.
     *
     * @param rejectListener - a reject listener
     * @throws BCSAPIException
     */
    void registerRejectListener(RejectListener rejectListener) throws BCSAPIException;

    /**
     * Remove a reject listener
     *
     * @param rejectListener - a previously registered listener
     */
    void removeRejectListener(RejectListener rejectListener);

    /**
     * Mine a block
     */
    APIHeader mine(Address address) throws BCSAPIException;

    /**
     * Send a block newly created by this node.
     *
     * @param block - a new valid block
     * @throws BCSAPIException
     */
    void sendBlock(Block block) throws BCSAPIException;

    /**
     * Register a transactions listener.
     * All valid transactions observed on the network will be forwarded to this listener.
     *
     * @param listener will be called for every validated transaction
     * @throws BCSAPIException
     */
    void registerTransactionListener(TransactionListener listener) throws BCSAPIException;

    /**
     * Remove a listener for validated transactions
     *
     * @param listener - a previously registered transaction listener
     */
    public void removeTransactionListener(TransactionListener listener);

    /**
     * Register a block listener.
     * All validated new blocks on the network, that extend the longest chain, will be forwarded to this listener.
     *
     * @param listener will be called for every validated new block
     * @throws BCSAPIException
     */
    void registerTrunkListener(TrunkListener listener) throws BCSAPIException;

    /**
     * remove a trunk listener previously registered
     *
     * @param listener
     */
    void removeTrunkListener(TrunkListener listener);

    /**
     * Scan transactions using an address in the given set.
     * The listener will be called for every transaction matching the search criteria AND with every transaction
     * spending an output of a transaction that matches the criteria.
     * This call will not return until the listener is called for all transactions identified.
     *
     * @param addresses - address set
     * @param listener  - the transaction listener will be called for all transactions found, in chronological order.
     * @throws BCSAPIException
     */
    void scanTransactionsForAddresses(Set<Address> addresses, TransactionListener listener)
            throws BCSAPIException;

    /**
     * Scan transactions for reference of any address of a master key.
     * The listener will be called for every transaction matching the search criteria AND with every transaction
     * spending an output of a transaction that matches the criteria. The calls will happen in chronological order.
     * This call will not return until the listener is called for all transactions identified.
     *
     * @param master    - public master key
     * @param lookAhead - look ahead window while scanning for addresses. The server assumes that the gap between consecutive addresses of the master key used on the
     *                  block chain is not bigger than lookAhead.
     * @param listener  - the transaction listener will be called for all transactions found, in chronological order.
     * @throws BCSAPIException
     */
    void scanTransactions(MasterPublicKey master, int lookAhead, TransactionListener listener) throws BCSAPIException;

    /**
     * Generate a trunk update to catch up from current inventory.
     * Useful for a client that was disconnected from the network. The client might provide a trunk list of his
     * knowledge and the server replies with the appropriate extension list. The extension might not continue at
     * the last hast of the client's inventory, but on one earlier, indicating that the longest chain is a fork of that.
     *
     * @param inventory of block hashes known, highest first
     * @param limit     maximum number of blocks or header expected, if inventory is empty
     * @param headers   indicate if headers or full blocks are expected
     * @param listener  a listener for trunk extensions
     * @throws BCSAPIException
     */
    void catchUp(List<BID> inventory, int limit, boolean headers, TrunkListener listener) throws BCSAPIException;

    /**
     * Call listener for every transaction that spends any output from tids
     *
     * @param tids
     * @param listener
     * @throws BCSAPIException
     */
    void spendingTransactions(List<TID> tids, final TransactionListener listener) throws BCSAPIException;

}
