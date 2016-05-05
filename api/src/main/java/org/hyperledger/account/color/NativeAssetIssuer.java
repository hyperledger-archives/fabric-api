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

package org.hyperledger.account.color;

import org.hyperledger.account.KeyChain;
import org.hyperledger.account.KeyListChain;
import org.hyperledger.account.TransactionProposal;
import org.hyperledger.common.Address;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.PrivateKey;
import org.hyperledger.common.Transaction;

public class NativeAssetIssuer extends ReadOnlyNativeAssetIssuer {

    public NativeAssetIssuer(PrivateKey issuerKey) {
        super(new KeyListChain(issuerKey));
    }

    public KeyChain getChain() {
        return (KeyChain) super.getChain();
    }

    public Transaction issueNativeAsset(Address receiver, long quantity, long fee) throws HyperLedgerException {
        TransactionProposal t = proposeNativeAssetIssuance(receiver, quantity, fee);
        return t.sign(getChain());
    }
}
