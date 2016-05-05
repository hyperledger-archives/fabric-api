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

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;

/**
 * Master Private Key Generator following BIP32 @Link https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
 */
public class MasterPrivateKey implements MasterKey<PrivateKey> {
    private static final SecureRandom rnd = new SecureRandom();
    private static final X9ECParameters curve = SECNamedCurves.getByName("secp256k1");

    private final PrivateKey master;
    private final byte[] chainCode;
    private final int depth;
    private final int parent;
    private final int sequence;

    private static final byte[] BITCOIN_SEED = "Bitcoin seed".getBytes();


    /**
     * Create a new MasterPrivateKey using the platform provided SecureRandom generator
     *
     * @return new random MasterPrivateKey
     */
    public static MasterPrivateKey createNew() {
        PrivateKey key = PrivateKey.createNew(true);
        byte[] chainCode = new byte[32];
        rnd.nextBytes(chainCode);
        return new MasterPrivateKey(key, chainCode, 0, 0, 0);
    }

    /**
     * Create a new MasterPrivateKey from a PrivateKey and added entropy
     *
     * @param key       the starting or master PrivateKey
     * @param chainCode additional entropy (32 bytes)
     * @param depth     depth of generation. One more than the depth of the parent that generated this key.
     * @param parent    short digest of the parent key
     * @param sequence  sequence number between siblings of this MasterPrivateKey
     */
    public MasterPrivateKey(PrivateKey key, byte[] chainCode, int depth, int parent, int sequence) {
        this.master = key;
        this.chainCode = chainCode;
        this.parent = parent;
        this.depth = depth;
        this.sequence = sequence;
    }

    /**
     * Create a MasterPrivateKey from a seed, that is assumed to be encrypted. In practice often simply random.
     *
     * @param passphrase    - passphrase for decryption
     * @param encryptedSeed the seed
     * @return (re-)created MasterPrivateKey
     * @throws HyperLedgerException for any error in used libraries
     */
    public static MasterPrivateKey createFromEncryptedSeed(String passphrase, byte[] encryptedSeed) throws HyperLedgerException {
        try {
            byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"), BITCOIN_SEED, 16384, 8, 8, 32);
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            if (encryptedSeed.length != 32) {
                throw new HyperLedgerException("Incorrect encrypted seed length");
            }
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, keyspec);
            return create(cipher.doFinal(encryptedSeed));
        } catch (UnsupportedEncodingException | NoSuchPaddingException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new HyperLedgerException(e);
        }
    }


    /**
     * Create a MasterPrivateKey from a plain text seed. The seed is stretched/resized to 64 bytes with HmacSHA512
     *
     * @param seed arbitrary data
     * @return (re-)created MasterPrivateKey
     * @throws HyperLedgerException for any error in called crypto libraries
     */
    public static MasterPrivateKey create(byte[] seed) throws HyperLedgerException {
        try {
            Mac mac = Mac.getInstance("HmacSHA512", "BC");
            SecretKey seedkey = new SecretKeySpec(BITCOIN_SEED, "HmacSHA512");
            mac.init(seedkey);
            byte[] lr = mac.doFinal(seed);
            byte[] l = Arrays.copyOfRange(lr, 0, 32);
            byte[] r = Arrays.copyOfRange(lr, 32, 64);
            BigInteger m = new BigInteger(1, l);
            if (m.compareTo(curve.getN()) >= 0 || m.compareTo(BigInteger.ZERO) == 0) {
                throw new HyperLedgerException("This is rather unlikely, but it did just happen");
            }
            PrivateKey keyPair = new PrivateKey(m, true);
            return new MasterPrivateKey(keyPair, r, 0, 0, 0);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * Re-create a MasterPrivateKey from encrypted serialization
     *
     * @param passphrase passphrase
     * @param encrypted  cipher text from encrypt
     * @return
     * @throws HyperLedgerException error in used libraries or wrong format
     */
    public static MasterPrivateKey decrypt(String passphrase, byte[] encrypted) throws HyperLedgerException {
        try {
            byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"), BITCOIN_SEED, 16384, 8, 8, 32);
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            byte[] iv = Arrays.copyOfRange(encrypted, 0, 16);
            byte[] data = Arrays.copyOfRange(encrypted, 16, encrypted.length);
            cipher.init(Cipher.DECRYPT_MODE, keyspec, new IvParameterSpec(iv));
            return MasterPrivateKey.parse(new String(cipher.doFinal(data)));
        } catch (UnsupportedEncodingException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * Encrypt this key with AES/CBC/PKCS5Padding. Useful if you decide to store it.
     *
     * @param passphrase - passphrase
     * @param production - determines the Base58 serialization that will then be encrypted.
     * @return ciphertext
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

    @Override
    public PrivateKey getMaster() {
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
     * Get the PrivateKey with the sequence number
     *
     * @param sequence sequence number in [0-2^31)
     * @return EC PrivateKey
     * @throws HyperLedgerException if sequence number is negative
     */
    @Override
    public PrivateKey getKey(int sequence) throws HyperLedgerException {
        if ((sequence & 0x80000000) != 0)
            throw new HyperLedgerException("Use getHardenedKey instead");
        return generateKey(sequence).getMaster();
    }

    /**
     * Get the child MasterPrivate key with the sequence number
     *
     * @param sequence sequence number in [0-2^31)
     * @return generated child MasterPrivateKey
     * @throws HyperLedgerException if sequence number is negative
     */
    @Override
    public MasterPrivateKey getChild(int sequence) throws HyperLedgerException {
        if ((sequence & 0x80000000) != 0)
            throw new HyperLedgerException("Use getHardenedChild instead");
        MasterPrivateKey sub = generateKey(sequence);
        return new MasterPrivateKey(sub.getMaster(), sub.getChainCode(), sub.getDepth() + 1, getFingerPrint(), sequence);
    }

    /**
     * Get a "hardened" MasterPrivateKey with the sequence number. Public counterparts of "hardened" keys are not attainable
     * from corresponding MasterPublicKey
     *
     * @param sequence sequence number in [0-2^31)
     * @return generated hardened child MasterPrivateKey
     * @throws HyperLedgerException
     */
    public MasterPrivateKey getHardenedChild(int sequence) throws HyperLedgerException {
        sequence |= 0x80000000;
        MasterPrivateKey sub = generateKey(sequence);
        return new MasterPrivateKey(sub.getMaster(), sub.getChainCode(), sub.getDepth() + 1, getFingerPrint(), sequence);
    }

    /**
     * Get a "hardened" PrivateKey with the sequence number. Public counterparts of "hardened" keys are not attainable
     * from corresponding MasterPublicKey
     *
     * @param sequence sequence number in [0-2^31)
     * @return generated hardened Key
     * @throws HyperLedgerException
     */
    public PrivateKey getHardenedKey(int sequence) throws HyperLedgerException {
        sequence |= 0x80000000;
        return generateKey(sequence).getMaster();
    }

    /**
     * Get the corresponding MasterPublicKey that is able to generate public keys and public master children for non-hardened
     * sequences. The MasterPublicKey does not contain the private Key but still has to be guarded closer than plain public keys
     * as the extra entropy in them plus a single derived private key allows for computing all other private keys and descendant
     * master private keys.
     *
     * @return
     */
    public MasterPublicKey getMasterPublic() {
        return new MasterPublicKey(new PublicKey(master.getPublic().toByteArray(), true), chainCode, depth, parent, sequence);
    }

    private MasterPrivateKey generateKey(int sequence) throws HyperLedgerException {
        try {
            Mac mac = Mac.getInstance("HmacSHA512", "BC");
            SecretKey key = new SecretKeySpec(chainCode, "HmacSHA512");
            mac.init(key);

            byte[] extended;
            byte[] pub = master.getPublic().toByteArray();
            if ((sequence & 0x80000000) == 0) {
                extended = new byte[pub.length + 4];
                System.arraycopy(pub, 0, extended, 0, pub.length);
                extended[pub.length] = (byte) ((sequence >>> 24) & 0xff);
                extended[pub.length + 1] = (byte) ((sequence >>> 16) & 0xff);
                extended[pub.length + 2] = (byte) ((sequence >>> 8) & 0xff);
                extended[pub.length + 3] = (byte) (sequence & 0xff);
            } else {
                byte[] priv = master.toByteArray();
                extended = new byte[priv.length + 5];
                System.arraycopy(priv, 0, extended, 1, priv.length);
                extended[priv.length + 1] = (byte) ((sequence >>> 24) & 0xff);
                extended[priv.length + 2] = (byte) ((sequence >>> 16) & 0xff);
                extended[priv.length + 3] = (byte) ((sequence >>> 8) & 0xff);
                extended[priv.length + 4] = (byte) (sequence & 0xff);
            }
            byte[] lr = mac.doFinal(extended);
            byte[] l = Arrays.copyOfRange(lr, 0, 32);
            byte[] r = Arrays.copyOfRange(lr, 32, 64);

            BigInteger m = new BigInteger(1, l);
            if (m.compareTo(curve.getN()) >= 0 || m.compareTo(BigInteger.ZERO) == 0) {
                throw new HyperLedgerException("This is rather unlikely, but it did just happen");
            }
            BigInteger k = m.add(new BigInteger(1, master.toByteArray())).mod(curve.getN());
            if (k.compareTo(BigInteger.ZERO) == 0) {
                throw new HyperLedgerException("This is rather unlikely, but it did just happen");
            }
            return new MasterPrivateKey(new PrivateKey(k, true), r, depth, parent, sequence);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            throw new HyperLedgerException(e);
        }
    }

    private static final byte[] xprv = new byte[]{0x04, (byte) 0x88, (byte) 0xAD, (byte) 0xE4};
    private static final byte[] tprv = new byte[]{0x04, (byte) 0x35, (byte) 0x83, (byte) 0x94};

    /**
     * Serialize into a Base58 string in BIP32 convention
     *
     * @param production - serialization will start with x if true otherwise with t
     * @return
     */
    @Override
    public String serialize(boolean production) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (production) {
                out.write(xprv);
            } else {
                out.write(tprv);
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
            out.write(0x00);
            out.write(master.toByteArray());
        } catch (IOException e) {
        }
        return ByteUtils.toBase58WithChecksum(out.toByteArray());
    }

    /**
     * Recreate a key from BIP32 serialization
     *
     * @param serialized
     * @return MasterPrivateKey
     * @throws HyperLedgerException
     */
    public static MasterPrivateKey parse(String serialized) throws HyperLedgerException {
        byte[] data = ByteUtils.fromBase58WithChecksum(serialized);
        if (data.length != 78) {
            throw new HyperLedgerException("invalid master key");
        }
        byte[] type = Arrays.copyOf(data, 4);
        if (!Arrays.areEqual(type, xprv) && !Arrays.areEqual(type, tprv)) {
            throw new HyperLedgerException("invalid magic number for a master private key");
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
        return new MasterPrivateKey(new PrivateKey(new BigInteger(1, pubOrPriv), true), chainCode, depth, parent, sequence);
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
        if (obj instanceof MasterPrivateKey) {
            return master.equals(((MasterPrivateKey) obj).master) && Arrays.areEqual(chainCode, ((MasterPrivateKey) obj).chainCode)
                    && depth == ((MasterPrivateKey) obj).depth &&
                    parent == ((MasterPrivateKey) obj).parent && sequence == ((MasterPrivateKey) obj).sequence;
        }
        return false;
    }
}
