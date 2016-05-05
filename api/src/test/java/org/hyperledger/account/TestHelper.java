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

import static org.junit.Assert.assertEquals;

public class TestHelper {

    private TestHelper() {
    }

    public static void assertCoinsAndValues(ReadOnlyAccount account, Long expectedSending, Long expectedChange, Long expectedReceiving, Long expectedConfirmed, Long expectedBalance) {
        assertEquals(expectedSending == null ? 0L : expectedSending, account.getSendingCoins().getTotalSatoshis());
        assertEquals(expectedChange == null ? 0L : expectedChange, account.getChangeCoins().getTotalSatoshis());
        assertEquals(expectedReceiving == null ? 0L : expectedReceiving, account.getReceivingCoins().getTotalSatoshis());
        assertEquals(expectedConfirmed == null ? 0L : expectedConfirmed, account.getConfirmedCoins().getTotalSatoshis());
        assertEquals(expectedBalance == null ? 0L : expectedBalance, account.getCoins().getTotalSatoshis());
    }

}
