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

import org.bitcoin.NativeSecp256k1;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.hyperledger.HyperLedgerSettings;

import java.io.IOException;
import java.math.BigInteger;

/**
 * An EC public Key suitable for verifying a signature created with the corresponding EC PrivateKey
 *
 * @see PrivateKey
 */
public class PublicKey implements Key {
    private final byte[] pub;
    private final boolean compressed;

    public static PublicKey decode(byte[] bytes) {
        if (bytes.length == 0)
            throw new IllegalArgumentException("Invalid public key");

        boolean compressed = isCompressed(bytes);
        return decodeAndCompress(bytes, compressed);
    }

    public static PublicKey decodeAndCompress(byte[] bytes, boolean compress) {
        if (bytes.length == 0)
            throw new IllegalArgumentException("Invalid public key");

            ECPoint p = curve.getCurve().decodePoint(bytes);
        return new PublicKey(p.getEncoded(compress), compress);
    }

    private static boolean isCompressed(byte[] bytes) {
        byte type = bytes[0];
        return type == 0x02 || type == 0x03;
    }

    /**
     * Create a public key from its binary representation and telling if it is compressed
     *
     * @param pub        EC point coordinates encoded in a byte array
     * @param compressed true for modern, false for legacy encoding
     */
    public PublicKey(byte[] pub, boolean compressed) {
        this.pub = pub;
        this.compressed = compressed;
    }

    @Override
    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public Address getAddress() {
        try {
            return new Address(Address.Type.COMMON, Hash.keyHash(pub));
        } catch (HyperLedgerException e) {
            return null;
        }
    }

    /**
     * Get the TransactionOutput script for legacy pay-to-key. Use COMMON or P2SH addresses instead.
     *
     * @return a transaction output script spendable with the private key for this key
     * @see Address
     */
    public Script getP2KeyScript() {
        return Script.create().payToPublicKey(new PublicKey(pub, compressed)).build();
    }

    /**
     * Get the Address using legacy pay-to-key format. Use COMMON or P2SH addresses instead.
     *
     * @return an address spendable with the private key for this key
     */
    public Address getP2KeyAddress() {
        try {
            return new LegacyAddress(this);
        } catch (HyperLedgerException e) {
            return null;
        }
    }

    @Override
    public byte[] toByteArray() {
        return Arrays.clone(pub);
    }

    /**
     * verify a signature created with the private counterpart of this key
     *
     * @param hash      arbitrary data
     * @param signature signature
     * @return true if valid
     */
    public boolean verify(byte[] hash, byte[] signature) {
        return verify(hash, signature, pub);
    }

    /**
     * verify a signature
     *
     * @param hash      arbitrary data
     * @param signature signature
     * @param pub       public key in binary representation
     * @return true if signature is valid for the key and data
     */
    public static boolean verify(byte[] hash, byte[] signature, byte[] pub) {
        ASN1InputStream asn1 = new ASN1InputStream(signature);
        try {
            ECDSASigner signer = new ECDSASigner();
            signer.init(false, new ECPublicKeyParameters(curve.getCurve().decodePoint(pub), domain));

            DLSequence seq = (DLSequence) asn1.readObject();
            BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue();
            BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();
            return signer.verifySignature(hash, r, s);
        } catch (Exception e) {
            // treat format errors as invalid signatures
            return false;
        } finally {
            try {
                asn1.close();
            } catch (IOException e) {
            }
        }
    }

    public PublicKey offsetKey(BigInteger offset) throws HyperLedgerException {
        boolean invert = false;
        if (offset.compareTo(BigInteger.ZERO) < 0) {
            invert = true;
            offset = offset.abs();
        }
        if (HyperLedgerSettings.getInstance().isEnableNativeCryptoPublicKeyOffset()) {
            return getPublicKeyNativeImpl(offset);
        } else {
            return getPublicKeyBcImpl(offset, invert);
        }
    }

    private PublicKey getPublicKeyBcImpl(BigInteger offset, boolean invert) throws HyperLedgerException {
        ECPoint oG = curve.getG().multiply(offset);
        if (invert) {
            oG = oG.negate();
        }
        ECPoint q = oG.add(curve.getCurve().decodePoint(pub));
        if (q.isInfinity()) {
            throw new HyperLedgerException("This is rather unlikely, but it did just happen");
        }
        return new PublicKey(q.getEncoded(compressed), compressed);
    }

    private PublicKey getPublicKeyNativeImpl(BigInteger offset) {
        byte[] offsetb = offset.toByteArray();
        byte[] pkBytes = NativeSecp256k1.pubKeyTweakAdd(getUnsafeByteArray(), offsetb);
        return decodeAndCompress(pkBytes, true);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pub);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PublicKey) {
            return Arrays.areEqual(pub, ((PublicKey) obj).pub) && compressed == ((PublicKey) obj).compressed;
        }
        return false;
    }

    @Override
    public String toString() {
        return "public key of " + getAddress();
    }

    public byte[] getUnsafeByteArray() {
        return pub;
    }
}
