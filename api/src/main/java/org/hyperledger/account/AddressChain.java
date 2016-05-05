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

import java.util.Set;

public interface AddressChain {

    Set<Address> getRelevantAddresses();

    Address getNextChangeAddress() throws HyperLedgerException;

    Address getNextReceiverAddress() throws HyperLedgerException;

    void sync(BCSAPI api, TransactionListener txListener) throws BCSAPIException;
}
