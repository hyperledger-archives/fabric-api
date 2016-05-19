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

/**
 * This is the low level API to the HyperLedger block chain server
 */
public interface Hyperledger {
    /**
     * retrieves client version
     *
     * @return client version e.g. 4.0.0
     */
    String getClientVersion() throws HyperledgerException;

    /**
     * retrieves server version
     *
     * @return server version e.g. 4.0.0
     */
    String getServerVersion() throws HyperledgerException;

    /**
     * Returns nounce while doing a full roundtrip to the server.
     * Useful only to check connectivity. The server returns the same
     * nonce with a full roundtrip.
     *
     * @param nonce - random nonce
     * @return the same nonce
     * @throws HyperledgerException
     */
    long ping(long nonce) throws HyperledgerException;

    /**
     * Sets the alert listener for the connections.
     * This would only be called if an alert message was available on the P2P network
     * In case of the Bitcoin network the alert message would have to be signed with
     * a key satoshi left to some developer.
     *
     * @param listener - an alert listener
     * @throws HyperledgerException
     */
    void addAlertListener(AlertListener listener) throws HyperledgerException;

    void removeAlertListener(AlertListener listener);

    /**
     * Get chain height of the trunk list
     *
     * @throws HyperledgerException
     */
    int getChainHeight() throws HyperledgerException;

    /**
     * Get block header for the hash
     *
     * @param hash - block hash
     * @return block header or null if hash is unknown
     * @throws HyperledgerException
     */
    HyperledgerHeader getBlockHeader(BID hash) throws HyperledgerException;

    /**
     * Get block for the hash
     *
     * @param hash - block hash
     * @return block or null if hash is unknown
     * @throws HyperledgerException
     */
    HyperledgerBlock getBlock(BID hash) throws HyperledgerException;

    /**
     * Get the transaction identified by the hash, if it is on the current trunk (longest chain)
     *
     * @param hash - transaction hash (id)
     * @return transaction or null if no transaction with that hash on the trunk
     * @throws HyperledgerException
     */
    HyperledgerTransaction getTransaction(TID hash) throws HyperledgerException;

    /**
     * Send a signed transaction to the network.
     *
     * @param transaction - a signed transaction
     * @throws HyperledgerException
     */
    void sendTransaction(Transaction transaction) throws HyperledgerException;

    /**
     * Register a reject message listener.
     * A connected node might reject a transaction or block message of this server.
     * It could be an indication of a problem on this end, but it might be byzantine behaviour of the other.
     *
     * @param rejectListener - a reject listener
     * @throws HyperledgerException
     */
    void registerRejectListener(RejectListener rejectListener) throws HyperledgerException;

    /**
     * Remove a reject listener
     *
     * @param rejectListener - a previously registered listener
     */
    void removeRejectListener(RejectListener rejectListener);

     /**
     * Send a block newly created by this node.
     *
     * @param block - a new valid block
     * @throws HyperledgerException
     */
    void sendBlock(Block block) throws HyperledgerException;

    /**
     * Register a transactions listener.
     * All valid transactions observed on the network will be forwarded to this listener.
     *
     * @param listener will be called for every validated transaction
     * @throws HyperledgerException
     */
    void registerTransactionListener(TransactionListener listener) throws HyperledgerException;

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
     * @throws HyperledgerException
     */
    void registerTrunkListener(TrunkListener listener) throws HyperledgerException;

    /**
     * remove a trunk listener previously registered
     *
     * @param listener
     */
    void removeTrunkListener(TrunkListener listener);


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
     * @throws HyperledgerException
     */
    void catchUp(List<BID> inventory, int limit, boolean headers, TrunkListener listener) throws HyperledgerException;
}
