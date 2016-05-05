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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.hyperledger.HyperLedgerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * An EC Private Key, technically a big positive integer capable of signing such that the signature can be verified
 * with the corresponding EC Public Key
 *
 * @see PublicKey
 */
public class PrivateKey implements Key {
    private static final Logger log = LoggerFactory.getLogger(PrivateKey.class);

    static final SecureRandom secureRandom = new SecureRandom();
    static final BigInteger HALF_CURVE_ORDER = curve.getN().shiftRight(1);

    private final BigInteger priv;
    private byte[] pub;
    private final boolean compressed;

    /**
     * Older Bitcoin transactions used uncompressed Public keys. Forget them.
     *
     * @return true for compressed
     */
    @Override
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * Create a new PrivateKey using the platform provided secure random source.
     *
     * @return new PrivateKey
     */
    public static PrivateKey createNew() {
        return createNew(true);
    }


    /**
     * Create a new PrivateKey using the platform provided secure random source.
     *
     * @param compressed set to false if you relly want legacy format
     * @return new PrivateKey
     */
    public static PrivateKey createNew(boolean compressed) {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(domain, secureRandom);
        generator.init(keygenParams);
        AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
        ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
        return new PrivateKey(privParams.getD(), compressed, pubParams.getQ().getEncoded(compressed));
    }

    /**
     * Return whether the private key is valid or not.
     *
     * @return a boolean
     */
    public boolean isValid() {
        return ! priv.equals(BigInteger.ZERO);
    }

    /**
     * Return the private key as big positive integer.
     *
     * @return an integer
     */
    public BigInteger asBigInteger() {
        return priv;
    }

    /**
     * Return the private key as a 32byte array aligned to the right.
     *
     * @return private key as byte array
     */
    @Override
    public byte[] toByteArray() {
        if (!isValid()) {
            return new byte[0];
        }
        byte[] p = priv.toByteArray();

        if (p.length != 32) {
            byte[] tmp = new byte[32];
            System.arraycopy(p, Math.max(0, p.length - 32), tmp, Math.max(0, 32 - p.length), Math.min(32, p.length));
            p = tmp;
        }

        return p;
    }

    /**
     * Return the corresponding public key. Computing Public from Private is easy, the other direction is supposedly
     * practically impossible. Bitcoin was dead if it was not.
     *
     * @return
     */
    public PublicKey getPublic() {
        return new PublicKey(getPub(), compressed);
    }

    private byte[] getPublicKeyBytesBcImpl() {
        return curve.getG().multiply(priv).getEncoded(compressed);
    }

    private byte[] getPublicKeyBytesNativeImpl() {
        byte[] bytes = NativeSecp256k1.computePubkey(toByteArray());
        if (bytes.length == 0)
            throw new IllegalArgumentException("Invalid public key");

        ECPoint p = curve.getCurve().decodePoint(bytes);
        return p.getEncoded(compressed);
    }

    @Override
    public Address getAddress() {
        if (!isValid()) {
            return null;
        }
        try {
            return new Address(Address.Type.COMMON, Hash.keyHash(getPub()));
        } catch (HyperLedgerException e) {
            return null;
        }
    }

    private byte[] getPub() {
        if (pub == null) {
            if (HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeyGetPublic()) {
                pub = getPublicKeyBytesNativeImpl();
            } else {
                pub = getPublicKeyBytesBcImpl();
            }
        }
        return pub;
    }

    /**
     * (Re-)create a private key from a byte array and telling if its public counterpart should be compressed.
     *
     * @param p          32 bytes of data
     * @param compressed true for modern, false for legacy keys
     * @throws HyperLedgerException
     */
    public PrivateKey(byte[] p, boolean compressed) throws HyperLedgerException {
        if (p.length != 32) {
            throw new HyperLedgerException("Invalid private key");
        }
        this.priv = new BigInteger(1, p).mod(curve.getN());
        this.compressed = compressed;
    }

    /**
     * (Re-)create a private key from a big positive integer and telling if its public counterpart should be compressed.
     *
     * @param priv       key as number
     * @param compressed true for modern, false for legacy keys
     * @throws HyperLedgerException if priv is not positive or greater than curve modulo
     */
    public PrivateKey(BigInteger priv, boolean compressed) throws HyperLedgerException {
        if (priv.compareTo(BigInteger.ZERO) <= 0 || priv.compareTo(curve.getN()) >= 0) {
            throw new HyperLedgerException("invalid key");
        }
        this.priv = priv;
        this.compressed = compressed;
    }

    private PrivateKey(BigInteger priv, boolean compressed, byte[] pub) {
        this.priv = priv;
        this.compressed = compressed;
        this.pub = pub;
    }

    /**
     * Sign a digest with this key.
     *
     * @param hash arbitrary data
     * @return signature
     */
    public byte[] sign(byte[] hash) {
        if (!isValid()) {
            return null;
        }
        // NativeSecp256k1.sign() supports signing only 32 byte size data
        if (HyperLedgerSettings.getInstance().isEnableNativeCryptoPrivateKeySign() && isHashNativelySupported(hash)) {
            return signNativeImpl(hash);
        } else {
            try {
                return signBcImpl(hash);
            } catch (IOException e) {
                log.warn("Unexpected exception happened: {}", e);
            }
        }
        return null;
    }

    private byte[] signBcImpl(byte[] hash) throws IOException {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(priv, domain));
        BigInteger[] signature = signer.generateSignature(hash);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DERSequenceGenerator seq = new DERSequenceGenerator(baos);
        seq.addObject(new ASN1Integer(signature[0]));
        seq.addObject(new ASN1Integer(toCanonicalS(signature[1])));
        seq.close();
        return baos.toByteArray();
    }

    private byte[] signNativeImpl(byte[] hash) {
        byte[] privateKeyBytes = toByteArray();
        return NativeSecp256k1.sign(hash, privateKeyBytes);
    }

    private boolean isHashNativelySupported(byte[] hash) {
        return hash.length == 32;
    }

    private BigInteger toCanonicalS(BigInteger s) {
        if (s.compareTo(HALF_CURVE_ORDER) <= 0) {
            return s;
        } else {
            return curve.getN().subtract(s);
        }
    }

    @Override
    public String toString() {
        return "private key of " + getAddress();
    }

    /**
     * Serialize a private key into Base58 used by Bitcoin
     *
     * @param key private key
     * @return Base58 string
     */
    public static String serializeWIF(PrivateKey key) {
        return ByteUtils.toBase58(bytesWIF(key));
    }


    private static byte[] bytesWIF(PrivateKey key) {
        byte[] k = key.toByteArray();
        if (key.isCompressed()) {
            byte[] encoded = new byte[k.length + 6];
            byte[] ek = new byte[k.length + 2];
            ek[0] = (byte) 0x80;
            System.arraycopy(k, 0, ek, 1, k.length);
            ek[k.length + 1] = 0x01;
            byte[] hash = Hash.hash(ek);
            System.arraycopy(ek, 0, encoded, 0, ek.length);
            System.arraycopy(hash, 0, encoded, ek.length, 4);
            return encoded;
        } else {
            byte[] encoded = new byte[k.length + 5];
            byte[] ek = new byte[k.length + 1];
            ek[0] = (byte) 0x80;
            System.arraycopy(k, 0, ek, 1, k.length);
            byte[] hash = Hash.hash(ek);
            System.arraycopy(ek, 0, encoded, 0, ek.length);
            System.arraycopy(hash, 0, encoded, ek.length, 4);
            return encoded;
        }
    }

    /**
     * Recreate a private key form its Base58 serialization
     *
     * @param serialized private key in text
     * @return Base58 string
     */
    public static PrivateKey parseWIF(String serialized) throws HyperLedgerException {
        byte[] store = ByteUtils.fromBase58(serialized);
        return parseBytesWIF(store);
    }

    /**
     * Recreate a private key form its binary serialization
     *
     * @param store private key in text
     * @return Base58 string
     */
    public static PrivateKey parseBytesWIF(byte[] store) throws HyperLedgerException {
        if (store.length == 37) {
            checkChecksum(store);
            byte[] key = new byte[store.length - 5];
            System.arraycopy(store, 1, key, 0, store.length - 5);
            return new PrivateKey(key, false);
        } else if (store.length == 38) {
            checkChecksum(store);
            byte[] key = new byte[store.length - 6];
            System.arraycopy(store, 1, key, 0, store.length - 6);
            return new PrivateKey(key, true);
        }
        throw new HyperLedgerException("Invalid key length");
    }

    private static void checkChecksum(byte[] store) throws HyperLedgerException {
        byte[] checksum = new byte[4];
        System.arraycopy(store, store.length - 4, checksum, 0, 4);
        byte[] ekey = new byte[store.length - 4];
        System.arraycopy(store, 0, ekey, 0, store.length - 4);
        byte[] hash = Hash.hash(ekey);
        for (int i = 0; i < 4; ++i) {
            if (hash[i] != checksum[i]) {
                throw new HyperLedgerException("checksum mismatch");
            }
        }
    }

    public PrivateKey offsetKey(BigInteger offset) throws HyperLedgerException {
        if (! isValid()) {
            throw new HyperLedgerException("Illegal operation for an invalid private key");
        }

        if (offset.compareTo(curve.getN()) >= 0) {
            throw new HyperLedgerException("This is rather unlikely, but it did just happen");
        }
        BigInteger k = offset.add(priv).mod(curve.getN());
        if (k.compareTo(BigInteger.ZERO) == 0) {
            throw new HyperLedgerException("This is rather unlikely, but it did just happen");
        }
        return new PrivateKey(k, compressed);
    }

    @Override
    public int hashCode() {
        return priv.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PrivateKey) {
            return priv.compareTo(((PrivateKey) obj).priv) == 0 && compressed == ((PrivateKey) obj).compressed;
        }
        return false;
    }
}
