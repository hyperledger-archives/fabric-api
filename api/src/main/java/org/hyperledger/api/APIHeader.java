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

import java.io.IOException;
import java.time.LocalTime;

public class APIHeader implements Header {
    private final int height;
    private final Header header;

    public APIHeader(Header header, int height) {
        this.header = header;
        this.height = height;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder extends HeaderBuilder<Builder> {
        private int height = 0;

        private Builder() {
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public APIHeader build() {
            return new APIHeader(new BitcoinHeader(version, previousID, merkleRoot, createTime, difficultyTarget, nonce), height);
        }
    }

    public int getHeight() {
        return height;
    }

    public BCSAPIMessage.BLK toProtobuf() {
        BCSAPIMessage.BLK.Builder builder = BCSAPIMessage.BLK.newBuilder();
        builder.setVersion(getVersion());
        builder.setDifficulty(getEncodedDifficulty());
        builder.setNonce(getNonce());
        builder.setTimestamp(getCreateTime());
        builder.setMerkleRoot(ByteString.copyFrom(getMerkleRoot().unsafeGetArray()));
        builder.setPreviousBlock(ByteString.copyFrom(getPreviousID().unsafeGetArray()));
        if (height >= 0) {
            builder.setHeight(height);
        }
        return builder.build();
    }

    public static APIHeader fromProtobuf(BCSAPIMessage.BLK pb) {

        Builder builder = create();
        builder.version(pb.getVersion());
        builder.difficultyTarget(pb.getDifficulty());
        builder.nonce(pb.getNonce());
        builder.createTime(pb.getTimestamp());
        builder.previousID(BID.createFromSafeArray(pb.getPreviousBlock().toByteArray()));
        builder.merkleRoot(MerkleRoot.createFromSafeArray(pb.getMerkleRoot().toByteArray()));
        if (pb.hasHeight()) {
            builder.height(pb.getHeight());
        }
        return builder.build();
    }

    @Override
    public BID getID() {
        return header.getID();
    }

    @Override
    public int getVersion() {
        return header.getVersion();
    }

    @Override
    public BID getPreviousID() {
        return header.getPreviousID();
    }

    @Override
    public MerkleRoot getMerkleRoot() {
        return header.getMerkleRoot();
    }

    @Deprecated
    @Override
    public int getCreateTime() {
        return header.getCreateTime();
    }

    @Override
    public LocalTime getLocalCreateTime() {
        return header.getLocalCreateTime();
    }

    @Override
    public int getEncodedDifficulty() {
        return header.getEncodedDifficulty();
    }

    @Override
    public int getNonce() {
        return header.getNonce();
    }

    @Override
    public void toWireHeader(WireFormat.Writer writer) throws IOException {
        header.toWireHeader(writer);
    }

    @Override
    public byte[] toWireHeaderBytes() throws IOException {
        return header.toWireHeaderBytes();
    }
}
