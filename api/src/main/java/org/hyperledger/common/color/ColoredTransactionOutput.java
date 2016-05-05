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
package org.hyperledger.common.color;

import com.google.protobuf.ByteString;
import org.hyperledger.api.BCSAPIMessage;
import org.hyperledger.common.*;

import java.io.IOException;

/**
 * A transaction output with added color information
 */
public class ColoredTransactionOutput extends TransactionOutput {

    private final Color color;
    private final long quantity;

    public static Builder create() {
        return new Builder();
    }

    public static class Builder extends TransactionOutput.Builder {
        private Color color = Color.BITCOIN;
        private long quantity;

        @Override
        public Builder value(long value) {
            return (Builder) super.value(value);
        }

        @Override
        public Builder script(Script script) {
            return (Builder) super.script(script);
        }

        @Override
        public Builder payTo(Address address) throws HyperLedgerException {
            return (Builder) super.payTo(address);
        }

        @Override
        public Builder data(byte[] data) throws HyperLedgerException {
            return (Builder) super.data(data);
        }

        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        public Builder burn() {
            script = Script.create().burn().build();
            return this;
        }

        public Builder quantity(long quantity) {
            this.quantity = quantity;
            return this;
        }

        public ColoredTransactionOutput build() {
            return new ColoredTransactionOutput(value, script, color, quantity);
        }
    }

    public ColoredTransactionOutput(long value, Script script) {
        super(value, script);
        color = Color.BITCOIN;
        quantity = value;
    }

    public ColoredTransactionOutput(TransactionOutput o, Color color, long quantity) {
        super(o.getValue(), o.getScript());
        this.color = color;
        this.quantity = quantity;
    }

    public ColoredTransactionOutput(long value, Script script, Color color, long quantity) {
        super(value, script);
        this.color = color;
        this.quantity = quantity;
    }

    public Color getColor() {
        return color;
    }

    public long getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "ColoredTransactionOutput{" +
                "color=" + color +
                ", quantity=" + quantity +
                "} " + super.toString();
    }

    public BCSAPIMessage.TXOUT toProtobuf() {
        BCSAPIMessage.TXOUT.Builder builder = BCSAPIMessage.TXOUT.newBuilder();
        builder.setScript(ByteString.copyFrom(getScript().toByteArray()));
        builder.setValue(getValue());
        builder.setColor(ByteString.copyFrom(getColor().getEncoded()));
        builder.setHasColor(true);
        builder.setQuantity(getQuantity());
        return builder.build();
    }

    @Override
    public void toWire(WireFormat.Writer writer) throws IOException {
        writer.writeUint64(value);
        script.toWire(writer);
    }

    @Override
    public void toWireNativeAsset(WireFormat.Writer writer) throws IOException {
        toWireNativeAsset(writer, color, value, quantity, script);
    }

    public static void toWireNativeAsset(WireFormat.Writer writer, Color color, long value, long quantity, Script script) throws IOException {
        if (!color.isNative())
            throw new UnsupportedOperationException("color is not a native asset");
        if (color.isToken())
            writer.writeUint64(value);
        else
            writer.writeUint64(quantity);
        writer.writeBytes(color.getEncoded());
        script.toWire(writer);
    }

    public static ColoredTransactionOutput fromWireNativeAsset(WireFormat.Reader reader) throws IOException {
        long value = reader.readUint64();
        Color color = NativeAsset.fromEncoded(reader.readBytes(NativeAsset.ENCODED_SIZE));
        Builder builder =
                new Builder()
                        .color(color)
                        .script(Script.fromWire(reader));
        if (color.isToken())
            builder.value(value);
        else
            builder.quantity(value);
        return builder.build();
    }

    @Override
    public Coin toCoin(Transaction tx, int i) {
        // Ensure all colors are canonical (i.e. native assets have TID filled in)
        return new Coin(new Outpoint(tx.getID(), i), toCanonical(tx));
    }

    private TransactionOutput toCanonical(Transaction tx) {
        return new ColoredTransactionOutput(value, script, color.toCanonical(tx), quantity);
    }

    @Override
    public ColoredTransactionOutput toColor() {
        return this;
    }
}
