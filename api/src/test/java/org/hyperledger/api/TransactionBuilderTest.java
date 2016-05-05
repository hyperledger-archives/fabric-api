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
package org.hyperledger.api;

import org.hyperledger.common.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TransactionBuilderTest {
    @Test
    public void testBuilder() throws HyperLedgerException {
        Address address1 = PrivateKey.createNew(true).getAddress();
        Address address2 = PrivateKey.createNew(true).getAddress();
        Transaction tx = new Transaction.Builder()
                .inputs(TransactionInput.create()
                                .sequence(0)
                                .source(new Outpoint(TID.INVALID, 0))
                                .build(),
                        TransactionInput.create()
                                .source(new Outpoint(TID.INVALID, 1))
                                .sequence(0)
                                .build())

                .outputs(TransactionOutput.create()
                                .payTo(address1)
                                .value(1000)
                                .build(),
                        TransactionOutput.create()
                                .payTo(address2)
                                .value(2000)
                                .build())
                .build();


        assertEquals(2, tx.getInputs().size());
        assertEquals(0, tx.getInput(0).getOutputIndex());
        assertEquals(1, tx.getInput(1).getOutputIndex());

        assertEquals(2, tx.getOutputs().size());
        assertEquals(1000L, tx.getOutput(0).getValue());
        assertEquals(2000L, tx.getOutput(1).getValue());
        assertTrue(tx.getOutput(0).getScript().isPayToAddress());
        assertTrue(tx.getOutput(1).getScript().isPayToAddress());
    }
}
