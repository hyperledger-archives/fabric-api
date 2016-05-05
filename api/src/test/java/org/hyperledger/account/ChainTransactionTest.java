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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.common.*;
import org.junit.Test;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChainTransactionTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void chainTest() throws HyperLedgerException {
        MasterPrivateChain chain = new MasterPrivateChain(MasterPrivateKey.createNew());

        List<Coin> starterCoins = new ArrayList<>();
        starterCoins.add(new Coin(new Outpoint(TID.INVALID, 0),
                TransactionOutput.create().payTo(chain.getNextReceiverAddress()).value(100000).build()));
        Account account = new BaseAccount(chain, starterCoins);


        // Have to sign until TID depends on signatures
        Transaction first =
                account.createTransactionFactory().propose(chain.getNextChangeAddress(), 50000).sign(chain);

        Transaction second = new ChainTransactionFactory(chain, first)
                .propose(chain.getNextReceiverAddress(), 40000).sign(chain);

        assertEquals(second.getSource(0), first.getCoin(0).getOutpoint());
    }
}
