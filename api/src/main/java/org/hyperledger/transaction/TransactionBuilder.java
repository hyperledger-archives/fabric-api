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

package org.hyperledger.transaction;

import org.hyperledger.common.Hash;
import org.hyperledger.common.PrivateKey;

import java.util.ArrayList;
import java.util.List;

public class TransactionBuilder {

    private byte[] payload = new byte[0];
    private List<Endorser> endorsers = new ArrayList<>();
    private List<PrivateKey> endorserKeys = new ArrayList<>();

    public TransactionBuilder payload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    public TransactionBuilder endorsers(List<Endorser> endorsers) {
        this.endorsers.addAll(endorsers);
        return this;
    }

    public TransactionBuilder endorse(PrivateKey key) {
        endorserKeys.add(key);
        return this;
    }

    public Transaction build() {
        for (PrivateKey key : endorserKeys) {
            Endorser endorser = Endorser.create(Hash.of(payload).toByteArray(), key);
            endorsers.add(endorser);
        }
        return new Transaction(payload, endorsers);
    }

}
