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

import org.bouncycastle.util.Arrays;
import org.hyperledger.account.UIAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A public address in the ledger
 */
public class Address implements Serializable {
    /**
     * Supported Address types are
     * <p>
     * <pre>
     * COMMON - digest of a single public key
     * P2SH - digest of a script
     * P2KEY - legacy pay to key - do not use, no advantages just more space
     * LINK - a link to external meta data
     * </pre>
     */
    public enum Type {
        COMMON, P2SH, P2KEY, LINK
    }

    private final Type type;
    private final byte[] bytes;


    /**
     * Create an address
     *
     * @param type    - COMMON or P2SH or P2KEY
     * @param address - SHA256(RIPEM160(key)) digest of public key (COMMON, P2KEY) or spend script (P2SH)
     * @throws HyperLedgerException - thrown if digest length is not 20 bytes
     */
    public Address(Type type, byte[] address) throws HyperLedgerException {
        this.type = type;
        if (address.length != 20) {
            throw new HyperLedgerException("invalid digest length for an address");
        }
        this.bytes = Arrays.clone(address);
    }

    public static Address fromLinkedData(InputStream dataStream) throws HyperLedgerException {
        try {
            MessageDigest digest = MessageDigest.getInstance("RIPEM160");
            byte[] buffer = new byte[20];
            while (dataStream.available() > 0) {
                int read = dataStream.read(buffer, 0, Math.min(20, dataStream.available()));
                digest.update(buffer, 0, read);
            }
            return new Address(Type.LINK, digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * The address type.
     *
     * @return one of COMMON, P2SH, P2KEY, LINK
     */
    public Type getType() {
        return type;
    }


    /**
     * The digest representing the address
     *
     * @return a copy of the digest
     */
    public byte[] toByteArray() {
        return Arrays.clone(bytes);
    }

    /**
     * Get the script suitable for a TransactionOutput script, that it refers to this Address
     * For P2KEY type addresses use the same PublicKey method instead.
     *
     * @return transaction output script
     * @throws HyperLedgerException - if output script is unknown for this address
     * @see org.hyperledger.common.TransactionOutput
     * @see org.hyperledger.common.PublicKey
     */
    public Script getAddressScript() throws HyperLedgerException {
        if (type != Type.LINK) {
            return Script.create().payTo(this).build();
        } else {
            return Script.create().data(bytes).build();
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes) + type.ordinal();
    }

    /**
     * Compare addresses. Note that it is non-standard such that it makes P2Key Address equals LegacyAddress
     *
     * @param obj the other address
     * @return true if equals
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Address)) {
            return false;
        }
        return Arrays.areEqual(bytes, ((Address) obj).bytes) && type == ((Address) obj).type;
    }

    /**
     * Returns human readable representation suitable for debugging.
     * It will use main network encoding for COMMON and P2SH and a hex encoding for P2KEY
     * Use @UIAddress instead if you need to encode to e.g. Testnet3 conventions
     *
     * @return - human readable
     */
    @Override
    public String toString() {
        try {
            return new UIAddress(UIAddress.Network.PRODUCTION, type, bytes).toString();
        } catch (HyperLedgerException e) {
            return type.name() + ":" + ByteUtils.toHex(bytes);
        }
    }
}
