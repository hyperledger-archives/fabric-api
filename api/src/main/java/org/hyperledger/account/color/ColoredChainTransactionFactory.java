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

import org.hyperledger.account.AddressChain;
import org.hyperledger.account.TransactionProposal;
import org.hyperledger.common.Transaction;

public class ColoredChainTransactionFactory extends ColoredBaseTransactionFactory {
    public ColoredChainTransactionFactory(AddressChain chain, Transaction transaction) {
        super(new ColoredBaseReadOnlyAccount(chain, transaction.getCoins()));
    }

    public ColoredChainTransactionFactory(AddressChain chain, TransactionProposal proposal) {
        super(new ColoredBaseReadOnlyAccount(chain, proposal.getTransaction().getCoins()));
    }
}
