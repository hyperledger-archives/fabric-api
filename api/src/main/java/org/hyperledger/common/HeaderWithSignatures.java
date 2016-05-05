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

import java.io.IOException;

public class HeaderWithSignatures extends BitcoinHeader {
    private final Script inScript;
    private final byte[] nextScriptHash;

    public HeaderWithSignatures(int version, BID previousID, MerkleRoot merkleRoot, int createTime, int encodedDifficulty, int nonce, Script inScript, byte[] nextScriptHash) {
        super(version, previousID, merkleRoot, createTime, encodedDifficulty, nonce);
        this.inScript = inScript;
        this.nextScriptHash = nextScriptHash;
    }

    public static HeaderWithSignatures.Builder create() {
        return new HeaderWithSignatures.Builder();
    }

    public static class Builder extends BitcoinHeader.Builder {
        protected Script inScript;
        protected byte[] nextScriptHash;

        protected Builder() {
            super();
        }

        public Builder inScript(byte[] bytes) {
            this.inScript = new Script(bytes);
            return this;
        }

        public Builder nextScriptHash(byte[] hash) {
            this.nextScriptHash = hash;
            return this;
        }

        public HeaderWithSignatures build() {
            return new HeaderWithSignatures(version, previousID, merkleRoot, createTime, difficultyTarget, nonce, inScript, nextScriptHash);
        }
    }

    @Override
    public void toWireHeader(WireFormat.Writer writer) throws IOException {
        super.toWireHeader(writer);
        writer.writeVarBytes(inScript.toByteArray());
        writer.writeVarBytes(nextScriptHash);
    }

    public void toWireBitcoinHeader(WireFormat.Writer writer) throws IOException {
        super.toWireHeader(writer);
    }

    protected static Builder fromWire(Builder builder, WireFormat.Reader reader) throws IOException {
        BitcoinHeader.fromWire(builder, reader);
        return builder.inScript(reader.readVarBytes())
                .nextScriptHash(reader.readVarBytes());
    }

    public static HeaderWithSignatures fromWire(byte[] bytes) throws IOException {
        return fromWire(new WireFormat.Reader(bytes));
    }

    public static HeaderWithSignatures fromWire(WireFormat.Reader reader) throws IOException {
        return fromWire(new HeaderWithSignatures.Builder(), reader).build();
    }

    public Script getInScript() {
        return inScript;
    }

    public byte[] getNextScriptHash() {
        return nextScriptHash;
    }
}
