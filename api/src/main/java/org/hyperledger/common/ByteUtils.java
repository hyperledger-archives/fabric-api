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

import org.bouncycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * generic byte array utilities
 */
public class ByteUtils {
    private static final char[] b58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] r58 = new int[256];

    static {
        for (int i = 0; i < 256; ++i) {
            r58[i] = -1;
        }
        for (int i = 0; i < b58.length; ++i) {
            r58[b58[i]] = i;
        }
    }

    /**
     * convert a byte array to a human readable base58 string. Base58 is a Bitcoin specific encoding similar to widely used base64 but avoids using characters
     * of similar shape, such as 1 and l or O an 0
     *
     * @param b
     * @return
     */
    public static String toBase58(byte[] b) {
        if (b.length == 0) {
            return "";
        }

        int lz = 0;
        while (lz < b.length && b[lz] == 0) {
            ++lz;
        }

        StringBuilder s = new StringBuilder();
        BigInteger n = new BigInteger(1, b);
        while (n.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] r = n.divideAndRemainder(BigInteger.valueOf(58));
            n = r[0];
            char digit = b58[r[1].intValue()];
            s.append(digit);
        }
        while (lz > 0) {
            --lz;
            s.append("1");
        }
        return s.reverse().toString();
    }

    /**
     * Encode in base58 with an added checksum of four bytes.
     *
     * @param b
     * @return
     */
    public static String toBase58WithChecksum(byte[] b) {
        byte[] cs = Hash.hash(b);
        byte[] extended = new byte[b.length + 4];
        System.arraycopy(b, 0, extended, 0, b.length);
        System.arraycopy(cs, 0, extended, b.length, 4);
        return toBase58(extended);
    }

    /**
     * decode from base58 assuming a trailing checksum of four bytes
     *
     * @param s
     * @return
     * @throws HyperLedgerException
     */
    public static byte[] fromBase58WithChecksum(String s) throws HyperLedgerException {
        byte[] b = fromBase58(s);
        if (b.length < 4) {
            throw new HyperLedgerException("Too short for checksum " + s);
        }
        byte[] cs = new byte[4];
        System.arraycopy(b, b.length - 4, cs, 0, 4);
        byte[] data = new byte[b.length - 4];
        System.arraycopy(b, 0, data, 0, b.length - 4);
        byte[] h = new byte[4];
        System.arraycopy(Hash.hash(data), 0, h, 0, 4);
        if (Arrays.equals(cs, h)) {
            return data;
        }
        throw new HyperLedgerException("Checksum mismatch " + s);
    }

    /**
     * decode from base58
     *
     * @param s
     * @return
     * @throws HyperLedgerException
     */
    public static byte[] fromBase58(String s) throws HyperLedgerException {
        try {
            boolean leading = true;
            int lz = 0;
            BigInteger b = BigInteger.ZERO;
            for (char c : s.toCharArray()) {
                if (leading && c == '1') {
                    ++lz;
                } else {
                    leading = false;
                    b = b.multiply(BigInteger.valueOf(58));
                    b = b.add(BigInteger.valueOf(r58[c]));
                }
            }
            byte[] encoded = b.toByteArray();
            if (encoded[0] == 0) {
                if (lz > 0) {
                    --lz;
                } else {
                    byte[] e = new byte[encoded.length - 1];
                    System.arraycopy(encoded, 1, e, 0, e.length);
                    encoded = e;
                }
            }
            byte[] result = new byte[encoded.length + lz];
            System.arraycopy(encoded, 0, result, lz, encoded.length);

            return result;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new HyperLedgerException("Invalid character in address");
        } catch (Exception e) {
            throw new HyperLedgerException(e);
        }
    }

    /**
     * reverse a byte array in place
     * WARNING the parameter array is altered and returned.
     *
     * @param data
     * @return
     */
    public static byte[] reverse(byte[] data) {
        for (int i = 0, j = data.length - 1; i < data.length / 2; i++, j--) {
            data[i] ^= data[j];
            data[j] ^= data[i];
            data[i] ^= data[j];
        }
        return data;
    }

    /**
     * convert a byte array to hexadecimal
     *
     * @param data
     * @return
     */
    public static String toHex(byte[] data) {
        try {
            return new String(Hex.encode(data), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    /**
     * recreate a byte array from hexadecimal
     *
     * @param hex
     * @return
     */
    public static byte[] fromHex(String hex) {
        return Hex.decode(hex);
    }
}
