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

import com.google.protobuf.ByteString;
import org.hyperledger.api.BCSAPIMessage;

import java.io.IOException;
import java.util.Objects;

/**
 * An input to a transaction.
 * It is a reference to the previous output plus as script that if prepended to
 * the output's script evaluates that to true.
 */
public class TransactionInput {
    private final Outpoint source;
    private final int sequence;
    private final Script script; // should be cons

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private Outpoint outpoint = Outpoint.COINBASE;
        private int sequence = -1;
        private Script script = new Script();

        public Builder source(Outpoint outpoint) {
            this.outpoint = outpoint;
            return this;
        }

        public Builder source(TID sourceTransactionID, int ix) {
            this.outpoint = new Outpoint(sourceTransactionID, ix);
            return this;
        }

        public Builder sequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder script(Script script) {
            this.script = script;
            return this;
        }

        public TransactionInput build() {
            return new TransactionInput(outpoint, sequence, script);
        }
    }

    /**
     * Construct an input from output location and script
     *
     * @param source   location of the referred transaction output
     * @param sequence - the transaction is final if all sequence number are -1, there are further rules though...
     * @param script   spending script, that depends on the output script referenced.
     */
    public TransactionInput(Outpoint source, int sequence, Script script) {
        Objects.requireNonNull(source, "Transaction source must not be null");
        Objects.requireNonNull(script, "Transaction input script must not be null");
        this.source = source;
        this.sequence = sequence;
        this.script = script;
    }

    /**
     * @return location of the referred output
     */
    public Outpoint getSource() {
        return source;
    }

    /**
     * @return transaction ID of the referred output
     */
    public TID getSourceTransactionID() {
        return source.getTransactionId();
    }

    /**
     * @return index of the output within the referred transaction
     */
    public int getOutputIndex() {
        return source.getOutputIndex();
    }

    /**
     * @return sequence number, usually -1
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * redemption script that if prepended to the referred output script evaluates to true
     *
     * @return a script
     */
    public Script getScript() {
        return script;
    }

    /**
     * materialize from wire format
     *
     * @param reader
     * @return a transaction input
     * @throws IOException on I/O or format errors
     */
    public static TransactionInput fromWire(WireFormat.Reader reader) throws IOException {
        return new Builder()
                .source(new Outpoint(new TID(reader.readHash()), reader.readUint32()))
                .script(Script.fromWire(reader))
                .sequence(reader.readUint32())
                .build();
    }

    /**
     * convert to client-server message. You wont need this.
     *
     * @return
     */
    public BCSAPIMessage.TXIN toProtobuf() {
        BCSAPIMessage.TXIN.Builder builder = BCSAPIMessage.TXIN.newBuilder();
        builder.setScript(ByteString.copyFrom(script.toByteArray()));
        builder.setSequence(sequence);
        builder.setSource(ByteString.copyFrom(source.getTransactionId().unsafeGetArray()));
        builder.setSourceix(source.getOutputIndex());
        return builder.build();
    }

    /**
     * convert from client-server message. You wont need this.
     *
     * @param pi
     * @return
     */
    public static TransactionInput fromProtobuf(BCSAPIMessage.TXIN pi) {

        return new TransactionInput(
                new Outpoint(new TID(pi.getSource().toByteArray()),
                        pi.getSourceix()),
                pi.getSequence(),
                new Script(pi.getScript().toByteArray())
        );
    }

    @Override
    public String toString() {
        return source.toString() + " " + script.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionInput that = (TransactionInput) o;

        if (sequence != that.sequence) return false;
        if (!source.equals(that.source)) return false;
        return script.equals(that.script);

    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + sequence;
        result = 31 * result + script.hashCode();
        return result;
    }
}
