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

import org.hyperledger.common.Transaction;

/**
 * A transaction factory that uses outputs of a previous transaction or proposal
 * The address chain parameter is needed to select change address for the created transaction
 * <p>
 * <p>
 * Note that proposal can only be used if it is fully signed or if TID does not depend
 * on signature (not yet available on Bitcoin).
 * <p>
 * Output order is preserved.
 */
public class ChainTransactionFactory extends BaseTransactionFactory {
    public ChainTransactionFactory(AddressChain chain, Transaction transaction) {
        super(new BaseReadOnlyAccount(chain, transaction.getCoins()));
    }

    public ChainTransactionFactory(AddressChain chain, TransactionProposal proposal) {
        super(new BaseReadOnlyAccount(chain, proposal.getTransaction().getCoins()));
    }
}
