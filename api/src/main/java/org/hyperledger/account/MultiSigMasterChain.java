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

import java.security.SecureRandom;
import java.util.*;

public class MultiSigMasterChain implements MultiSigKeyChain {
    private static final Logger log = LoggerFactory.getLogger(MultiSigMasterChain.class);
    private static final SecureRandom random = new SecureRandom();

    private final Map<Address, Integer> keyIDForAddress = new HashMap<>();
    private final List<MasterKey> keys;
    private final int signaturesNeeded;
    private final MultiSigSigner signer;
    private int poolSize = 100;

    public MultiSigMasterChain(int signaturesNeeded, MasterKey... keys) throws HyperLedgerException {
        if (signaturesNeeded <= 0 || signaturesNeeded > keys.length)
            throw new HyperLedgerException("n of m multisig needs n > 0 && n <= m");
        this.keys = new ArrayList<>();
        Collections.addAll(this.keys, keys);
        this.signaturesNeeded = signaturesNeeded;
        signer = new MultiSigSigner(signaturesNeeded, this);
        for (int i = 0; i < poolSize; ++i) {
            keyIDForAddress.put(getAddress(i), i);
        }
    }

    public MultiSigMasterChain(int signaturesNeeded, List<MasterKey> keys) throws HyperLedgerException {
        if (signaturesNeeded <= 0 || signaturesNeeded > keys.size())
            throw new HyperLedgerException("n of m multisig needs n > 0 && n <= m");
        this.keys = keys;
        this.signaturesNeeded = signaturesNeeded;
        signer = new MultiSigSigner(signaturesNeeded, this);
        for (int i = 0; i < poolSize; ++i) {
            keyIDForAddress.put(getAddress(i), i);
        }
    }


    @Override
    public Signer getSigner() {
        return signer;
    }

    public int getSignaturesNeeded() {
        return signaturesNeeded;
    }

    // TODO: this is temporary, solve scan for multi-sig on server side
    public void setPoolSize(int poolSize) throws HyperLedgerException {
        for (int i = this.poolSize; keyIDForAddress.size() < poolSize; ++i) {
            keyIDForAddress.put(getAddress(i), i);
        }
        this.poolSize = poolSize;
    }

    private Address getAddress(int kix) throws HyperLedgerException {
        List<Key> publicKeys = new ArrayList<>();

        for (MasterKey key : keys) {
            Key k = key.getKey(kix);
            if (k instanceof PrivateKey) {
                k = ((PrivateKey) k).getPublic();
            }
            publicKeys.add(k);
        }

        return signer.getSpendScript(publicKeys).toP2SHAddress();
    }

    @Override
    public synchronized Script getSpendScript(Address a) throws HyperLedgerException {
        return signer.getSpendScript(a);
    }

    @Override
    public synchronized Set<Address> getRelevantAddresses() {
        return Collections.unmodifiableSet(keyIDForAddress.keySet());
    }

    @Override
    public synchronized Address getNextChangeAddress() throws HyperLedgerException {
        return getAddress(random.nextInt(keyIDForAddress.size()));
    }

    @Override
    public synchronized Address getNextReceiverAddress() throws HyperLedgerException {
        return getAddress(random.nextInt(keyIDForAddress.size()));
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        log.trace("Sync naddr: {}", keys.size());
        api.scanTransactionsForAddresses(getRelevantAddresses(), txListener);
        log.trace("Sync finished naddr: {}", keys.size());
    }

    @Override
    public PrivateKey getKeyForAddress(Address address) throws HyperLedgerException {
        throw new HyperLedgerException("Use getKeysForAddress instead for a multi-sig key chain");
    }

    @Override
    public List<Key> getKeysForAddress(Address a) throws HyperLedgerException {
        Integer kix = keyIDForAddress.get(a);
        if (kix == null) {
            throw new HyperLedgerException("unkown address for this multisig key chain " + a);
        }
        List<Key> addressKeys = new ArrayList<>();
        for (MasterKey m : keys) {
            addressKeys.add(m.getKey(kix));
        }
        return addressKeys;
    }
}
