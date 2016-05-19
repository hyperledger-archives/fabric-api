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

import org.hyperledger.common.*;

import java.time.LocalTime;

public class HyperledgerHeader implements Header {
    private final int height;
    private final Header header;

    public HyperledgerHeader(Header header, int height) {
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

        public HyperledgerHeader build() {
            return new HyperledgerHeader(new BitcoinHeader(previousID, merkleRoot, createTime), height);
        }
    }

    public int getHeight() {
        return height;
    }


    @Override
    public BID getID() {
        return header.getID();
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
}
