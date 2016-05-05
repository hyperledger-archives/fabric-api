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

import org.hyperledger.common.Coin;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.SignatureOptions;
import org.hyperledger.common.Transaction;

/** Interface for signing transaction inputs */
public interface Signer {
    /**
     * @param source the source the input is spending
     * @param ix the input index
     * @param transaction the spending transaction to be signed
     * @return a signed coin
     * @throws HyperLedgerException
     */
    Coin sign(Coin source, int ix, Transaction transaction) throws HyperLedgerException;

    /**
     * @param source the source the input is spending
     * @param ix the input index
     * @param transaction the spending transaction to be signed
     * @param signatureOptions signature options
     * @return a signed coin
     * @throws HyperLedgerException
     */
    Coin sign(Coin source, int ix, Transaction transaction, SignatureOptions signatureOptions) throws HyperLedgerException;
}
