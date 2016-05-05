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

/**
 * Outpoint is the location of a TransactionOutput identified by its enclosing Transaction's ID
 * and the index within the array of outputs, 0 is first.
 *
 * @see Transaction
 * @see TransactionOutput
 */
public class Outpoint {
    public static final Outpoint COINBASE = new Outpoint(TID.INVALID, 0);
    public static final Outpoint NULL = new Outpoint(TID.INVALID, -1);

    private final TID transactionId;
    private final int outputIndex;

    /**
     * Create an Outpoint supplying coordinates
     *
     * @param transactionId - ID of enclosing Transaction
     * @param outputIndex   - index within outputs of the holding transaction
     * @see Transaction
     */
    public Outpoint(TID transactionId, int outputIndex) {
        this.transactionId = transactionId;
        this.outputIndex = outputIndex;
    }

    public boolean isNull() {
        return outputIndex == -1 && transactionId.equals(TID.INVALID);
    }

    /**
     * return the enclosing Transaction ID
     *
     * @return id
     * @see Transaction
     */
    public TID getTransactionId() {
        return transactionId;
    }

    /**
     * return the index within the outputs of the enclosing Transaction
     *
     * @return index
     * @see Transaction
     */
    public int getOutputIndex() {
        return outputIndex;
    }

    /**
     * Serialize to Bitcoin wire format
     *
     * @return wire format byte array
     */
    public byte[] toWire() {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        try {
            writer.writeHash(transactionId);
            writer.writeUint32(outputIndex);
        } catch (IOException e) {
        }
        return writer.toByteArray();
    }

    @Override
    public String toString() {
        return transactionId.toString() + " [" + outputIndex + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Outpoint outpoint = (Outpoint) o;

        if (outputIndex != outpoint.outputIndex) return false;
        return !(transactionId != null ? !transactionId.equals(outpoint.transactionId) : outpoint.transactionId != null);
    }

    @Override
    public int hashCode() {
        int result = transactionId != null ? transactionId.hashCode() : 0;
        result = 31 * result + outputIndex;
        return result;
    }
}

