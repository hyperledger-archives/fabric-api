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
import org.hyperledger.HyperLedgerSettings;
import org.hyperledger.api.BCSAPIMessage;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A block of the ledger.
 * It consists a BitcoinHeader and a list of Transaction
 * <p>
 * Some or all transactions might have been pruned, that is removed and replaced with MerkleTree nodes
 * that prove that the merkle tree (@Link https://en.wikipedia.org/wiki/Merkle_tree) root of the block header is valid even if some transactions are no
 * longer available.
 *
 * @see BitcoinHeader
 * @see Transaction
 * @see MerkleTreeNode
 */
public class Block {
    private final Header header;
    private List<? extends MerkleTreeNode> nodes;
    private List<Transaction> transactions;

    /**
     * Create a block from a header and transaction list.
     * The transaction list might be pruned, hence it is a list of MerkleTreeNodes
     *
     * @param header       - the header
     * @param transactions - the potentially transaction list
     */
    @SuppressWarnings("unchecked")
    public Block(Header header, List<? extends MerkleTreeNode> transactions) {
        this.header = header;
        this.nodes = Collections.unmodifiableList(transactions);
        int pruned = 0;
        for (MerkleTreeNode n : transactions) {
            if (!(n instanceof Transaction))
                ++pruned;
        }
        if (pruned > 0) {
            this.transactions = new ArrayList<>(nodes.size() - pruned);
            for (MerkleTreeNode n : transactions) {
                if (n instanceof Transaction)
                    this.transactions.add((Transaction) n);
            }
            this.transactions = Collections.unmodifiableList(this.transactions);
        } else {
            this.transactions = (List<Transaction>) nodes;
        }
    }

    /**
     * @return - true if some transactions were pruned.
     */
    public boolean isPruned() {
        return transactions != nodes;
    }

    /**
     * create a Block builder.
     *
     * @return builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * block builder helper class
     */
    public static class Builder {
        private static final Header INVALID_HEADER = ((BitcoinHeader.Builder) BitcoinHeader.create().previousID(BID.INVALID)).build();
        protected List<Transaction> transactions = new ArrayList<>();
        private Header header = INVALID_HEADER;

        public Builder transactions(Iterable<Transaction> transactions) {
            transactions.forEach(this.transactions::add);
            return this;
        }

        public Builder transactions(Transaction... transactions) {
            Collections.addAll(this.transactions, transactions);
            return this;
        }

        public Builder header(Header header) {
            this.header = header;
            return this;
        }

        public Block build() {
            if (MerkleRoot.INVALID.equals(header.getMerkleRoot())) {
                // instanceof check must happen with subclass first
                if (header instanceof HeaderWithSignatures) {
                    HeaderWithSignatures hws = (HeaderWithSignatures) header;
                    header = new HeaderWithSignatures(hws.getVersion(), hws.getPreviousID(), MerkleTree.computeMerkleRoot(transactions), hws.getCreateTime(), hws.getEncodedDifficulty(), hws.getNonce(), hws.getInScript(), hws.getNextScriptHash());
                } else {
                    header = new BitcoinHeader(header.getVersion(), header.getPreviousID(), MerkleTree.computeMerkleRoot(transactions), header.getCreateTime(), header.getEncodedDifficulty(), header.getNonce());
                }
            }
            return new Block(header, transactions);
        }
    }

    /**
     * @return the block header of this block
     */
    public Header getHeader() {
        return header;
    }

    /**
     * @return the block's unique id, that is identical to its header's id
     */
    public BID getID() {
        return header.getID();
    }

    /**
     * @return block version
     */
    public int getVersion() {
        return header.getVersion();
    }

    /**
     * @return pointer to the previous block, this creates the block chain
     */
    public BID getPreviousID() {
        return header.getPreviousID();
    }

    /**
     * The merkle root of transaction within this block (@Link https://en.wikipedia.org/wiki/Merkle_tree)
     *
     * @return the merkle root of transaction hashes
     */
    public Hash getMerkleRoot() {
        return header.getMerkleRoot();
    }

    /**
     * Unfortunately Satoshi used a 32bit unsigned integer for time as seconds in the Unix era.
     * This will turn negative in java's integer in 2038 (@Link https://en.wikipedia.org/wiki/Year_2038_problem)
     * and will ultimately overflow in 2106.
     * <p>
     * For above reasons, do not use this method for application purposes, but the getLocalCreateTime that
     * will ensure seemless transition as block header format eventually changes, hopefully before 2106.
     *
     * @return the time point the block was created. This is seconds in the Unix era.
     */
    @Deprecated
    public int getCreateTime() {
        return header.getCreateTime();
    }

    /**
     * @return the time point of the block was created
     */
    @SuppressWarnings("deprecation")
    public LocalTime getLocalCreateTime() {
        return header.getLocalCreateTime();
    }

    /**
     * The difficulty of proof-of-work (POW). This is a big integer encoded in a 32 bits.
     * The blocks's hash if also interpreted as a big integer must be lower than this.
     * <p>
     * For the curious, POW is valid if:
     * <code>
     * getID().toBigInteger().compareTo(BigInteger.valueOf(getEncodedDifficulty() & 0x7fffffL).shiftLeft((int) (8 * ((getEncodedDifficulty() >>> 24) - 3)))) <= 0
     * </code>
     *
     * @return the encoded POW difficulty
     */
    public int getDifficultyTarget() {
        return header.getEncodedDifficulty();
    }

    /**
     * Nonce for the miner that performs the POW. Unfortunately Satoshi used a 32 bit integer for the purpose,
     * that is insufficient at least since 2012. Miner that work faster than a few GH/s must also roll some
     * other content of the header, preferably including new transactions that change the merkle root.
     * The insufficient size of this is the reason for the inaccuracy of the time stamp as many miner misuse
     * the time stamp for an extension of this nonce.
     *
     * @return nonce - no meaning besides quantifying Satoshi's fortune, see @Link https://bitslog.wordpress.com/2013/04/17/the-well-deserved-fortune-of-satoshi-nakamoto/
     */
    public int getNonce() {
        return header.getNonce();
    }

    /**
     * Transactions of the block. This might not be a complete list if the block is pruned.
     *
     * @return immutable transaction list
     */
    public List<? extends Transaction> getTransactions() {
        return transactions;
    }

    /**
     * direct access to a transaction by its index in the block
     *
     * @param i - index of the transaction within the block. 0 is coin base
     * @return
     */
    public Transaction getTransaction(int i) {
        return transactions.get(i);
    }

    /**
     * Merkle tree nodes in the block. Nodes might be transactions or merkle tree nodes that were computed
     * out of transactions already pruned.
     *
     * @return immutable list of merkle tree nodes
     * @see MerkleTreeNode
     * @see MerkleTree
     */
    public List<? extends MerkleTreeNode> getMerkleTreeNodes() {
        return nodes;
    }

    /**
     * Get hexadecimal serialization of the on-wire block. Useful for tests only.
     *
     * @return hex string
     * @throws IOException
     */
    public String toWireDump() throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWire(writer);
        return ByteUtils.toHex(writer.toByteArray());
    }

    /**
     * Reconstruct a block from a hexadecimal string that is on-wire format. Useful for tests only.
     *
     * @param s a hex string
     * @return a block creaked
     * @throws HyperLedgerException - if the string is not a valid block (only in the sense of wire format)
     */
    public static <T extends BitcoinHeader> Block fromWireDump(String s, WireFormatter formatter, Class<T> c) throws HyperLedgerException {
        try {
            return Block.fromWire(new WireFormat.Reader(ByteUtils.fromHex(s)), formatter, c);
        } catch (IOException e) {
            throw new HyperLedgerException(e);
        }
    }

    public byte[] toWireBytes() throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWire(writer);
        return writer.toByteArray();
    }

    /**
     * Serialize a header in P2P wire format
     *
     * @param writer a serializer
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void toWire(WireFormat.Writer writer) throws IOException {
        WireFormatter formatter = HyperLedgerSettings.getInstance().getTxWireFormatter();
        if (isPruned())
            throw new IOException("No wire format defined for pruned blocks");
        header.toWireHeader(writer);
        writer.writeVarInt(nodes.size());
        for (MerkleTreeNode t : nodes) {
            formatter.toWire((Transaction) t, writer);
        }
    }

    public static <T extends BitcoinHeader> Block fromWire(byte[] bytes, WireFormatter formatter, Class<T> c) throws IOException, HyperLedgerException {
        return fromWire(new WireFormat.Reader(bytes), formatter, c);
    }

    /**
     * Reconstruct a header from P2P wire format
     *
     * @param reader a deserializer
     * @throws IOException
     */
    public static <T extends BitcoinHeader> Block fromWire(WireFormat.Reader reader, WireFormatter formatter, Class<T> type) throws HyperLedgerException, IOException {
        Header header;
        // subclass must be checked first
        if (HeaderWithSignatures.class.isAssignableFrom(type)) {
            header = HeaderWithSignatures.fromWire(reader);
        } else if (BitcoinHeader.class.isAssignableFrom(type)) {
            header = BitcoinHeader.fromWire(reader);
        } else {
            throw new IllegalArgumentException("Unsupported header type: " + type);
        }
        long nt = reader.readVarInt();
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < nt; ++i) {
            transactions.add(formatter.fromWire(reader));
        }
        MerkleRoot calculatedMerkleRoot = MerkleTree.computeMerkleRoot(transactions);
        if (!header.getMerkleRoot().equals(calculatedMerkleRoot)) {
            String s = String.format("Transaction list not consistent with merkle root in header, prevHash: %s, txCount: %d, merkleRoot: %s, calculatedMerkleRoot: %s", header.getPreviousID(), nt, header.getMerkleRoot(), calculatedMerkleRoot);
            throw new HyperLedgerException(s);
        }
        return new Block(header, transactions);
    }

    /**
     * Convert to server message.
     *
     * @return a serialization between client and server.
     */
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
                builder.addTransactions(BCSAPIMessage.MerkleNode.newBuilder().setHash(
                        ByteString.copyFrom(n.getID().unsafeGetArray())
                ));
            }
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Block) {
            return getID().equals(((Block) o).getID());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    @Override
    public String toString() {
        return getID().toString();
    }
}
