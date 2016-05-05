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

import org.hyperledger.common.Address;
import org.hyperledger.common.Transaction;

import java.util.Arrays;

/**
 * Represents a color that is tracked while reallocated with transactions
 */
public class ForeignAsset implements Color {

    private final Address assetAddress;

    private ForeignAsset() {
        assetAddress = null;
    }

    public ForeignAsset(Address assetAddress) {
        this.assetAddress = assetAddress;
    }

    public Address getAssetAddress() {
        return assetAddress;
    }

    @Override
    public byte[] getEncoded() {
        byte[] enc = Arrays.copyOf(assetAddress.toByteArray(), 21);
        enc[20] = (byte) assetAddress.getType().ordinal();
        return enc;
    }

    @Override
    public Color toCanonical(Transaction tx) {
        return this;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForeignAsset that = (ForeignAsset) o;

        return !(assetAddress != null ? !assetAddress.equals(that.assetAddress) : that.assetAddress != null);

    }

    @Override
    public int hashCode() {
        return assetAddress != null ? assetAddress.hashCode() % Integer.MAX_VALUE : Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "OutputColor{" +
                "id=" + assetAddress +
                '}';
    }
}
