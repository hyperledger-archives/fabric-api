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
package org.hyperledger.transaction;

import org.hyperledger.common.Hash;
import org.hyperledger.common.PublicKey;
import org.hyperledger.merkletree.MerkleTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Transaction implements MerkleTreeNode {
    private static final Logger log = LoggerFactory.getLogger(Transaction.class);

    private final TID ID;
    private final byte[] payload;
    private final List<Endorser> endorsers;

    public Transaction(byte[] payload, List<Endorser> endorsers) {
        this.payload = payload;
        this.endorsers = endorsers;
        this.ID = new TID(Hash.of(toByteArray()));
    }

    public Transaction(Transaction t) {
        payload = t.payload;
        endorsers = t.endorsers;
        ID = t.ID;
    }

    /**
     * @return 0 since Transaction is always the leaf of the Merkle Tree
     */
    @Override
    public int getMerkleHeight() {
        return 0;
    }


    public TID getID() {
        return ID;
    }

    public byte[] getPayload() {
        return payload;
    }

    public List<Endorser> getEndorsers() {
        return endorsers;
    }

    /**
     * Verifies if the endorser signed the transaction with the private pair
     * of the provided public key.
     */
    public boolean verify(Endorser endorser, PublicKey key) {
        byte[] hash = Hash.of(payload).toByteArray();
        return endorser.verify(hash, key);
    }

    public byte[] toByteArray() {
        try {
            return toByteArray(payload, endorsers);
        } catch (IOException e) {
            log.error("Failed to serialize transaction {}: {}", ID, e.getMessage());
            return new byte[0];
        }
    }

    public static byte[] toByteArray(byte[] payload, List<Endorser> endorsers) throws IOException {
        List<ByteBuffer> endorserBytes = endorsers.stream()
                .map(endorser -> ByteBuffer.wrap(endorser.getSignature()))
                .collect(toList());

        SerializedTransaction t = SerializedTransaction.newBuilder()
                .setPayload(ByteBuffer.wrap(payload))
                .setEndorsers(endorserBytes)
                .build();

        return AvroSerializer.serialize(t);
    }

    public static Transaction fromByteArray(byte[] array) throws IOException {
        SerializedTransaction t = AvroSerializer.deserialize(array, SerializedTransaction.getClassSchema());

        List<Endorser> endorsers = t.getEndorsers().stream()
                .map(endorser -> new Endorser(endorser.array()))
                .collect(toList());

        return new TransactionBuilder()
                .payload(t.getPayload().array())
                .endorsers(endorsers)
                .build();
    }

}
