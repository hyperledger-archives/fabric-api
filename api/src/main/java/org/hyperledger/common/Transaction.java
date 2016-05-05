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
package org.hyperledger.common;

import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.api.BCSAPIMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A transaction in the ledger.
 * A transaction reallocates inputs to outputs. Inputs are outputs of previous transactions.
 */
public class Transaction implements MerkleTreeNode {
    private final int version;
    private final int lockTime;
    private final List<? extends TransactionInput> inputs;
    private final List<? extends TransactionOutput> outputs;

    private TID ID;

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private int version = 1;
        private int lockTime = 0;
        private List<TransactionInput> inputs = new ArrayList<>();
        private List<TransactionOutput> outputs = new ArrayList<>();

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder lockTime(int lockTime) {
            this.lockTime = lockTime;
            return this;
        }

        public Builder inputs(Iterable<? extends TransactionInput> inputs) {
            inputs.forEach(this.inputs::add);
            return this;
        }

        public Builder inputs(TransactionInput... input) {
            Collections.addAll(this.inputs, input);
            return this;
        }

        public Builder outputs(Iterable<? extends TransactionOutput> outputs) {
            outputs.forEach(this.outputs::add);
            return this;
        }

        public Builder outputs(TransactionOutput... output) {
            Collections.addAll(this.outputs, output);
            return this;
        }

        public Transaction build() {
            return new Transaction(version, lockTime, inputs, outputs);
        }
    }

    public Transaction(int version, int lockTime, List<? extends TransactionInput> inputs, List<? extends TransactionOutput> outputs) {
        requireNonNull(inputs, "Transaction inputs must not be null");
        requireNonNull(outputs, "Transaction outputs must not be null");

        this.version = version;
        this.lockTime = lockTime;
        this.inputs = Collections.unmodifiableList(inputs);
        this.outputs = Collections.unmodifiableList(outputs);
        Hash h;
        try {
            WireFormat.HashWriter writer = new WireFormat.HashWriter();
            HyperLedgerSettings.getInstance().getTxWireFormatter().toTxID(this, writer);
            h = writer.hash();
        } catch (IOException e) {
            h = null;
        }
        ID = new TID(h);
    }

    public Transaction(int version, int lockTime, List<? extends TransactionInput> inputs, List<? extends TransactionOutput> outputs,
                       TID tid) {
        this(version, lockTime, inputs, outputs);
        ID = tid;
    }

    /**
     * @return 0 since Transaction is always the leaf of the Merkle Tree
     */
    @Override
    public int getMerkleHeight() {
        return 0;
    }

    /**
     * Transaction version number
     *
     * @return version
     */
    public int getVersion() {
        return version;
    }

    /**
     * The transaction can not be in a block before the time point here.
     * In Bitcoin main net lock time is interpreted as seconds in the Unix era if greater than 500000000
     * otherwise it is block height.
     *
     * @return
     */
    public int getLockTime() {
        return lockTime;
    }

    /**
     * @return unmutable list of transaction inputs
     */
    public List<? extends TransactionInput> getInputs() {
        return inputs;
    }

    /**
     * Get the input with index
     *
     * @param inputIndex input index
     * @return a transaction input
     */
    public TransactionInput getInput(int inputIndex) {
        return inputs.get(inputIndex);
    }

    /**
     * Get the location of the consumed TransactionOutput
     *
     * @param inputIndex
     * @return output location
     */
    public Outpoint getSource(int inputIndex) {
        return inputs.get(inputIndex).getSource();
    }

    /**
     * Get the list of outpoints the transaction consumes
     *
     * @return outpoints
     */
    public List<Outpoint> getSources() {
        List<Outpoint> outs = new ArrayList<>();
        for (TransactionInput input : inputs) {
            outs.add(input.getSource());
        }
        return outs;
    }


    /**
     * Get the output with index
     *
     * @param outputIndex
     * @return a transaction output
     */
    public TransactionOutput getOutput(int outputIndex) {
        return outputs.get(outputIndex);
    }

    /**
     * get an immutable list of transaction outputs
     *
     * @return output list
     */
    public List<? extends TransactionOutput> getOutputs() {
        return outputs;
    }

    /**
     * get the Transaction ID
     *
     * @return id
     */
    public TID getID() {
        return ID;
    }

    /**
     * Get the coins this transaction offers, that is its outputs with their location.
     *
     * @return list of coins
     */
    public List<Coin> getCoins() {
        ArrayList<Coin> coins = new ArrayList<>();
        for (int i = 0; i < outputs.size(); ++i)
            coins.add(outputs.get(i).toCoin(this, i));
        return coins;
    }

    /**
     * Get the output coin with index
     *
     * @param outputIndex
     * @return a coin
     */
    public Coin getCoin(int outputIndex) {
        return new Coin(new Outpoint(getID(), outputIndex), getOutput(outputIndex));
    }

    /**
     * Decides if this is a coinbase transaction.
     *
     * @return
     */
    public boolean isCoinBase() {
        return getInputs().size() == 1 && getInput(0).getSource().getTransactionId().equals(TID.INVALID);
    }

    /**
     * Convert to a client-server message. You won't call this.
     *
     * @return
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
        return builder.build();
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Transaction) {
            return ID.equals(((Transaction) obj).ID);
        }
        return false;
    }

    @Override
    public String toString() {
        return ID.toString();
    }

    public String dump() {
        StringBuilder builder = new StringBuilder();
        builder.append("TX(");
        builder.append("version=").append(version);
        builder.append(" locktime=").append(lockTime);
        builder.append(" inputs=[\n");
        for (TransactionInput input : inputs) {
            builder.append("    ").append(input.toString()).append("\n");
        }
        builder.append("]\n outputs=[\n");
        for (TransactionOutput output : outputs) {
            builder.append("    ").append(output.toString()).append("\n");
        }
        builder.append("])\n");
        return builder.toString();
    }
}
