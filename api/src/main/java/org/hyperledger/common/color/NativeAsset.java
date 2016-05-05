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

import org.hyperledger.common.TID;
import org.hyperledger.common.Transaction;
import org.hyperledger.common.WireFormat;

import java.io.IOException;

/**
 * Represents a native asset, consensus enforced.  Attempts to follow Sidechain Elements design
 * of the same feature.
 */
public class NativeAsset implements Color {
    private final TID txid;
    private final int index;
    public static final int ENCODED_SIZE = 32 + 4;

    public NativeAsset(TID txid, int index) {
        this.txid = txid;
        this.index = index;
    }

    /**
     * Construct an newly-defined NativeAsset.
     * <p>
     * An asset that is being defined in the current transaction has empty txid
     * because the txid has covers the output where it is defined.
     */
    public NativeAsset(int index) {
        this.txid = TID.INVALID;
        this.index = index;
    }

    static public Color fromEncoded(byte[] encoded) throws IOException {
        WireFormat.Reader reader = new WireFormat.Reader(encoded);
        TID tid = new TID(reader.readBytes(32));
        int index = reader.readUint32();
        if (tid.equals(BITCOIN.getTxid()) && index == BITCOIN.getIndex())
            return BITCOIN;
        return new NativeAsset(tid, index);
    }

    public TID getTxid() {
        return txid;
    }

    public int getIndex() {
        return index;
    }

    public boolean isBeingDefined() {
        return txid.equals(TID.INVALID);
    }

    @Override
    public byte[] getEncoded() {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        try {
            writer.writeBytes(txid.toByteArray());
            writer.writeUint32(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("OutputAsset{id=%s/%02x}", txid, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NativeAsset)) return false;
        NativeAsset o = (NativeAsset) obj;
        return txid.equals(o.txid) && index == o.index;
    }

    @Override
    public int hashCode() {
        return txid.hashCode() + index;
    }

    /**
     * Make canonical by fililng in any missing transaction ID
     */
    @Override
    public Color toCanonical(Transaction tx) {
        if (isBeingDefined()) {
            return new NativeAsset(tx.getID(), index);
        } else {
            return this;
        }
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public boolean isToken() {
        return false;
    }
}
