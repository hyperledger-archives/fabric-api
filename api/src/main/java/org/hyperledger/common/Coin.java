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

/**
 * Represents an output of a transaction with its precise location
 */
public class Coin {
    private final Outpoint outpoint;
    private final TransactionOutput output;

    /**
     * Create a coin with source and content.
     *
     * @param outpoint - the source of the coin
     * @param output   - the output that gives the coin a value and defines how it can be spent
     */
    public Coin(Outpoint outpoint, TransactionOutput output) {
        this.outpoint = outpoint;
        this.output = output;
    }

    /**
     * Create a coin of a transaction
     *
     * @param transaction - the transaction of the coin
     * @param outputIndex - the index of the coin within the transaction's outputs. 0 is first
     */
    public Coin(Transaction transaction, int outputIndex) {
        this(new Outpoint(transaction.getID(), outputIndex), transaction.getOutput(outputIndex));
    }

    /**
     * The source location of the coin.
     *
     * @return a coint location as outpoint
     */
    public Outpoint getOutpoint() {
        return outpoint;
    }

    /**
     * The transaction output of the coin. Defines its value and how to spend.
     *
     * @return transaction output
     */
    public TransactionOutput getOutput() {
        return output;
    }

    @Override
    public String toString() {
        return "Coin{" +
                "outpoint=" + outpoint +
                ", output=" + output +
                '}';
    }
}
