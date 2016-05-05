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
import org.hyperledger.common.HyperLedgerException;
import org.hyperledger.common.TID;
import org.hyperledger.common.Transaction;

import java.io.IOException;
import java.util.Arrays;

/**
 * Interface implemented by both colored coins and native assets.
 */
public interface Color {
    NativeAsset BITCOIN = new NativeAsset(TID.BITCOIN_NATIVE, 0) {
        @Override
        public Color toCanonical(Transaction tx) {
            return this;
        }

        @Override
        public boolean isNative() {
            return true;
        }

        /** This is only true for the BITCOIN singleton */
        @Override
        public boolean isToken() {
            return true;
        }
    };

    static Color fromEncoded(byte[] encoded) {
        try {
            if (encoded.length == 21) {
                return new ForeignAsset(
                        new Address(Address.Type.values()[encoded[20]], Arrays.copyOfRange(encoded, 0, 20)));
            } else {
                try {
                    return NativeAsset.fromEncoded(encoded);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (HyperLedgerException e) {
            return null;
        }
    }

    byte[] getEncoded();

    /**
     * Make the color canonical - see {@link NativeAsset#toCanonical}
     */
    Color toCanonical(Transaction tx);

    /**
     * Whether this is a native asset - see {@link NativeAsset}
     */
    boolean isNative();

    /**
     * Whether this is the underlying token - only true for the {@link Color#BITCOIN} singleton
     */
    boolean isToken();
}
