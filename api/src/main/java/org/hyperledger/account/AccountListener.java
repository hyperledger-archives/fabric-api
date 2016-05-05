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

import org.hyperledger.api.APITransaction;

/**
 * An account listener
 */
public interface AccountListener {
    /**
     * Called whenever the account's content is changing provided the account is
     * registered as listener for transactions, rejects and confirmations
     *
     * @param account the account that changed
     * @param t       a transaction that triggered the change
     */
    void accountChanged(ReadOnlyAccount account, APITransaction t);
}
