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
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.MasterPrivateKey;
import org.hyperledger.common.PrivateKey;

public class MasterPrivateChain extends MasterKeyChain<PrivateKey> implements KeyChain {

    private final MasterPrivateKey master;
    private final Signer signer;

    public MasterPrivateKey getMaster() {
        return master;
    }

    public MasterPrivateChain(MasterPrivateKey master) throws HyperLedgerException {
        this(master, 10);
    }

    public MasterPrivateChain(MasterPrivateKey master, int lookAhead) throws HyperLedgerException {
        super(master, lookAhead);
        this.master = master;
        signer = new ChainSigner(this);
    }

    @Override
    public Signer getSigner() {
        return signer;
    }

    @Override
    public void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException {
        sync(master.getMasterPublic(), api, txListener);
    }

}
