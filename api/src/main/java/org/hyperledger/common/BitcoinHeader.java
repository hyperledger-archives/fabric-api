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
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public class BitcoinHeader implements Header {
    private BID ID;
    private final int version;
    private final BID previousID;
    private final MerkleRoot merkleRoot;
    private final int createTime;
    private final int encodedDifficulty;
    private final int nonce;

    public BitcoinHeader(int version, BID previousID, MerkleRoot merkleRoot, int createTime, int encodedDifficulty, int nonce) {
        this.version = version;
        this.previousID = previousID;
        this.merkleRoot = merkleRoot;
        this.createTime = createTime;
        this.encodedDifficulty = encodedDifficulty;
        this.nonce = nonce;
    }

    public static BitcoinHeader.Builder create() {
        return new BitcoinHeader.Builder();
    }

    public static class Builder extends HeaderBuilder<Builder> {
        protected Builder() {
            super();
        }

        public BitcoinHeader build() {
            return new BitcoinHeader(version, previousID, merkleRoot, createTime, difficultyTarget, nonce);
        }
    }

    /**
     * The ID of the header. It is technically a cryptographic hash of the header's content.
     * For Bitcoin it is also the proof of work, see getEncodedDifficulty
     *
     * @return unique ID
     */
    @Override
    public BID getID() {
        if (ID == null) {
            Hash h = null;
            WireFormat.HashWriter writer;
            try {
                writer = new WireFormat.HashWriter();
                toWireHeader(writer);
                h = writer.hash();
            } catch (IOException e) {
            }
            ID = new BID(h);
        }
        return ID;
    }

    /**
     * The header version
     *
     * @return - version
     */
    @Override
    public int getVersion() {
        return version;
    }

    /**
     * The ID of the previos header, these liks create the block chain
     *
     * @return
     */
    @Override
    public BID getPreviousID() {
        return previousID;
    }

    /**
     * The merkle root of transaction within the associated block (@Link https://en.wikipedia.org/wiki/Merkle_tree)
     *
     * @return the merkle root of transaction hashes
     */
    @Override
    public MerkleRoot getMerkleRoot() {
        return merkleRoot;
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
    @Override
    @Deprecated
    public int getCreateTime() {
        return createTime;
    }

    /**
     * @return the time point of the block was created as observed in local time
     */
    @Override
    public LocalTime getLocalCreateTime() {
        return LocalTime.from(Instant.ofEpochSecond(Integer.toUnsignedLong(createTime)).atZone(ZoneId.of("Z")));
    }

    /**
     * The difficulty of proof-of-work (POW). This is a big integer encoded in a 32 bits.
     * The blocks's ID if also interpreted as a big integer must be lower than this.
     * <p>
     * For the curious, POW is valid if:
     * <code>
     * getID().toBigInteger().compareTo(BigInteger.valueOf(getEncodedDifficulty() & 0x7fffffL).shiftLeft((int) (8 * ((getEncodedDifficulty() >>> 24) - 3)))) <= 0
     * </code>
     *
     * @return the encoded POW difficulty
     */
    @Override
    public int getEncodedDifficulty() {
        return encodedDifficulty;
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
    @Override
    public int getNonce() {
        return nonce;
    }

    public byte[] toWireHeaderBytes() throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWireHeader(writer);
        return writer.toByteArray();
    }


    /**
     * Serialize a header in P2P wire format
     *
     * @param writer a serializer
     * @throws IOException
     */
    @Override
    public void toWireHeader(WireFormat.Writer writer) throws IOException {
        writer.writeUint32(version);
        writer.writeHash(previousID);
        writer.writeHash(merkleRoot);
        writer.writeUint32(createTime);
        writer.writeUint32(encodedDifficulty);
        writer.writeUint32(nonce);
    }

    public static BitcoinHeader fromWire(byte[] bytes) throws IOException {
        return fromWire(new WireFormat.Reader(bytes));
    }

    /**
     * Reconstruct a header from P2P wire format
     *
     * @param reader a deserializer
     * @throws IOException
     */
    public static BitcoinHeader fromWire(WireFormat.Reader reader) throws IOException {
        return fromWire(new BitcoinHeader.Builder(), reader).build();
    }

    protected static Builder fromWire(BitcoinHeader.Builder builder, WireFormat.Reader reader) throws IOException {
        return builder
                .version(reader.readUint32())
                .previousID(new BID(reader.readHash()))
                .merkleRoot(new MerkleRoot(reader.readHash()))
                .createTime(reader.readUint32())
                .difficultyTarget(reader.readUint32())
                .nonce(reader.readUint32());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitcoinHeader header = (BitcoinHeader) o;
        return Objects.equals(getID(), header.getID());
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    @Override
    public String toString() {
        return getID().toString();
    }

    public byte[] toByteArray() throws IOException {
        WireFormat.ArrayWriter writer = new WireFormat.ArrayWriter();
        toWireHeader(writer);
        return writer.toByteArray();
    }

}
