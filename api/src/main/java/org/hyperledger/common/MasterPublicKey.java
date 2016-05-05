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
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.hyperledger.HyperLedgerSettings;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Public Key Generator following BIP32 @Link https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
 */
public class MasterPublicKey implements MasterKey<PublicKey> {
    private static final X9ECParameters curve = SECNamedCurves.getByName("secp256k1");

    private final PublicKey master;
    private final byte[] chainCode;
    private final int depth;
    private final int parent;
    private final int sequence;

    private static final byte[] BITCOIN_SEED = "Bitcoin seed".getBytes();

    /**
     * Create a PublicKey generator aka. MasterPublicKey from an initial key and added entropy.
     *
     * @param key       initial EC public key
     * @param chainCode additional entropy (32 bytes)
     * @param depth     depth if this is the child of another MasterPublic key
     * @param parent    reference to parent MasterPublicKey with its digest
     * @param sequence  sequence of this MasterPublicKey within its siblings
     */
    public MasterPublicKey(PublicKey key, byte[] chainCode, int depth, int parent, int sequence) {
        this.master = key;
        this.chainCode = chainCode;
        this.parent = parent;
        this.depth = depth;
        this.sequence = sequence;
    }

    /**
     * Encrypt this key with AES/CBC/PKCS5Padding. Useful if you decide to store it.
     *
     * @param passphrase - passphrase
     * @param production - determines the Base58 serialization that will then be encrypted.
     * @return cipher text
     * @throws HyperLedgerException for any error in the used libraries
     */
    public byte[] encrypt(String passphrase, boolean production) throws HyperLedgerException {
        try {
            byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"), BITCOIN_SEED, 16384, 8, 8, 32);
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keyspec);
            byte[] iv = cipher.getIV();
            byte[] c = cipher.doFinal(serialize(production).getBytes());
            byte[] result = new byte[iv.length + c.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(c, 0, result, iv.length, c.length);
            return result;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * Re-create a MasterPublicKey from encrypted serialization.
     *
     * @param passphrase - passphrase
     * @param encrypted  - the cipher text returned by encrypt
     * @return
     * @throws HyperLedgerException error in used libraries or wrong format
     */
    public static MasterPublicKey decrypt(String passphrase, byte[] encrypted) throws HyperLedgerException {
        try {
            byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"), BITCOIN_SEED, 16384, 8, 8, 32);
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            byte[] iv = Arrays.copyOfRange(encrypted, 0, 16);
            byte[] data = Arrays.copyOfRange(encrypted, 16, encrypted.length);
            cipher.init(Cipher.DECRYPT_MODE, keyspec, new IvParameterSpec(iv));
            return MasterPublicKey.parse(new String(cipher.doFinal(data)));
        } catch (UnsupportedEncodingException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new HyperLedgerException(e);
        }
    }


    @Override
    public PublicKey getMaster() {
        return master;
    }

    @Override
    public byte[] getChainCode() {
        return Arrays.clone(chainCode);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public int getParent() {
        return parent;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public int getFingerPrint() {
        int fingerprint = 0;
        byte[] address = master.getAddress().toByteArray();
        for (int i = 0; i < 4; ++i) {
            fingerprint <<= 8;
            fingerprint |= address[i] & 0xff;
        }
        return fingerprint;
    }

    /**
     * Returns an EC public key with the given sequence number. The derivation is deterministic.
     *
     * @param sequence a positive sequence number in [0-2^31)
     * @return a PublicKey
     * @throws HyperLedgerException - there are extremerly seldom (means won't happen in human scale) conditions where
     *                              a key can not be derived with the sequence number. Use an other. And more importantly record the exact condition this
     *                              happened as cryptographers could get rather excited of a concrete demonstration of such condition in a 256 bit key space.
     */
    @Override
    public PublicKey getKey(int sequence) throws HyperLedgerException {
        return generateKey(sequence).getMaster();
    }

    /**
     * Returns child MasterPublicKey key with the given sequence number. The derivation is deterministic.
     *
     * @param sequence a positive sequence number in [0-2^31)
     * @return a MasterPublicKey
     * @throws HyperLedgerException - there are extremerly seldom (means won't happen in human scale) conditions where
     *                              a key can not be derived with the sequence number. Use an other. And more importantly record the exact condition this
     *                              happened as cryptographers could get rather excited of a concrete demonstration of such condition in a 256 bit key space.
     */
    @Override
    public MasterPublicKey getChild(int sequence) throws HyperLedgerException {
        MasterPublicKey sub = generateKey(sequence);
        return new MasterPublicKey(sub.getMaster(), sub.getChainCode(), sub.getDepth() + 1, getFingerPrint(), sequence);
    }

    private MasterPublicKey generateKey(int sequence) throws HyperLedgerException {
        try {
            if ((sequence & 0x80000000) != 0) {
                throw new HyperLedgerException("need private key for hardened generation");
            }
            Mac mac = Mac.getInstance("HmacSHA512", "BC");
            SecretKey key = new SecretKeySpec(chainCode, "HmacSHA512");
            mac.init(key);

            byte[] extended;
            byte[] pub = master.toByteArray();
            extended = new byte[pub.length + 4];
            System.arraycopy(pub, 0, extended, 0, pub.length);
            extended[pub.length] = (byte) ((sequence >>> 24) & 0xff);
            extended[pub.length + 1] = (byte) ((sequence >>> 16) & 0xff);
            extended[pub.length + 2] = (byte) ((sequence >>> 8) & 0xff);
            extended[pub.length + 3] = (byte) (sequence & 0xff);
            byte[] lr = mac.doFinal(extended);
            byte[] l = Arrays.copyOfRange(lr, 0, 32);
            byte[] r = Arrays.copyOfRange(lr, 32, 64);

            BigInteger m = new BigInteger(1, l);
            if (m.compareTo(curve.getN()) >= 0 || m.compareTo(BigInteger.ZERO) == 0) {
                throw new HyperLedgerException("This is rather unlikely, but it did just happen");
            }

            ECPoint p;
            if (HyperLedgerSettings.getInstance().isEnableNativeCryptoMasterPublicKeyGenerateKey()) {
                p = tweakAndDecodePointNativeImpl(l);
            } else {
                p = tweakAndDecodePointBcImpl(pub, m);
            }
            pub = p.getEncoded(true);
            return new MasterPublicKey(new PublicKey(pub, true), r, depth, parent, sequence);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            throw new HyperLedgerException(e);
        }
    }

    private ECPoint tweakAndDecodePointBcImpl(byte[] pub, BigInteger m) throws HyperLedgerException {
        ECPoint p;
        p = curve.getG().multiply(m).add(curve.getCurve().decodePoint(pub));
        if (p.isInfinity()) {
            throw new HyperLedgerException("This is rather unlikely, but it did just happen");
        }
        return p;
    }

    private ECPoint tweakAndDecodePointNativeImpl(byte[] l) {
        ECPoint p;
        byte[] bytes = NativeSecp256k1.pubKeyTweakAdd(master.getUnsafeByteArray(), l);
        p = curve.getCurve().decodePoint(bytes);
        return p;
    }

    private static final byte[] xpub = new byte[]{0x04, (byte) 0x88, (byte) 0xB2, (byte) 0x1E};
    private static final byte[] tpub = new byte[]{0x04, (byte) 0x35, (byte) 0x87, (byte) 0xCF};

    @Override
    public String serialize(boolean production) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {

            if (production) {
                out.write(xpub);
            } else {
                out.write(tpub);
            }

            out.write(depth & 0xff);
            out.write((parent >>> 24) & 0xff);
            out.write((parent >>> 16) & 0xff);
            out.write((parent >>> 8) & 0xff);
            out.write(parent & 0xff);
            out.write((sequence >>> 24) & 0xff);
            out.write((sequence >>> 16) & 0xff);
            out.write((sequence >>> 8) & 0xff);
            out.write(sequence & 0xff);
            out.write(chainCode);
            out.write(master.toByteArray());
        } catch (IOException e) {
        }
        return ByteUtils.toBase58WithChecksum(out.toByteArray());
    }

    /**
     * Parse a MasterPublickey from its BIP32 compliant serialization.
     *
     * @param serialized a Base58 string
     * @return a master key
     * @throws HyperLedgerException for invalid format
     */
    public static MasterPublicKey parse(String serialized) throws HyperLedgerException {
        byte[] data = ByteUtils.fromBase58WithChecksum(serialized);
        if (data.length != 78) {
            throw new HyperLedgerException("invalid extended key");
        }
        byte[] type = Arrays.copyOf(data, 4);
        if (!Arrays.areEqual(type, xpub) && !Arrays.areEqual(type, tpub)) {
            throw new HyperLedgerException("invalid magic number for an master public key");
        }

        int depth = data[4] & 0xff;

        int parent = data[5] & 0xff;
        parent <<= 8;
        parent |= data[6] & 0xff;
        parent <<= 8;
        parent |= data[7] & 0xff;
        parent <<= 8;
        parent |= data[8] & 0xff;

        int sequence = data[9] & 0xff;
        sequence <<= 8;
        sequence |= data[10] & 0xff;
        sequence <<= 8;
        sequence |= data[11] & 0xff;
        sequence <<= 8;
        sequence |= data[12] & 0xff;

        byte[] chainCode = Arrays.copyOfRange(data, 13, 13 + 32);
        byte[] pubOrPriv = Arrays.copyOfRange(data, 13 + 32, data.length);
        return new MasterPublicKey(new PublicKey(pubOrPriv, true), chainCode, depth, parent, sequence);
    }

    @Override
    public String toString() {
        return serialize(true);
    }

    @Override
    public int hashCode() {
        return master.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MasterPublicKey) {
            return master.equals(((MasterPublicKey) obj).master) && Arrays.areEqual(chainCode, ((MasterPublicKey) obj).chainCode)
                    && depth == ((MasterPublicKey) obj).depth &&
                    parent == ((MasterPublicKey) obj).parent && sequence == ((MasterPublicKey) obj).sequence;
        }
        return false;
    }
}
