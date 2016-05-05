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

import org.hyperledger.api.BCSAPI;
import org.hyperledger.api.BCSAPIException;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class MasterKeyChain<K extends Key> {
    private static final Logger log = LoggerFactory.getLogger(MasterPrivateChain.class);

    private final Map<Address, Integer> keyIDForAddress = new HashMap<>();
    private final Map<Integer, K> keyCache = new HashMap<>();
    private final MasterKey<K> master;
    private final int lookAhead;
    private int nextSequence;

    private static ExecutorService pool = Executors.newWorkStealingPool();

    public MasterKeyChain(MasterKey<K> master, int lookAhead) throws HyperLedgerException {
        this.master = master;
        this.lookAhead = lookAhead;
        ensureLookAhead(0);
    }

    public int getLookAhead() {
        return lookAhead;
    }

    public int getNextSequence() {
        return nextSequence;
    }

    private void ensureLookAhead(int from) throws HyperLedgerException {
        while (keyIDForAddress.size() < (from + lookAhead)) {
            int keyIndex = keyIDForAddress.size();
            K key = master.getKey(keyIndex);
            keyCache.put(keyIndex, key);
            keyIDForAddress.put(key.getAddress(), keyIndex);
        }
    }

    /**
     * Pre-generate keys.  This is done in parallel to reduce latency
     * and keep CPU fully occupied.  The next n calls to {@link #getNextAddress()}
     * and {@link #getNextKey()} will be served from the cache.
     *
     * @param n the number of keys to pre-genereate
     */
    public synchronized void preGenerate(int n) {
        int end = nextSequence + n + lookAhead;

        List<Future<K>> futures = new ArrayList<>();
        int size = keyIDForAddress.size();
        for (int i = size ; i < end ; i++) {
            int keyIndex = i;
            futures.add(pool.submit(() -> master.getKey(keyIndex)));
        }

        for (int keyIndex = size ; keyIndex < end ; keyIndex++) {
            K key;
            try {
                key = futures.get(keyIndex - size).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            keyCache.put(keyIndex, key);
            keyIDForAddress.put(key.getAddress(), keyIndex);
        }
    }

    public synchronized K getKey(int i) throws HyperLedgerException {
        ensureLookAhead(i + 1);
        return keyCache.get(i);
    }

    public synchronized void setNextKey(int i) throws HyperLedgerException {
        nextSequence = i;
        ensureLookAhead(nextSequence);
    }

    public synchronized K getNextKey() throws HyperLedgerException {
        return getKey(nextSequence++);
    }

    public synchronized Integer getKeyIDForAddress(Address address) {
        return keyIDForAddress.get(address);
    }

    public Set<Address> getRelevantAddresses() {
        return Collections.unmodifiableSet(keyIDForAddress.keySet());
    }

    public Address getNextChangeAddress() throws HyperLedgerException {
        return getNextAddress();
    }

    public Address getNextReceiverAddress() throws HyperLedgerException {
        return getNextAddress();
    }

    public Address getNextAddress() throws HyperLedgerException {
        return getNextKey().getAddress();
    }

    protected void sync(MasterPublicKey masterPublicKey, BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        log.trace("Sync nkeys: {}", nextSequence);
        api.scanTransactions(masterPublicKey, lookAhead, t -> {
            for (TransactionOutput o : t.getOutputs()) {
                Integer thisKey = getKeyIDForAddress(o.getOutputAddress());
                if (thisKey != null) {
                    ensureLookAhead(thisKey);
                    nextSequence = Math.max(nextSequence, thisKey + 1);
                }
            }
            txListener.process(t);
        });
        log.trace("Sync finished with nkeys: {}", nextSequence);
    }

    public synchronized K getKeyForAddress(Address address) {
        Integer keyId = getKeyIDForAddress(address);
        if (keyId == null) {
            return null;
        }
        try {
            return getKey(keyId);
        } catch (HyperLedgerException e) {
            return null;
        }
    }

}
