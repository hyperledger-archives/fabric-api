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
package org.hyperledger.common;

import org.hyperledger.account.ReadOnlyAccount;

public class OutputProposal {

    private final long amount;
    private final ReadOnlyAccount recipient;
    private final boolean blinded;
    private final Address receiverAddress;

    public OutputProposal(long amount, ReadOnlyAccount recipient, boolean blinded) throws HyperLedgerException {
        this.amount = amount;
        this.recipient = recipient;
        this.blinded = blinded;

        receiverAddress = recipient.getChain().getNextReceiverAddress();
    }

    public long getAmount() {
        return amount;
    }

    public ReadOnlyAccount getRecipient() {
        return recipient;
    }

    public Address getAddress() {
        return receiverAddress;
    }

    public boolean isBlinded() {
        return blinded;
    }
}
