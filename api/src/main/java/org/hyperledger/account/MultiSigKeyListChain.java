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

public class MultiSigKeyListChain implements MultiSigKeyChain {
    private static final Logger log = LoggerFactory.getLogger(MultiSigKeyListChain.class);
    private final Address address;
    private final MultiSigSigner signer;

    private final List<Key> keys;

    public MultiSigKeyListChain(int signaturesNeeded, Key... keyz) throws HyperLedgerException {
        if (signaturesNeeded <= 0 || signaturesNeeded > keyz.length)
            throw new HyperLedgerException("n of m multisig needs n > 0 && n <= m");
        keys = new ArrayList<>();
        for (Key k : keyz)
            keys.add(k);
        signer = new MultiSigSigner(signaturesNeeded, this);
        address = signer.getSpendScript(keys).getAddress();
    }

    public MultiSigKeyListChain(int signaturesNeeded, List<Key> keys) throws HyperLedgerException {
        if (signaturesNeeded <= 0 || signaturesNeeded > keys.size())
            throw new HyperLedgerException("n of m multisig needs n > 0 && n <= m");
        signer = new MultiSigSigner(signaturesNeeded, this);
        this.keys = keys;
        address = signer.getSpendScript(keys).toP2SHAddress();
    }

    @Override
    public Signer getSigner() {
        return signer;
    }

    @Override
    public Set<Address> getRelevantAddresses() {
        Set<Address> aset = new HashSet<>();
        aset.add(address);
        return aset;
    }

    @Override
    public Address getNextChangeAddress() throws HyperLedgerException {
        return address;
    }

    @Override
    public Address getNextReceiverAddress() throws HyperLedgerException {
        return address;
    }

    @Override
    public PrivateKey getKeyForAddress(Address address) throws HyperLedgerException {
        throw new HyperLedgerException("Use getKeysForAddress instead");
    }

    @Override
    public Script getSpendScript(Address address) throws HyperLedgerException {
        return signer.getSpendScript(address);
    }

    @Override
    public List<Key> getKeysForAddress(Address a) throws HyperLedgerException {
        return Collections.unmodifiableList(keys);
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        log.trace("Sync naddr: {}", keys.size());
        api.scanTransactionsForAddresses(getRelevantAddresses(), txListener);
        log.trace("Sync finished naddr: {}", keys.size());

    }
}
