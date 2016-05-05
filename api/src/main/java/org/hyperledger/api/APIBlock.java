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
package org.hyperledger.api;

import com.google.protobuf.ByteString;
import org.hyperledger.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class APIBlock extends Block {

    public APIBlock(APIHeader header, List<? extends MerkleTreeNode> transactions) {
        super(header, transactions);
    }

    public static class Builder {
        int height;

        protected int version;
        protected BID previousHash;
        protected MerkleRoot merkleRoot;
        protected int createTime;
        protected int difficultyTarget;
        protected int nonce;
        protected List<MerkleTreeNode> transactions = new ArrayList<>();

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder previousHash(BID previousHash) {
            this.previousHash = previousHash;
            return this;
        }

        public Builder merkleRoot(MerkleRoot merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public Builder createTime(int createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder difficultyTarget(int difficultyTarget) {
            this.difficultyTarget = difficultyTarget;
            return this;
        }

        public Builder nonce(int nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder transactions(Iterable<APITransaction> transactions) {
            transactions.forEach(this.transactions::add);
            return this;
        }

        public Builder transactions(APITransaction... transactions) {
            Collections.addAll(this.transactions, transactions);
            return this;
        }

        public Builder merkleNodes(MerkleTreeNode... nodes) {
            Collections.addAll(this.transactions, nodes);
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public APIBlock build() {
            if (merkleRoot == null)
                return new APIBlock(
                        new APIHeader(
                                new BitcoinHeader(version, previousHash, MerkleTree.computeMerkleRoot(transactions), createTime, difficultyTarget, nonce),
                                height), transactions);
            else
                return new APIBlock(
                        new APIHeader(
                                new BitcoinHeader(version, previousHash, merkleRoot, createTime, difficultyTarget, nonce),
                                height), transactions);
        }
    }

    public int getHeight() {
        return getHeader().getHeight();
    }

    @Override
    @SuppressWarnings("unchecked")
    public APIHeader getHeader() {
        return (APIHeader) super.getHeader();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends APITransaction> getTransactions() {
        return (List<? extends APITransaction>) super.getTransactions();
    }

    @SuppressWarnings("deprecation")
    public BCSAPIMessage.BLK toBCSAPIMessage() {
        BCSAPIMessage.BLK.Builder builder = BCSAPIMessage.BLK.newBuilder();
        builder.setVersion(getVersion());
        builder.setDifficulty(getDifficultyTarget());
        builder.setNonce(getNonce());
        builder.setTimestamp(getCreateTime());
        builder.setMerkleRoot(ByteString.copyFrom(getMerkleRoot().unsafeGetArray()));
        builder.setPreviousBlock(ByteString.copyFrom(getPreviousID().unsafeGetArray()));
        for (MerkleTreeNode n : getMerkleTreeNodes()) {
            if (n instanceof Transaction) {
                builder.addTransactions(BCSAPIMessage.MerkleNode.newBuilder().setTransaction(
                        ((Transaction) n).toBCSAPIMessage()
                ));
            } else {
                byte[] hash = Arrays.copyOf(n.getID().unsafeGetArray(), 33);
                hash[32] = (byte) (n.getMerkleHeight() & 0xff);
                builder.addTransactions(BCSAPIMessage.MerkleNode.newBuilder()
                        .setHash(ByteString.copyFrom(hash)));
            }
        }
        if (getHeight() >= 0) {
            builder.setHeight(getHeight());
        }
        return builder.build();
    }

    public static APIBlock fromProtobuf(BCSAPIMessage.BLK pb) throws HyperLedgerException {
        APIBlock.Builder builder = new APIBlock.Builder();
        builder.version(pb.getVersion());
        builder.difficultyTarget(pb.getDifficulty());
        builder.nonce(pb.getNonce());
        builder.createTime(pb.getTimestamp());
        builder.previousHash(BID.createFromSafeArray(pb.getPreviousBlock().toByteArray()));
        builder.merkleRoot(MerkleRoot.createFromSafeArray(pb.getMerkleRoot().toByteArray()));
        if (pb.getTransactionsCount() > 0) {
            builder.transactions(new ArrayList<>());
            for (BCSAPIMessage.MerkleNode n : pb.getTransactionsList()) {
                if (n.hasHash()) {
                    byte[] hash = Arrays.copyOf(n.getHash().toByteArray(), 33);
                    builder.merkleNodes(new PrunedNode(Hash.createFromSafeArray(Arrays.copyOf(hash, 32)), hash[33]));
                } else
                    builder.transactions(APITransaction.fromProtobuf(n.getTransaction()));
            }
        }
        if (pb.hasHeight()) {
            builder.height(pb.getHeight());
        }
        return builder.build();
    }
}
