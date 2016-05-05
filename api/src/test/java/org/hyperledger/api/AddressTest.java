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

import org.hyperledger.account.UIAddress;
import org.hyperledger.common.*;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.*;

public class AddressTest {
    @Test
    public void base58Test() throws HyperLedgerException {
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < 10000; ++i) {
            BigInteger n = new BigInteger(160, rnd);
            assertTrue(new BigInteger(ByteUtils.fromBase58(ByteUtils.toBase58(n.toByteArray()))).equals(n));
        }
    }

    @Test
    public void legacyTest() throws HyperLedgerException {
        PublicKey pk = new PublicKey(ByteUtils.fromHex("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"), false);
        Address satoshi = new LegacyAddress(pk);
        Address modern = UIAddress.fromSatoshiStyle("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa").getAddress();
        assertEquals("62e907b15cbf27d5425399ebf6f0fb50ebb88f18", ByteUtils.toHex(modern.toByteArray()));
        assertEquals("62e907b15cbf27d5425399ebf6f0fb50ebb88f18", ByteUtils.toHex(satoshi.toByteArray()));
        assertEquals(satoshi, new Address(Address.Type.P2KEY, satoshi.toByteArray()));
        assertNotEquals(satoshi, modern);
    }

    @Test
    public void satoshiAddressTest() throws HyperLedgerException, UnsupportedEncodingException {
        // some real addresses
        assertTrue(new Address(Address.Type.COMMON, ByteUtils.fromHex("9e969049aefe972e41aaefac385296ce18f30751")).toString().equals(
                "1FTY8etSpSW3xv6s2XRrYE77rrRfza8aJJ"));
        assertTrue(new Address(Address.Type.COMMON, ByteUtils.fromHex("623dbe779a29c6bc2615cd7bf5a35453f495e229")).toString().equals(
                "19xTBrDcnZiJSMuzirE7SfcsjkG1ghp1RL"));

        // some random
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < 10000; ++i) {
            byte[] keyDigest = new byte[20];
            rnd.nextBytes(keyDigest);
            String a = new Address(Address.Type.COMMON, keyDigest).toString();
            byte[] check = UIAddress.fromSatoshiStyle(a).getAddress().toByteArray();
            assertTrue(Arrays.equals(check, keyDigest));
        }
    }
}
