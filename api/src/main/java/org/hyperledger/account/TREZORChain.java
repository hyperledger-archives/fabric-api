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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TREZORChain implements AddressChain {
    private final MasterPublicChain receiver;
    private final MasterPublicChain change;
    private final MasterKey master;

    public TREZORChain(MasterPublicKey master, int lookAhead) throws HyperLedgerException {
        this.master = master;
        receiver = new MasterPublicChain(master.getChild(0), lookAhead);
        change = new MasterPublicChain(master.getChild(1), lookAhead);
    }

    public TREZORChain(MasterPublicKey master) throws HyperLedgerException {
        this.master = master;
        receiver = new MasterPublicChain(master.getChild(0));
        change = new MasterPublicChain(master.getChild(1));
    }

    public MasterKey getMaster() {
        return master;
    }

    public int getLookAhead() {
        return receiver.getLookAhead();
    }

    public boolean isReceiverAddress(Address address) {
        Key key = receiver.getKeyForAddress(address);
        return key != null;
    }

    public boolean isChangeAddress(Address address) {
        Key key = change.getKeyForAddress(address);
        return key != null;
    }

    public synchronized int[] getKeyPathForAddress(Address address) {
        Integer rk = receiver.getKeyIDForAddress(address);
        if (rk != null) {
            return new int[]{0, rk};
        }
        Integer ck = change.getKeyIDForAddress(address);
        if (ck != null) {
            return new int[]{1, ck};
        }
        return null;
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        receiver.sync(api, txListener);
        change.sync(api, txListener);
    }

    @Override
    public Set<Address> getRelevantAddresses() {
        Set<Address> all = new HashSet<>();
        all.addAll(receiver.getRelevantAddresses());
        all.addAll(change.getRelevantAddresses());
        return Collections.unmodifiableSet(all);
    }

    @Override
    public Address getNextChangeAddress() throws HyperLedgerException {
        return change.getNextChangeAddress();
    }

    @Override
    public Address getNextReceiverAddress() throws HyperLedgerException {
        return receiver.getNextReceiverAddress();
    }
}
