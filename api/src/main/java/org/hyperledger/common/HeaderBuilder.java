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

public class HeaderBuilder<T extends HeaderBuilder> {
    protected int version;
    protected BID previousID;
    protected MerkleRoot merkleRoot = MerkleRoot.INVALID;
    protected int createTime;
    protected int difficultyTarget;
    protected int nonce;

    protected HeaderBuilder() {
    }

    public T version(int version) {
        this.version = version;
        return (T) this;
    }

    public T previousID(BID previousHash) {
        this.previousID = previousHash;
        return (T) this;
    }

    public T merkleRoot(MerkleRoot merkleRoot) {
        this.merkleRoot = merkleRoot;
        return (T) this;
    }

    public T createTime(int createTime) {
        this.createTime = createTime;
        return (T) this;
    }

    public T difficultyTarget(int difficultyTarget) {
        this.difficultyTarget = difficultyTarget;
        return (T) this;
    }

    public T nonce(int nonce) {
        this.nonce = nonce;
        return (T) this;
    }
}
