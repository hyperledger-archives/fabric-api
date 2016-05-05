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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AddressListChain implements AddressChain {
    private static final Logger log = LoggerFactory.getLogger(AddressListChain.class);

    private final Set<Address> addresses = new HashSet<>();

    public AddressListChain(Address... addresses) {
        for (Address address : addresses) {
            addAddress(address);
        }
    }

    public AddressListChain(Collection<Address> addresses) {
        this.addresses.addAll(addresses);
    }

    @Override
    public Set<Address> getRelevantAddresses() {
        return Collections.unmodifiableSet(addresses);
    }

    @Override
    public Address getNextChangeAddress() throws HyperLedgerException {
        return addresses.iterator().next();
    }

    @Override
    public Address getNextReceiverAddress() throws HyperLedgerException {
        return addresses.iterator().next();
    }

    public void addAddress(Address address) {
        addresses.add(address);
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        log.trace("Sync naddr: {}", addresses.size());
        api.scanTransactionsForAddresses(addresses, txListener);
        log.trace("Sync finished naddr: {}", addresses.size());
    }
}
