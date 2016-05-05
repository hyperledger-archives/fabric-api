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

public class APITransaction extends Transaction {

    private final BID blockID;

    public APITransaction(Transaction transaction, BID blockID) {
        super(transaction.getVersion(), transaction.getLockTime(), transaction.getInputs(), transaction.getOutputs(),
                transaction.getID());
        this.blockID = blockID;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder extends Transaction.Builder {
        private int version = 1;
        private int lockTime = 0;
        private BID blockID;

        private List<TransactionInput> inputs = new ArrayList<>();
        private List<TransactionOutput> outputs = new ArrayList<>();

        public Builder inputs(Iterable<? extends TransactionInput> inputs) {
            inputs.forEach(this.inputs::add);
            return this;
        }

        public Builder inputs(TransactionInput... inputs) {
            Collections.addAll(this.inputs, inputs);
            return this;
        }

        public Builder outputs(Iterable<? extends TransactionOutput> outputs) {
            outputs.forEach(this.outputs::add);
            return this;
        }

        public Builder outputs(TransactionOutput... outputs) {
            Collections.addAll(this.outputs, outputs);
            return this;
        }

        public Builder version(int v) {
            version = v;
            return this;
        }

        public Builder lockTime(int lt) {
            lockTime = lt;
            return this;
        }

        public Builder blockID(BID b) {
            blockID = b;
            return this;
        }

        public APITransaction build() {
            return new APITransaction(new Transaction(version, lockTime, inputs, outputs), blockID);
        }
    }

    /**
     * get hash of the block this transaction is embedded into. Note that this is not part of the protocol, but is filled by the server while retrieving a
     * transaction in context of a block A transaction alone might not have this filled.
     */
    public BID getBlockID() {
        return blockID;
    }

    /**
     * Create a protobuf message for the transaction as used to communicate with the server.
     */
    public BCSAPIMessage.TX toBCSAPIMessage() {
        BCSAPIMessage.TX.Builder builder = BCSAPIMessage.TX.newBuilder();
        builder.setLocktime(getLockTime());
        builder.setVersion(getVersion());
        for (TransactionInput i : getInputs()) {
            builder.addInputs(i.toProtobuf());
        }
        for (TransactionOutput o : getOutputs()) {
            builder.addOutputs(o.toProtobuf());
        }
        if (blockID != null) {
            builder.setBlock(ByteString.copyFrom(blockID.unsafeGetArray()));
        }
        return builder.build();
    }

    /**
     * Recreate the transaction object from a protobuf message
     */
    public static APITransaction fromProtobuf(BCSAPIMessage.TX pt) throws HyperLedgerException {
        APITransaction.Builder builder = new APITransaction.Builder();

        builder.lockTime(pt.getLocktime());
        builder.version(pt.getVersion());
        if (pt.getInputsCount() > 0) {
            for (BCSAPIMessage.TXIN i : pt.getInputsList()) {
                builder.inputs(TransactionInput.fromProtobuf(i));
            }
        }

        if (pt.getOutputsCount() > 0) {
            for (BCSAPIMessage.TXOUT o : pt.getOutputsList()) {
                builder.outputs(TransactionOutput.fromProtobuf(o));
            }
        }

        if (pt.hasBlock()) {
            builder.blockID(new BID(pt.getBlock().toByteArray()));
        }

        return builder.build();
    }
}
