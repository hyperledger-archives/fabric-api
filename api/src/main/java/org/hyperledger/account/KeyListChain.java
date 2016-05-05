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
import org.hyperledger.common.Address;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.PrivateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;

public class KeyListChain implements KeyChain {
    private static final Logger log = LoggerFactory.getLogger(KeyListChain.class);

    private final Map<Address, PrivateKey> keyByAddress = new HashMap<>();
    private final Signer signer;

    private static final SecureRandom rnd = new SecureRandom();

    private final List<PrivateKey> keys = new ArrayList<>();

    public KeyListChain(PrivateKey... keys) {
        for (PrivateKey k : keys)
            this.addKey(k);
        signer = new ChainSigner(this);
    }

    @Override
    public Signer getSigner() {
        return signer;
    }

    public void addKey(PrivateKey key) {
        keys.add(key);
        keyByAddress.put(key.getAddress(), key);
    }

    @Override
    public Set<Address> getRelevantAddresses() {
        return Collections.unmodifiableSet(keyByAddress.keySet());
    }

    @Override
    public Address getNextChangeAddress() throws HyperLedgerException {
        return getNextKey().getAddress();
    }

    @Override
    public Address getNextReceiverAddress() throws HyperLedgerException {
        return getNextKey().getAddress();
    }

    public PrivateKey getNextKey() throws HyperLedgerException {
        return keys.get(rnd.nextInt(keys.size()));
    }

    @Override
    public PrivateKey getKeyForAddress(Address address) {
        return keyByAddress.get(address);
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        log.trace("Sync naddr: {}", keys.size());
        api.scanTransactionsForAddresses(getRelevantAddresses(), txListener);
        log.trace("Sync finished naddr: {}", keys.size());

    }
}
