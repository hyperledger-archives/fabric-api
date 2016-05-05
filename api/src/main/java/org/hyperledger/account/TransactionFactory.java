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

import org.hyperledger.common.Address;
import org.hyperledger.common.Coin;
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.TransactionOutput;

import java.util.Comparator;
import java.util.List;

/** Create transaction proposals.  The order of outputs is preserved. */
public interface TransactionFactory {

    public static final long DUST_LIMIT = 2730;
    public static final long KB_FEE = 1000;
    public static final long MINIMUM_FEE = 5000;
    public static final long MAXIMUM_FEE = 1000000;


    public static Comparator<Coin> spendPreferenceOrder = (o1, o2) -> {
        // prefers aggregation
        return -Long.compare(o1.getOutput().getValue(), o2.getOutput().getValue());
    };


    ReadOnlyAccount getAccount();

    CoinBucket getSufficientSources(long amount, long fee) throws HyperLedgerException;

    TransactionProposal propose(Address receiver, long amount) throws HyperLedgerException;

    TransactionProposal propose(Address receiver, long amount, PaymentOptions options) throws HyperLedgerException;

    TransactionProposal propose(PaymentOptions options, TransactionOutput... outputs) throws HyperLedgerException;

    TransactionProposal propose(PaymentOptions options, List<TransactionOutput> outputs) throws HyperLedgerException;
}
