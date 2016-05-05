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

import org.hyperledger.common.color.ColoredTransactionOutput;

import java.io.IOException;
import java.util.*;

/**
 * Wire formatter.  Currently only supports transaction formatting.
 */
public class WireFormatter {
    public enum WireFormatFlags {
        NATIVE_ASSET,
        SIGLESS_TID
    }

    private final Set<WireFormatFlags> flags;

    public static final WireFormatter bitcoin = new WireFormatter();


    public WireFormatter() {
        this.flags = new HashSet<>();
    }

    public WireFormatter(Collection<WireFormatFlags> flags) {
        this.flags = new HashSet<>();
        this.flags.addAll(flags);
    }

    public WireFormatter(WireFormatFlags... flags) {
        this.flags = new HashSet<>();
        for (WireFormatFlags flag : flags) {
            this.flags.add(flag);
        }
    }

    public boolean isNativeAssets() {
        return flags.contains(WireFormatFlags.NATIVE_ASSET);
    }

    /**
     * Serialize into wire format on P2P network
     *
     * @param writer a wire format writer
     * @throws IOException depends on output target
     */
    public void toWire(Transaction t, WireFormat.Writer writer) throws IOException {
        writer.writeUint32(t.getVersion());
        writer.writeVarInt(t.getInputs().size());
        for (TransactionInput input : t.getInputs()) {
            toWire(input, writer);
        }

        List<? extends TransactionOutput> outputs = t.getOutputs();
        toWire(outputs, writer);

        writer.writeUint32(t.getLockTime());
    }

    public void toTxID(Transaction t, WireFormat.Writer writer) throws IOException {
        writer.writeUint32(t.getVersion());
        writer.writeVarInt(t.getInputs().size());
        for (TransactionInput input : t.getInputs()) {
            toTxID(input, writer);
        }

        List<? extends TransactionOutput> outputs = t.getOutputs();
        toWire(outputs, writer);

        writer.writeUint32(t.getLockTime());
    }

    public void toSignature(Transaction t, WireFormat.Writer writer, SignatureOptions signatureOptions, TransactionOutput spend) throws IOException {
        writer.writeUint32(t.getVersion());
        writer.writeVarInt(t.getInputs().size());
        for (TransactionInput input : t.getInputs()) {
            toSignature(input, writer, signatureOptions, spend);
        }

        List<? extends TransactionOutput> outputs = t.getOutputs();
        toWire(outputs, writer);

        writer.writeUint32(t.getLockTime());
    }

    public void toWire(TransactionInput input, WireFormat.Writer writer) throws IOException {
        Outpoint source = input.getSource();
        if (source.getTransactionId() != null && !TID.INVALID.equals(source.getTransactionId())) {
            writer.writeHash(source.getTransactionId());
            writer.writeUint32(source.getOutputIndex());
        } else {
            writer.writeBytes(TID.INVALID.unsafeGetArray());
            writer.writeUint32(-1);
        }
        input.getScript().toWire(writer);
        writer.writeUint32(input.getSequence());
    }

    public void toTxID(TransactionInput input, WireFormat.Writer writer) throws IOException {
        Outpoint source = input.getSource();
        if (source.getTransactionId() != null && !TID.INVALID.equals(source.getTransactionId())) {
            writer.writeHash(source.getTransactionId());
            writer.writeUint32(source.getOutputIndex());
        } else {
            writer.writeBytes(TID.INVALID.unsafeGetArray());
            writer.writeUint32(-1);
        }
        if (!flags.contains(WireFormatFlags.SIGLESS_TID)) {
            input.getScript().toWire(writer);
        }
        writer.writeUint32(input.getSequence());
    }

    public void toSignature(TransactionInput input, WireFormat.Writer writer, SignatureOptions signatureOptions, TransactionOutput spend) throws IOException {
        Outpoint source = input.getSource();
        if (source.getTransactionId() != null && !TID.INVALID.equals(source.getTransactionId())) {
            writer.writeHash(source.getTransactionId());
            writer.writeUint32(source.getOutputIndex());
        } else {
            writer.writeBytes(TID.INVALID.unsafeGetArray());
            writer.writeUint32(-1);
        }
        if (spend instanceof ColoredTransactionOutput && !((ColoredTransactionOutput) spend).getColor().isToken()) {
            if (signatureOptions.contains(SignatureOptions.Option.SCIV)) {
                writer.writeUint64(((ColoredTransactionOutput) spend).getQuantity());
            }
            if (signatureOptions.contains(SignatureOptions.Option.SCIC)) {
                writer.writeBytes(((ColoredTransactionOutput) spend).getColor().getEncoded());
            }
        } else {
            if (signatureOptions.contains(SignatureOptions.Option.SCIV)) {
                writer.writeUint64(spend.getValue());
            }
        }
        input.getScript().toWire(writer);
        writer.writeUint32(input.getSequence());
    }

    public void toWire(List<? extends TransactionOutput> outputs, WireFormat.Writer writer) throws IOException {
        writer.writeVarInt(outputs.size());
        if (flags.contains(WireFormatFlags.NATIVE_ASSET)) {
            for (TransactionOutput output : outputs) {
                output.toWireNativeAsset(writer);
            }
        } else {
            for (TransactionOutput output : outputs) {
                output.toWire(writer);
            }
        }
    }

    /**
     * Dump as bytes
     *
     * @param t the transaction
     * @return the transaction in wire format as bytes
     */
    public byte[] toWireBytes(Transaction t) throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWire(t, writer);
        return writer.toByteArray();
    }

    /**
     * Dump in hex. Useful for unit debugging and tests only.
     *
     * @param t the transaction
     * @return the transaction in wire format as a hex string
     * @throws IOException only if memory is full
     */
    public String toWireDump(Transaction t) throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWire(t, writer);
        return ByteUtils.toHex(writer.toByteArray());
    }

    /**
     * Recreate the transaction from wire dump. Useful for test and debug.
     *
     * @param dump - hex dump
     * @return a transaction
     * @throws IOException on format errors
     */
    public Transaction fromWireDump(String dump) throws IOException {
        WireFormat.Reader reader = new WireFormat.Reader(ByteUtils.fromHex(dump));
        return fromWire(reader);
    }

    public Transaction fromWire(byte[] bytes) throws IOException {
        return fromWire(new WireFormat.Reader(bytes));
    }

    /**
     * Recreate a transaction from a wire format stream
     *
     * @param reader a wire format reader
     * @return Transaction
     * @throws IOException on format or I/O error
     */
    public Transaction fromWire(WireFormat.Reader reader) throws IOException {
        Transaction.Builder builder = new Transaction.Builder();

        builder = builder.version(reader.readUint32());
        long nin = reader.readVarInt();
        if (nin > 0) {
            List<TransactionInput> inputs = new ArrayList<>();
            for (int i = 0; i < nin; ++i) {
                inputs.add(TransactionInput.fromWire(reader));
            }
            builder = builder.inputs(inputs);
        }

        long nout = reader.readVarInt();
        if (nout > 0) {
            List<TransactionOutput> outputs = new ArrayList<>();
            if (flags.contains(WireFormatFlags.NATIVE_ASSET)) {
                for (int i = 0; i < nout; ++i) {
                    outputs.add(ColoredTransactionOutput.fromWireNativeAsset(reader));
                }
            } else {
                for (int i = 0; i < nout; ++i) {
                    outputs.add(TransactionOutput.fromWire(reader));
                }
            }
            builder = builder.outputs(outputs);
        }

        return builder.lockTime(reader.readUint32()).build();
    }
}
