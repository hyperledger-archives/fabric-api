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
import org.hyperledger.common.color.Color;
import org.hyperledger.common.color.ColoredTransactionOutput;

import java.io.IOException;
import java.util.Objects;

public class TransactionOutput {
    protected final long value;
    protected final Script script;

    public TransactionOutput(long value, Script script) {
        Objects.requireNonNull(script, "Transaction output script must not be null");
        this.value = value;
        this.script = script;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        protected long value;
        protected Script script = new Script(new byte[0]);

        public Builder value(long value) {
            this.value = value;
            return this;
        }

        public Builder script(Script script) {
            this.script = script;
            return this;
        }

        public Builder payTo(Address address) throws HyperLedgerException {
            script = address.getAddressScript();
            return this;
        }

        public Builder data(byte[] data) throws HyperLedgerException {
            script = Script.data(data);
            return this;
        }

        public Builder burn() {
            script = Script.create().burn().build();
            return this;
        }

        public TransactionOutput build() {
            return new TransactionOutput(value, script);
        }
    }

    public void toWire(WireFormat.Writer writer) throws IOException {
        writer.writeUint64(value);
        script.toWire(writer);
    }

    public void toWireNativeAsset(WireFormat.Writer writer) throws IOException {
        ColoredTransactionOutput.toWireNativeAsset(writer, Color.BITCOIN, value, 0, script);
    }

    public static TransactionOutput fromWire(WireFormat.Reader reader) throws IOException {
        return new Builder()
                .value(reader.readUint64())
                .script(Script.fromWire(reader)).build();
    }

    public Address getOutputAddress() {
        return script.getAddress();
    }

    public long getValue() {
        return value;
    }

    public Script getScript() {
        return script;
    }

    public BCSAPIMessage.TXOUT toProtobuf() {
        BCSAPIMessage.TXOUT.Builder builder = BCSAPIMessage.TXOUT.newBuilder();
        builder.setScript(ByteString.copyFrom(script.toByteArray()));
        builder.setValue(value);
        return builder.build();
    }

    public static TransactionOutput fromProtobuf(BCSAPIMessage.TXOUT po) throws HyperLedgerException {
        if (po.hasColor()) {
            return new ColoredTransactionOutput(po.getValue(),
                    new Script(po.getScript().toByteArray()),
                    Color.fromEncoded(po.getColor().toByteArray()), po.getQuantity());
        } else
            return new TransactionOutput(po.getValue(),
                    new Script(po.getScript().toByteArray()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionOutput that = (TransactionOutput) o;

        if (value != that.value) return false;
        return script.equals(that.script);

    }

    @Override
    public int hashCode() {
        int result = (int) (value ^ (value >>> 32));
        result = 31 * result + script.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TransactionOutput{" +
                "value=" + value +
                ", script=" + script +
                '}';
    }

    public Coin toCoin(Transaction tx, int i) {
        return new Coin(tx, i);
    }

    public ColoredTransactionOutput toColor() {
        return new ColoredTransactionOutput(getValue(), getScript());
    }
}
