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

import com.google.protobuf.ByteString;
import org.hyperledger.common.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HyperledgerTransaction extends Transaction {

    private final BID blockID;

    public HyperledgerTransaction(Transaction transaction, BID blockID) {
        super(transaction.getID());
        this.blockID = blockID;
    }


    /**
     * get hash of the block this transaction is embedded into. Note that this is not part of the protocol, but is filled by the server while retrieving a
     * transaction in context of a block A transaction alone might not have this filled.
     */
    public BID getBlockID() {
        return blockID;
    }
}
